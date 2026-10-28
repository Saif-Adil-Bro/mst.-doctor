package com.health.knowledge.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.knowledge.data.repository.ArticleRepository
import com.health.knowledge.domain.model.Article
import com.health.knowledge.domain.model.toDomain
import com.health.knowledge.sync.DailyTipNotificationWorker
import com.health.knowledge.util.DatabaseSeeder
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HerbalIngredientStat(
    val name: String,
    val count: Int
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val prefsManager: UserPreferencesManager,
    private val databaseSeeder: DatabaseSeeder
) : ViewModel() {

    // First launch database seeding state
    val isSeeding: StateFlow<Boolean> = databaseSeeder.isSeeding

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected category filter
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Active tab: "home" | "search" | "bookmarks"
    private val _activeTab = MutableStateFlow("home")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Saved sub-tab inside "bookmarks": "bookmarks" | "history" | "notes"
    private val _savedSubTab = MutableStateFlow("bookmarks")
    val savedSubTab: StateFlow<String> = _savedSubTab.asStateFlow()

    // Herbal Index & Advanced Search states
    private val _herbSearchQuery = MutableStateFlow("")
    val herbSearchQuery: StateFlow<String> = _herbSearchQuery.asStateFlow()

    private val _selectedHerb = MutableStateFlow<String?>(null)
    val selectedHerb: StateFlow<String?> = _selectedHerb.asStateFlow()

    private val _selectedMultiCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedMultiCategories: StateFlow<Set<String>> = _selectedMultiCategories.asStateFlow()

    // Pull-to-refresh state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Preferences states
    val sortOrder: StateFlow<String> = prefsManager.sortOrder
    val themeMode: StateFlow<String> = prefsManager.themeMode
    val dailyReminderEnabled: StateFlow<Boolean> = prefsManager.dailyReminderEnabled
    val dailyReminderHour: StateFlow<Int> = prefsManager.dailyReminderHour
    val dailyReminderMinute: StateFlow<Int> = prefsManager.dailyReminderMinute
    val isAdminMode: StateFlow<Boolean> = prefsManager.isAdminMode

    // Saved scroll position across navigation and config changes
    var savedScrollIndex: Int = 0
        private set
    var savedScrollOffset: Int = 0
        private set

    var savedBookmarkScrollIndex: Int = 0
        private set
    var savedBookmarkScrollOffset: Int = 0
        private set

    fun saveHomeScrollPosition(index: Int, offset: Int) {
        savedScrollIndex = index
        savedScrollOffset = offset
    }

    fun saveBookmarkScrollPosition(index: Int, offset: Int) {
        savedBookmarkScrollIndex = index
        savedBookmarkScrollOffset = offset
    }

    // All active articles domain flow
    private val allActiveDomainArticles: StateFlow<List<Article>> = repository.getAllArticles()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Keep articles warm with SharingStarted.Lazily + apply Sort Order (Popular / Newest / Oldest)
    val articles: StateFlow<List<Article>> = combine(
        _searchQuery.debounce(200),
        _selectedCategory,
        sortOrder
    ) { query, category, sort ->
        Triple(query, category, sort)
    }.flatMapLatest { (query, category, sort) ->
        val baseFlow = when {
            category != null -> repository.getByCategory(category)
            query.isNotBlank() -> repository.searchArticles(query)
            else -> repository.getAllArticles()
        }
        baseFlow.map { entities ->
            val domainList = entities.map { it.toDomain() }
            sortArticles(domainList, sort)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Window limit for instantaneous progressive loading without lag
    private val _visibleItemCount = MutableStateFlow(25)
    val visibleItemCount: StateFlow<Int> = _visibleItemCount.asStateFlow()

    // Smooth chunked articles for LazyColumn to eliminate UI jank & load instantly on scroll
    val displayedArticles: StateFlow<List<Article>> = combine(
        articles,
        _visibleItemCount
    ) { all, limit ->
        all.take(limit)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val hasMoreArticles: StateFlow<Boolean> = combine(
        articles,
        _visibleItemCount
    ) { all, limit ->
        all.size > limit
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    // True while initial bundled JSON is seeding or initial database load is in progress
    val isInitialLoading: StateFlow<Boolean> = combine(
        allActiveDomainArticles,
        databaseSeeder.isSeeding
    ) { list, seeding ->
        seeding || list.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun loadMoreArticles() {
        val total = articles.value.size
        val current = _visibleItemCount.value
        if (current < total) {
            _visibleItemCount.value = (current + 25).coerceAtMost(total)
        }
    }

    // Daily Health Tip of the Day (deterministic per day + rotatable with nextDailyTip)
    val dailyTipArticle: StateFlow<Article?> = combine(
        allActiveDomainArticles,
        prefsManager.dailyTipOffset
    ) { list, offset ->
        if (list.isEmpty()) null
        else {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val index = (dayOfYear + offset).mod(list.size)
            list[index]
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Herbal / Ingredient Index aggregated across all active articles
    val herbalIndex: StateFlow<List<HerbalIngredientStat>> = combine(
        allActiveDomainArticles,
        _herbSearchQuery
    ) { list, herbQuery ->
        val counts = mutableMapOf<String, Int>()
        for (art in list) {
            for (med in art.medicines) {
                val clean = med.trim()
                if (clean.length >= 2) {
                    counts[clean] = (counts[clean] ?: 0) + 1
                }
            }
        }
        counts.entries
            .map { HerbalIngredientStat(it.key, it.value) }
            .filter {
                herbQuery.isBlank() || it.name.contains(herbQuery.trim(), ignoreCase = true)
            }
            .sortedWith(compareByDescending<HerbalIngredientStat> { it.count }.thenBy { it.name })
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Advanced Search & Herbal Filtered Articles for the "search" tab
    val advancedFilteredArticles: StateFlow<List<Article>> = combine(
        allActiveDomainArticles,
        _herbSearchQuery,
        _selectedHerb,
        _selectedMultiCategories,
        sortOrder
    ) { list, query, herb, multiCats, sort ->
        val filtered = list.filter { art ->
            val matchesHerb = herb == null || art.medicines.any { it.equals(herb, ignoreCase = true) }
            val matchesCats = multiCats.isEmpty() || multiCats.any { selCat ->
                art.categories.any { it.contains(selCat, ignoreCase = true) }
            }
            val matchesQuery = query.isBlank() ||
                art.title.contains(query.trim(), ignoreCase = true) ||
                art.content.contains(query.trim(), ignoreCase = true) ||
                art.medicines.any { it.contains(query.trim(), ignoreCase = true) }
            matchesHerb && matchesCats && matchesQuery
        }
        sortArticles(filtered, sort)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val bookmarkedArticles: StateFlow<List<Article>> = repository.getBookmarked()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val readingHistoryArticles: StateFlow<List<Article>> = repository.getReadingHistory()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val notedArticles: StateFlow<List<Article>> = repository.getArticlesWithNotes()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── My Posts & Analytics (created from this device) ──────────────────────
    val myArticles: StateFlow<List<Article>> = repository.getMyArticles()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val myPendingCount: StateFlow<Int> = myArticles
        .map { list -> list.count { !it.isReviewed && !it.isDraft } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val myApprovedCount: StateFlow<Int> = myArticles
        .map { list -> list.count { it.isReviewed && !it.isDraft } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val myTotalReactions: StateFlow<Int> = myArticles
        .map { list -> list.sumOf { it.reactions } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val totalCount: StateFlow<Int> = repository.observeActiveCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private fun sortArticles(list: List<Article>, sort: String): List<Article> {
        return when (sort) {
            UserPreferencesManager.SORT_NEWEST -> list.sortedWith(
                compareByDescending<Article> { it.isPinned }
                    .thenByDescending { it.createdAt }
                    .thenByDescending { it.id }
            )
            UserPreferencesManager.SORT_OLDEST -> list.sortedWith(
                compareByDescending<Article> { it.isPinned }
                    .thenBy { it.createdAt }
                    .thenBy { it.id }
            )
            else -> list.sortedWith(
                compareByDescending<Article> { it.isPinned }
                    .thenByDescending { it.reactions }
                    .thenByDescending { it.createdAt }
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _visibleItemCount.value = 25
        if (query.isNotBlank()) {
            _selectedCategory.value = null
            savedScrollIndex = 0
            savedScrollOffset = 0
        }
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
        _searchQuery.value = ""
        _visibleItemCount.value = 25
        savedScrollIndex = 0
        savedScrollOffset = 0
    }

    fun onSortOrderChange(order: String) {
        prefsManager.setSortOrder(order)
        _visibleItemCount.value = 25
        savedScrollIndex = 0
        savedScrollOffset = 0
    }

    fun submitUserPost(
        title: String,
        content: String,
        medicines: String,
        categories: List<String>,
        photoUrl: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (title.isBlank() || content.isBlank()) {
            onComplete(false, "শিরোনাম এবং বিস্তারিত বিবরণ পূরণ করা আবশ্যক")
            return
        }

        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                val cleanPhoto = photoUrl.trim()
                val medsCsv = medicines.trim()
                val catsCsv = categories.joinToString(",")

                val res = repository.submitUserArticle(
                    title = title.trim(),
                    content = content.trim(),
                    medicines = medsCsv,
                    categories = catsCsv,
                    photoUrl = cleanPhoto,
                    date = today
                )

                if (res.isSuccess) {
                    onComplete(true, "🎉 আপনার পোস্টটি পর্যালোচনার জন্য জমা হয়েছে! অ্যাডমিন অনুমোদন করার পর এটি সবার কাছে দৃশ্যমান হবে।")
                } else {
                    onComplete(false, "পোস্ট জমা দিতে সমস্যা হয়েছে। পরে আবার চেষ্টা করুন।")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "অপ্রত্যাশিত সমস্যা দেখা দিয়েছে")
            }
        }
    }

    fun onTabSelected(tab: String) {
        _activeTab.value = tab
    }

    fun onSavedSubTabChange(subTab: String) {
        _savedSubTab.value = subTab
    }

    // Herbal Index & Advanced Filter controls
    fun onHerbSearchQueryChange(query: String) {
        _herbSearchQuery.value = query
    }

    fun onSelectHerb(herb: String?) {
        _selectedHerb.value = if (_selectedHerb.value == herb) null else herb
    }

    fun onToggleMultiCategory(category: String) {
        val current = _selectedMultiCategories.value
        _selectedMultiCategories.value = if (current.contains(category)) {
            current - category
        } else {
            current + category
        }
    }

    fun clearAdvancedFilters() {
        _herbSearchQuery.value = ""
        _selectedHerb.value = null
        _selectedMultiCategories.value = emptySet()
    }

    // Theme & Daily Tip controls
    fun setThemeMode(mode: String) {
        prefsManager.setThemeMode(mode)
    }

    fun cycleThemeMode(): String {
        return prefsManager.cycleThemeMode()
    }

    fun nextDailyTip() {
        prefsManager.nextDailyTip()
    }

    fun getFormattedReminderTime(): String {
        return prefsManager.getFormattedReminderTime()
    }

    fun setDailyReminderEnabled(context: Context, enabled: Boolean) {
        prefsManager.setDailyReminderEnabled(enabled)
        DailyTipNotificationWorker.scheduleOrCancel(
            context = context,
            enabled = enabled,
            targetHour = prefsManager.dailyReminderHour.value,
            targetMinute = prefsManager.dailyReminderMinute.value
        )
    }

    fun setDailyReminderTime(context: Context, hour: Int, minute: Int) {
        prefsManager.setDailyReminderTime(hour, minute)
        if (prefsManager.dailyReminderEnabled.value) {
            DailyTipNotificationWorker.scheduleOrCancel(
                context = context,
                enabled = true,
                targetHour = hour,
                targetMinute = minute
            )
        }
    }

    fun sendTestDailyTipNotification(context: Context) {
        dailyTipArticle.value?.let { tip ->
            DailyTipNotificationWorker.showTipNotification(
                context = context,
                articleId = tip.id,
                title = tip.title
            )
        }
    }

    // Pull-to-refresh cloud sync
    fun refreshFromCloud(onDone: (Int) -> Unit = {}) {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            val count = repository.syncFromCloudNow()
            _isRefreshing.value = false
            onDone(count)
        }
    }

    fun toggleBookmark(articleId: Int, currentState: Boolean) {
        viewModelScope.launch {
            repository.updateBookmark(articleId, !currentState)
        }
    }

    fun toggleReaction(article: Article) {
        viewModelScope.launch {
            repository.toggleReaction(article)
        }
    }

    fun togglePin(article: Article) {
        viewModelScope.launch {
            repository.togglePinArticle(article)
        }
    }

    fun deleteArticle(article: Article) {
        viewModelScope.launch {
            repository.deleteArticleGlobally(article)
        }
    }

    fun clearReadingHistory() {
        viewModelScope.launch {
            repository.clearReadingHistory()
        }
    }

    fun saveUserNote(articleId: Int, note: String) {
        viewModelScope.launch {
            repository.saveUserNote(articleId, note)
        }
    }

    fun exportUserBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson(includeAllArticles = false)
            onReady(json)
        }
    }

    fun importUserBackup(jsonString: String, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = repository.importBackupJson(jsonString)
            onComplete(result)
        }
    }

    fun unlockAdmin(password: String): Boolean {
        return prefsManager.verifyAndUnlockAdmin(password)
    }

    fun lockAdmin() {
        prefsManager.lockAdminMode()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        savedScrollIndex = 0
        savedScrollOffset = 0
    }
}
