package com.health.knowledge.ui.tibbenabawi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.knowledge.data.local.entity.TibbeNabawiEntity
import com.health.knowledge.data.repository.TibbeNabawiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TibbeNabawiViewModel @Inject constructor(
    private val repository: TibbeNabawiRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("সকল")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _onlyBookmarked = MutableStateFlow(false)
    val onlyBookmarked: StateFlow<Boolean> = _onlyBookmarked.asStateFlow()

    val categories: StateFlow<List<String>> = repository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allPosts: StateFlow<List<TibbeNabawiEntity>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // High performance reactive filtered posts
    val filteredPosts: StateFlow<List<TibbeNabawiEntity>> = combine(
        allPosts,
        _searchQuery,
        _selectedCategory,
        _onlyBookmarked
    ) { posts, query, category, bookmarkedOnly ->
        val q = query.trim().lowercase()
        posts.filter { post ->
            val matchesCategory = (category == "সকল" || post.category == category)
            val matchesBookmark = (!bookmarkedOnly || post.isBookmarked)
            val matchesSearch = if (q.isBlank()) true else {
                post.title.lowercase().contains(q) ||
                post.foodName.lowercase().contains(q) ||
                (post.arabicName?.lowercase()?.contains(q) == true) ||
                post.diseases.lowercase().contains(q) ||
                post.searchKeywords.contains(q)
            }
            matchesCategory && matchesBookmark && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = combine(allPosts) { it.first().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bookmarkedCount: StateFlow<Int> = combine(allPosts) { postsList ->
        postsList.first().count { it.isBookmarked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
    }

    fun toggleOnlyBookmarked() {
        _onlyBookmarked.value = !_onlyBookmarked.value
    }

    fun toggleBookmark(post: TibbeNabawiEntity) {
        viewModelScope.launch {
            repository.toggleBookmark(post.id, post.isBookmarked)
        }
    }
}
