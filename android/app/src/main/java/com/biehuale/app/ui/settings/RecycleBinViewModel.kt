package com.biehuale.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 回收站 ViewModel
 *
 * 详见 docs/PRD.md §6.2
 */
@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<RecycleBinEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RecycleBinEvent> = _events.asSharedFlow()

    val uiState: StateFlow<RecycleBinUiState> = combine(
        transactionRepository.observeRecycleBin(),
        categoryRepository.observeAll(),
        accountRepository.observeAll()
    ) { transactions, categories, accounts ->
        val items = transactions.map { tx ->
            RecycleBinItem(
                transaction = tx,
                category = tx.categoryId?.let { id -> categories.firstOrNull { it.id == id } },
                account = accounts.firstOrNull { it.id == tx.accountId },
                daysUntilCleanup = daysUntilCleanup(tx.deletedAt)
            )
        }
        RecycleBinUiState(items = items, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecycleBinUiState(isLoading = true)
    )

    fun restore(id: Long) {
        viewModelScope.launch {
            try {
                if (transactionRepository.restore(id)) {
                    _events.emit(RecycleBinEvent.Message("已恢复"))
                } else {
                    _events.emit(RecycleBinEvent.Error("恢复失败：记录不存在或已不在回收站"))
                }
            } catch (e: Exception) {
                _events.emit(RecycleBinEvent.Error(e.message ?: "恢复失败"))
            }
        }
    }

    fun hardDelete(id: Long) {
        viewModelScope.launch {
            try {
                if (transactionRepository.hardDelete(id)) {
                    _events.emit(RecycleBinEvent.Message("已永久删除"))
                } else {
                    _events.emit(RecycleBinEvent.Error("删除失败：记录不存在"))
                }
            } catch (e: Exception) {
                _events.emit(RecycleBinEvent.Error(e.message ?: "删除失败"))
            }
        }
    }

    fun emptyBin() {
        viewModelScope.launch {
            try {
                transactionRepository.emptyBin()
                _events.emit(RecycleBinEvent.Message("回收站已清空"))
            } catch (e: Exception) {
                _events.emit(RecycleBinEvent.Error(e.message ?: "清空失败"))
            }
        }
    }

    private fun daysUntilCleanup(deletedAt: Long?): Int {
        if (deletedAt == null) return 0
        val threshold = 30L * 24 * 60 * 60 * 1000
        val remaining = (deletedAt + threshold) - System.currentTimeMillis()
        return (remaining / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    }
}

sealed interface RecycleBinEvent {
    data class Message(val text: String) : RecycleBinEvent
    data class Error(val message: String) : RecycleBinEvent
}

data class RecycleBinUiState(
    val items: List<RecycleBinItem> = emptyList(),
    val isLoading: Boolean = true
)

data class RecycleBinItem(
    val transaction: TransactionEntity,
    val category: CategoryEntity?,
    val account: AccountEntity?,
    val daysUntilCleanup: Int
)
