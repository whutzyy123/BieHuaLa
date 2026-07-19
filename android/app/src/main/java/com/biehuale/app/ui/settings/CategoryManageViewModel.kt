package com.biehuale.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.domain.model.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManageViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val uiState: StateFlow<CategoryManageUiState> = categoryRepository.observeAllActive()
        .map { categories ->
            CategoryManageUiState(
                expenseCategories = categories.filter { it.type == CategoryType.EXPENSE },
                incomeCategories = categories.filter { it.type == CategoryType.INCOME },
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CategoryManageUiState(isLoading = true)
        )

    private val _events = MutableSharedFlow<CategoryManageEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CategoryManageEvent> = _events.asSharedFlow()

    fun create(name: String, type: CategoryType, icon: String?, colorHex: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 10) {
            _events.tryEmit(CategoryManageEvent.Error("类别名需 1-10 字符"))
            return
        }
        viewModelScope.launch {
            try {
                categoryRepository.create(
                    name = trimmed,
                    type = type,
                    icon = icon,
                    colorHex = colorHex
                )
                _events.emit(CategoryManageEvent.Saved)
            } catch (e: Exception) {
                _events.emit(CategoryManageEvent.Error(e.message ?: "创建失败"))
            }
        }
    }

    fun update(id: Long, name: String, icon: String?, colorHex: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > 10) {
            _events.tryEmit(CategoryManageEvent.Error("类别名需 1-10 字符"))
            return
        }
        viewModelScope.launch {
            try {
                val existing = categoryRepository.getById(id) ?: run {
                    _events.emit(CategoryManageEvent.Error("类别不存在"))
                    return@launch
                }
                categoryRepository.update(
                    existing.copy(
                        name = trimmed,
                        icon = icon,
                        colorHex = colorHex
                    )
                )
                _events.emit(CategoryManageEvent.Saved)
            } catch (e: Exception) {
                _events.emit(CategoryManageEvent.Error(e.message ?: "更新失败"))
            }
        }
    }

    fun archive(id: Long) {
        viewModelScope.launch {
            try {
                categoryRepository.archive(id)
                _events.emit(CategoryManageEvent.Archived)
            } catch (e: Exception) {
                _events.emit(CategoryManageEvent.Error(e.message ?: "归档失败"))
            }
        }
    }

    /** PRD §6.4：仅恢复内置，不动自建 */
    fun resetDefaults() {
        viewModelScope.launch {
            try {
                categoryRepository.resetBuiltinDefaults()
                _events.emit(CategoryManageEvent.DefaultsReset)
            } catch (e: Exception) {
                _events.emit(CategoryManageEvent.Error(e.message ?: "重置失败"))
            }
        }
    }
}

data class CategoryManageUiState(
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface CategoryManageEvent {
    data object Saved : CategoryManageEvent
    data object Archived : CategoryManageEvent
    data object DefaultsReset : CategoryManageEvent
    data class Error(val message: String) : CategoryManageEvent
}
