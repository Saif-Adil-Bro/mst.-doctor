package com.health.knowledge.ui.home

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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val prefsManager: UserPreferencesManager
) : ViewModel() {

    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected category filter
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Active tab: "home" | "search" | "bookmarks"
    private val _activeTab = MutableStateFlow("home")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Admin Mode state
    val isAdminMode: StateFlow<Boolean> = prefsManager.isAdminMode

    // Articles result (instant search with 250ms debounce, pinned first)
    val articles: StateFlow<List<Article>> = combine(
        _searchQuery.debounce(250),
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        when {
            category != null -> repository.getByCategory(category)
            query.isNotBlank() -> repository.searchArticles(query)
            else -> repository.getAllArticles()
        }
    }
    .map { entities -> entities.map { it.toDomain() } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedArticles: StateFlow<List<Article>> = repository.getBookmarked()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.observeActiveCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            _selectedCategory.value = null
        }
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
        _searchQuery.value = ""
    }

    fun onTabSelected(tab: String) {
        _activeTab.value = tab
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

    fun unlockAdmin(password: String): Boolean {
        return prefsManager.verifyAndUnlockAdmin(password)
    }

    fun lockAdmin() {
        prefsManager.lockAdminMode()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _selectedCategory.value = null
    }
}
