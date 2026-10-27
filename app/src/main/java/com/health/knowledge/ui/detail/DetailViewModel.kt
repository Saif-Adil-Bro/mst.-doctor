package com.health.knowledge.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.knowledge.data.repository.ArticleRepository
import com.health.knowledge.domain.model.Article
import com.health.knowledge.domain.model.toDomain
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val prefsManager: UserPreferencesManager
) : ViewModel() {

    private val _articleId = MutableStateFlow<Int?>(null)

    val article: StateFlow<Article?> = _articleId
        .filterNotNull()
        .flatMapLatest { id -> repository.observeById(id) }
        .map { it?.toDomain() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val fontSizeSp: StateFlow<Float> = prefsManager.fontSizeSp
    val isAdminMode: StateFlow<Boolean> = prefsManager.isAdminMode

    fun loadArticle(id: Int) {
        _articleId.value = id
        viewModelScope.launch {
            repository.getById(id)?.toDomain()?.let { loaded ->
                repository.refreshLiveReactions(loaded)
            }
        }
    }

    fun increaseFontSize() {
        prefsManager.increaseFontSize()
    }

    fun decreaseFontSize() {
        prefsManager.decreaseFontSize()
    }

    fun resetFontSize() {
        prefsManager.resetFontSize()
    }

    fun toggleBookmark(id: Int, currentState: Boolean) {
        viewModelScope.launch {
            repository.updateBookmark(id, !currentState)
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

    fun deleteArticle(article: Article, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteArticleGlobally(article)
            onDeleted()
        }
    }
}
