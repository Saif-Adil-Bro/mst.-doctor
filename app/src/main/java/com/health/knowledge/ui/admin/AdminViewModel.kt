package com.health.knowledge.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.knowledge.data.repository.ArticleRepository
import com.health.knowledge.domain.model.Article
import com.health.knowledge.domain.model.toDomain
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val prefsManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _manageQuery = MutableStateFlow("")
    val manageQuery: StateFlow<String> = _manageQuery.asStateFlow()

    // Filter: "all" | "pinned" | "firebase"
    private val _manageFilter = MutableStateFlow("all")
    val manageFilter: StateFlow<String> = _manageFilter.asStateFlow()

    val managedArticles: StateFlow<List<Article>> = combine(
        _manageQuery.debounce(200),
        _manageFilter
    ) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, _) ->
        repository.searchArticles(query)
    }.combine(_manageFilter) { entities, filter ->
        val domainList = entities.map { it.toDomain() }
        when (filter) {
            "pinned" -> domainList.filter { it.isPinned }
            "firebase" -> domainList.filter { it.syncedFromFirebase }
            else -> domainList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onManageQueryChange(query: String) {
        _manageQuery.value = query
    }

    fun onManageFilterChange(filter: String) {
        _manageFilter.value = filter
    }

    fun submitArticle(
        title: String,
        content: String,
        medicines: String,
        selectedCategories: List<String>,
        isPinned: Boolean
    ) {
        if (title.isBlank() || content.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "শিরোনাম এবং বিষয়বস্তু পূরণ করুন")
            return
        }

        viewModelScope.launch {
            _uiState.value = AdminUiState(isLoading = true)
            val result = repository.addArticleToFirebase(
                title = title.trim(),
                content = content.trim(),
                medicines = medicines.trim(),
                categories = selectedCategories.joinToString(","),
                date = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date()),
                isPinned = isPinned
            )
            _uiState.value = if (result.isSuccess) {
                AdminUiState(isSuccess = true, message = "✅ সফলভাবে পোস্ট যোগ হয়েছে!")
            } else {
                AdminUiState(error = "আপলোড ব্যর্থ হয়েছে।")
            }
        }
    }

    fun togglePin(article: Article) {
        viewModelScope.launch {
            repository.togglePinArticle(article)
            val msg = if (!article.isPinned) "📌 পোস্ট পিন করা হয়েছে" else "পিন সরানো হয়েছে"
            _uiState.value = AdminUiState(isSuccess = true, message = msg)
        }
    }

    fun updateArticle(
        article: Article,
        title: String,
        content: String,
        medicines: String,
        categories: String
    ) {
        if (title.isBlank() || content.isBlank()) {
            _uiState.value = AdminUiState(error = "শিরোনাম এবং বিষয়বস্তু খালি রাখা যাবে না")
            return
        }
        viewModelScope.launch {
            _uiState.value = AdminUiState(isLoading = true)
            repository.updateArticleGlobally(
                article = article,
                title = title.trim(),
                content = content.trim(),
                medicines = medicines.trim(),
                categories = categories.trim(),
                isReviewed = true
            )
            _uiState.value = AdminUiState(isSuccess = true, message = "✅ পোস্ট আপডেট ও রিভিউ সম্পন্ন হয়েছে!")
        }
    }

    fun deleteArticle(article: Article) {
        viewModelScope.launch {
            repository.deleteArticleGlobally(article)
            _uiState.value = AdminUiState(isSuccess = true, message = "🗑️ পোস্ট গ্লোবালি ডিলিট করা হয়েছে!")
        }
    }

    fun changePassword(newPassword: String): Boolean {
        val ok = prefsManager.changeAdminPassword(newPassword)
        if (ok) {
            _uiState.value = AdminUiState(isSuccess = true, message = "🔑 অ্যাডমিন পাসওয়ার্ড পরিবর্তন হয়েছে!")
        } else {
            _uiState.value = AdminUiState(error = "পাসওয়ার্ড কমপক্ষে ৪ অক্ষরের হতে হবে")
        }
        return ok
    }

    fun lockAdminMode() {
        prefsManager.lockAdminMode()
    }

    fun clearState() {
        _uiState.value = AdminUiState()
    }
}
