package com.health.knowledge.ui.tibbenabawi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.knowledge.data.local.entity.TibbeNabawiEntity
import com.health.knowledge.data.repository.TibbeNabawiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TibbeNabawiDetailViewModel @Inject constructor(
    private val repository: TibbeNabawiRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: Int = savedStateHandle.get<Int>("id") ?: 1

    val post: StateFlow<TibbeNabawiEntity?> = repository.getById(postId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _relatedPosts = MutableStateFlow<List<TibbeNabawiEntity>>(emptyList())
    val relatedPosts: StateFlow<List<TibbeNabawiEntity>> = _relatedPosts.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getById(postId).collect { current ->
                if (current != null) {
                    repository.getByCategory(current.category).collect { categoryPosts ->
                        _relatedPosts.value = categoryPosts
                            .filter { it.id != current.id }
                            .take(5)
                    }
                }
            }
        }
    }

    fun toggleBookmark() {
        val current = post.value ?: return
        viewModelScope.launch {
            repository.toggleBookmark(current.id, current.isBookmarked)
        }
    }
}
