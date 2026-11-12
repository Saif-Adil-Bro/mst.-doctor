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
            val herb = extractCanonicalHerb(post)
            if (herb.isNotBlank() && herb != "অন্যান্য পথ্য") {
                counts[herb] = (counts[herb] ?: 0) + 1
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

    private data class FilterParams(
        val query: String,
        val category: String,
        val onlyBookmarked: Boolean,
        val selectedHerb: String?,
        val selectedDisease: String?
    )

    private val filterParams = combine(
        _searchQuery,
        _selectedCategory,
        _onlyBookmarked,
        _selectedHerb,
        _selectedDisease
    ) { query, category, bookmarkedOnly, selHerb, selDisease ->
        FilterParams(query, category, bookmarkedOnly, selHerb, selDisease)
    }

    // High performance reactive filtered posts
    val filteredPosts: StateFlow<List<TibbeNabawiEntity>> = combine(
        allPosts,
        filterParams
    ) { posts, filters ->
        val q = filters.query.trim().lowercase()
        posts.filter { post ->
            val canonicalHerb = extractCanonicalHerb(post)
            val matchesCategory = (filters.selectedHerb != null || filters.selectedDisease != null || filters.category == "সকল" || post.category == filters.category)
            val matchesBookmark = (!filters.onlyBookmarked || post.isBookmarked)
            val matchesHerb = if (filters.selectedHerb == null) true else {
                canonicalHerb.equals(filters.selectedHerb, ignoreCase = true) ||
                post.foodName.contains(filters.selectedHerb, ignoreCase = true) ||
                post.title.contains(filters.selectedHerb, ignoreCase = true) ||
                post.searchKeywords.contains(filters.selectedHerb, ignoreCase = true)
            }
            val matchesDisease = filters.selectedDisease == null || post.diseases.contains(filters.selectedDisease, ignoreCase = true)
            val matchesSearch = if (q.isBlank()) true else {
                post.title.lowercase().contains(q) ||
                post.foodName.lowercase().contains(q) ||
                (post.arabicName?.lowercase()?.contains(q) == true) ||
                post.diseases.lowercase().contains(q) ||
                post.searchKeywords.contains(q) ||
                canonicalHerb.lowercase().contains(q)
            }
            matchesCategory && matchesBookmark && matchesHerb && matchesDisease && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = allPosts
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bookmarkedCount: StateFlow<Int> = allPosts
        .map { postsList -> postsList.count { it.isBookmarked } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String) {
        _selectedCategory.value = category
        if (category != "সকল") {
            _selectedHerb.value = null
            _selectedDisease.value = null
        }
    }

    fun setIndexMode(mode: TibbeIndexMode) {
        _indexMode.value = mode
    }

    fun selectHerb(herb: String?) {
        if (_selectedHerb.value == herb) {
            _selectedHerb.value = null
        } else {
            _selectedHerb.value = herb
            _selectedCategory.value = "সকল"
            _selectedDisease.value = null
        }
    }

    fun selectDisease(disease: String?) {
        if (_selectedDisease.value == disease) {
            _selectedDisease.value = null
        } else {
            _selectedDisease.value = disease
            _selectedCategory.value = "সকল"
            _selectedHerb.value = null
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

/**
 * Extracts a clean, canonical herb/food heading (e.g. মধু, দুধ, মাংস, খেজুর)
 * from a Tibbe Nabawi entity for indexed grouping and card heading display.
 */
fun extractCanonicalHerb(post: TibbeNabawiEntity): String {
    val fn = post.foodName + " " + post.title
    return when {
        fn.contains("মধু") || fn.contains("আসাল") -> "মধু"
        fn.contains("দুধ") || fn.contains("লাবান") -> "দুধ"
        fn.contains("মাংস") || fn.contains("গোশত") || fn.contains("ছারিদ") || fn.contains("চর্বি") -> "মাংস"
        fn.contains("খেজুর") || fn.contains("তামর") || fn.contains("রুতব") || fn.contains("আজওয়া") || fn.contains("বুসর") -> "খেজুর"
        fn.contains("কালোজিরা") || fn.contains("হাববাহ সাওদা") || fn.contains("কালো বীজ") -> "কালোজিরা"
        fn.contains("কোহল") || fn.contains("সুরমা") || fn.contains("ইথমিড") -> "সুরমা (কোহল)"
        fn.contains("জলপাই") || fn.contains("যাইতুন") || fn.contains("তেল") -> "জলপাই ও তেল"
        fn.contains("যব") || fn.contains("বার্লি") || fn.contains("তালবিনা") || fn.contains("শাইর") -> "যব ও বার্লি"
        fn.contains("লাউ") || fn.contains("কদু") || fn.contains("ইয়াকতিন") -> "লাউ"
        fn.contains("আঙ্গুর") || fn.contains("কিসমিস") || fn.contains("ই'নাব") || fn.contains("জাবীব") -> "আঙ্গুর ও কিসমিস"
        fn.contains("ডালিম") || fn.contains("রুম্মান") -> "ডালিম"
        fn.contains("ডুমুর") || fn.contains("তীন") -> "ডুমুর"
        fn.contains("আদা") || fn.contains("জাঞ্জাবিল") -> "আদা"
        fn.contains("মেহেদি") || fn.contains("হেনা") -> "মেহেদি"
        fn.contains("সোনামুখী") || fn.contains("সানা") || fn.contains("সেননা") -> "সোনামুখী"
        fn.contains("সিরকা") || fn.contains("ভিনেগার") || fn.contains("খাল্লা") -> "সিরকা (ভিনেগার)"
        fn.contains("মাছ") || fn.contains("সামোক") -> "মাছ"
        fn.contains("ডিম") || fn.contains("বাইদ") -> "ডিম"
        fn.contains("লেবু") || fn.contains("উতরুজ") -> "লেবু"
        fn.contains("শসা") || fn.contains("কিসসা") -> "শসা"
        fn.contains("রসুন") || fn.contains("ছাউম") -> "রসুন"
        fn.contains("পেঁয়াজ") || fn.contains("বাসাল") -> "পেঁয়াজ"
        fn.contains("মেথি") || fn.contains("হালবাহ") -> "মেথি"
        fn.contains("সুগন্ধি") || fn.contains("মিস্ক") || fn.contains("কস্তুরী") -> "সুগন্ধি ও মিস্ক"
        else -> {
            val raw = post.foodName.trim()
            val clean = raw.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
            clean.ifBlank { "অন্যান্য পথ্য" }
        }
    }
}
