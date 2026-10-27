package com.health.knowledge.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.knowledge.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val repository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState

    fun submitArticle(
        title: String,
        content: String,
        medicines: String,
        selectedCategories: List<String>
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
                medicines = medicines,
                categories = selectedCategories.joinToString(","),
                date = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    .format(java.util.Date())
            )
            _uiState.value = if (result.isSuccess) {
                AdminUiState(isSuccess = true)
            } else {
                AdminUiState(error = "আপলোড ব্যর্থ হয়েছে। ইন্টারনেট সংযোগ পরীক্ষা করুন।")
            }
        }
    }

    fun clearState() {
        _uiState.value = AdminUiState()
    }
}
