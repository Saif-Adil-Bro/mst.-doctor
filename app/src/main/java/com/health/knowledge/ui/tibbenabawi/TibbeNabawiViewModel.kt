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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TibbeIndexMode {
    ALL,       // সব পোস্ট ও ক্যাটাগরি
    HERBS,     // ভেষজ ও উপাদান সূচিপত্র
    DISEASES   // রোগভিত্তিক সূচিপত্র
}

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

    private val _indexMode = MutableStateFlow(TibbeIndexMode.ALL)
    val indexMode: StateFlow<TibbeIndexMode> = _indexMode.asStateFlow()

    private val _selectedHerb = MutableStateFlow<String?>(null)
    val selectedHerb: StateFlow<String?> = _selectedHerb.asStateFlow()

    private val _selectedDisease = MutableStateFlow<String?>(null)
    val selectedDisease: StateFlow<String?> = _selectedDisease.asStateFlow()

    val categories: StateFlow<List<String>> = repository.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPosts: StateFlow<List<TibbeNabawiEntity>> = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregated Herb / Food index with counts sorted by popularity
    val herbIndexList: StateFlow<List<Pair<String, Int>>> = allPosts.map { posts ->
        val counts = mutableMapOf<String, Int>()
        for (post in posts) {
            val raw = post.foodName.trim()
            if (raw.isNotBlank()) {
                val clean = raw.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
                if (clean.length >= 2) {
                    counts[clean] = (counts[clean] ?: 0) + 1
                }
            }
        }
        counts.entries
            .map { it.key to it.value }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregated Disease index with counts sorted by frequency
    val diseaseIndexList: StateFlow<List<Pair<String, Int>>> = allPosts.map { posts ->
        val counts = mutableMapOf<String, Int>()
        for (post in posts) {
            if (post.diseases.isNotBlank()) {
                val items = post.diseases.split(",").map { it.trim() }.filter { it.length >= 2 }
                for (d in items) {
                    counts[d] = (counts[d] ?: 0) + 1
                }
            }
        }
        counts.entries
            .map { it.key to it.value }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // High performance reactive filtered posts
    val filteredPosts: StateFlow<List<TibbeNabawiEntity>> = combine(
        allPosts,
        _searchQuery,
        _selectedCategory,
        _onlyBookmarked,
        _selectedHerb,
        _selectedDisease
    ) { posts, query, category, bookmarkedOnly, selHerb, selDisease ->
        val q = query.trim().lowercase()
        posts.filter { post ->
            val matchesCategory = (category == "সকল" || post.category == category)
            val matchesBookmark = (!bookmarkedOnly || post.isBookmarked)
            val matchesHerb = selHerb == null || post.foodName.contains(selHerb, ignoreCase = true)
            val matchesDisease = selDisease == null || post.diseases.contains(selDisease, ignoreCase = true)
            val matchesSearch = if (q.isBlank()) true else {
                post.title.lowercase().contains(q) ||
                post.foodName.lowercase().contains(q) ||
                (post.arabicName?.lowercase()?.contains(q) == true) ||
                post.diseases.lowercase().contains(q) ||
                post.searchKeywords.contains(q)
            }
            matchesCategory && matchesBookmark && matchesHerb && matchesDisease && matchesSearch
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

    fun setIndexMode(mode: TibbeIndexMode) {
        _indexMode.value = mode
    }

    fun selectHerb(herb: String?) {
        if (_selectedHerb.value == herb) {
            _selectedHerb.value = null
        } else {
            _selectedHerb.value = herb
        }
    }

    fun selectDisease(disease: String?) {
        if (_selectedDisease.value == disease) {
            _selectedDisease.value = null
        } else {
            _selectedDisease.value = disease
        }
    }

    fun toggleOnlyBookmarked() {
        _onlyBookmarked.value = !_onlyBookmarked.value
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = "সকল"
        _selectedHerb.value = null
        _selectedDisease.value = null
        _onlyBookmarked.value = false
        _indexMode.value = TibbeIndexMode.ALL
    }

    fun toggleBookmark(post: TibbeNabawiEntity) {
        viewModelScope.launch {
            repository.toggleBookmark(post.id, post.isBookmarked)
        }
    }
}
