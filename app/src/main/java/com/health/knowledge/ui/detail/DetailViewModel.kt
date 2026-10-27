package com.health.knowledge.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.knowledge.data.repository.ArticleRepository
import com.health.knowledge.domain.model.Article
import com.health.knowledge.domain.model.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {
    private val _article = MutableStateFlow<Article?>(null)
    val article: StateFlow<Article?> = _article

    fun loadArticle(id: Int) {
        viewModelScope.launch {
            _article.value = repository.getById(id)?.toDomain()
        }
    }

    fun toggleBookmark(id: Int, currentState: Boolean) {
        viewModelScope.launch {
            repository.updateBookmark(id, !currentState)
            _article.value = _article.value?.copy(isBookmarked = !currentState)
        }
    }
}
