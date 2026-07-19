package com.biehuale.app.ui.detail

import androidx.lifecycle.SavedStateHandle
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
 * 流水详情 ViewModel
 *
 * 详见 docs/DEV_PLAN.md §5 Task 2.5
 *
 * 职责：
 *  - 加载交易 + 关联的类别 + 账户
 *  - 软删除（弹确认后）
 *  - 发事件（删除成功 / 错误）
 */
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * 从路由参数取 transactionId
     */
    private val transactionId: Long = savedStateHandle.get<Long>("transactionId") ?: 0L

    /**
     * 详情 UI 状态（合并 4 个数据源）
     */
    val uiState: StateFlow<TransactionDetailUiState> = combine(
        transactionRepository.observeById(transactionId),
        categoryRepository.observeAll(),
        accountRepository.observeAll()
    ) { transaction, categories, accounts ->
        if (transaction == null || transaction.deletedAt != null) {
            // 软删后不可再编辑；视为不存在（回收站入口另走）
            TransactionDetailUiState(isLoading = false, notFound = true)
        } else {
            TransactionDetailUiState(
                transaction = transaction,
                category = transaction.categoryId?.let { id -> categories.firstOrNull { it.id == id } },
                account = accounts.firstOrNull { it.id == transaction.accountId },
                toAccount = transaction.toAccountId?.let { id -> accounts.firstOrNull { it.id == id } },
                isLoading = false,
                notFound = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionDetailUiState(isLoading = true)
    )

    private val _events = MutableSharedFlow<TransactionDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<TransactionDetailEvent> = _events.asSharedFlow()

    /**
     * 软删除
     */
    fun softDelete() {
        viewModelScope.launch {
            try {
                transactionRepository.softDelete(transactionId)
                _events.emit(TransactionDetailEvent.Deleted)
            } catch (e: Exception) {
                _events.emit(TransactionDetailEvent.Error(e.message ?: "删除失败"))
            }
        }
    }
}

data class TransactionDetailUiState(
    val transaction: TransactionEntity? = null,
    val category: CategoryEntity? = null,
    val account: AccountEntity? = null,
    val toAccount: AccountEntity? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false
)

sealed interface TransactionDetailEvent {
    data object Deleted : TransactionDetailEvent
    data class Error(val message: String) : TransactionDetailEvent
}
