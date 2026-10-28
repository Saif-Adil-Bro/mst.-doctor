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

    // Related articles based on shared categories or herbal ingredients
    val relatedArticles: StateFlow<List<Article>> = combine(
        article.filterNotNull(),
        repository.getAllArticles()
    ) { current, allEntities ->
        allEntities
            .asSequence()
            .filter { it.id != current.id }
            .map { it.toDomain() }
            .map { candidate ->
                val sharedMeds = candidate.medicines.count { med ->
                    current.medicines.any { it.equals(med, ignoreCase = true) }
                }
                val sharedCats = candidate.categories.count { cat ->
                    current.categories.any { it.equals(cat, ignoreCase = true) }
                }
                val score = sharedMeds * 3 + sharedCats * 2
                candidate to score
            }
            .filter { it.second > 0 }
            .sortedWith(
                compareByDescending<Pair<Article, Int>> { it.second }
                    .thenByDescending { it.first.reactions }
            )
            .take(5)
            .map { it.first }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fontSizeSp: StateFlow<Float> = prefsManager.fontSizeSp
    val themeMode: StateFlow<String> = prefsManager.themeMode
    val isAdminMode: StateFlow<Boolean> = prefsManager.isAdminMode

    fun loadArticle(id: Int) {
        _articleId.value = id
        viewModelScope.launch {
            repository.markArticleRead(id)
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

    fun setThemeMode(mode: String) {
        prefsManager.setThemeMode(mode)
    }

    fun cycleThemeMode(): String {
        return prefsManager.cycleThemeMode()
    }

    fun saveUserNote(note: String) {
        val id = _articleId.value ?: return
        viewModelScope.launch {
            repository.saveUserNote(id, note)
        }
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

    fun toggleVerification(article: Article) {
        viewModelScope.launch {
            repository.toggleVerification(article)
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
