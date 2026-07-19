package com.biehuale.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.QuickRecordEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.data.repository.QuickRecordRepository
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuickRecordManageViewModel @Inject constructor(
    private val quickRecordRepository: QuickRecordRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository
) : ViewModel() {

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val uiState: StateFlow<QuickRecordManageUiState> = combine(
        quickRecordRepository.observeAll(),
        categoryRepository.observeAllActive(),
        accountRepository.observeActive()
    ) { templates, categories, accounts ->
        val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        QuickRecordManageUiState(
            items = templates.map { t ->
                QuickRecordListItem(
                    entity = t,
                    category = categoryMap[t.categoryId],
                    account = accountMap[t.accountId]
                )
            },
            expenseCategories = expenseCategories,
            accounts = accounts,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuickRecordManageUiState(isLoading = true)
    )

    private val _events = MutableSharedFlow<QuickRecordManageEvent>(extraBufferCapacity = 1)
    val events = _events

    fun create(categoryId: Long, accountId: Long, amountYuan: String, description: String) {
        if (!_busy.compareAndSet(false, true)) return
        val cents = Money.parseToCents(amountYuan) ?: run {
            _busy.value = false
            _events.tryEmit(QuickRecordManageEvent.Error("金额格式错误"))
            return
        }
        viewModelScope.launch {
            try {
                quickRecordRepository.create(
                    categoryId = categoryId,
                    accountId = accountId,
                    amountCents = cents,
                    description = description
                )
                _events.emit(QuickRecordManageEvent.Saved)
            } catch (e: Exception) {
                _events.emit(QuickRecordManageEvent.Error(e.message ?: "创建失败"))
            } finally {
                _busy.value = false
            }
        }
    }

    fun update(
        id: Long,
        categoryId: Long,
        accountId: Long,
        amountYuan: String,
        description: String
    ) {
        if (!_busy.compareAndSet(false, true)) return
        val cents = Money.parseToCents(amountYuan) ?: run {
            _busy.value = false
            _events.tryEmit(QuickRecordManageEvent.Error("金额格式错误"))
            return
        }
        viewModelScope.launch {
            try {
                quickRecordRepository.update(
                    id = id,
                    categoryId = categoryId,
                    accountId = accountId,
                    amountCents = cents,
                    description = description
                )
                _events.emit(QuickRecordManageEvent.Saved)
            } catch (e: Exception) {
                _events.emit(QuickRecordManageEvent.Error(e.message ?: "更新失败"))
            } finally {
                _busy.value = false
            }
        }
    }

    fun delete(id: Long) {
        if (!_busy.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                quickRecordRepository.delete(id)
                _events.emit(QuickRecordManageEvent.Deleted)
            } catch (e: Exception) {
                _events.emit(QuickRecordManageEvent.Error(e.message ?: "删除失败"))
            } finally {
                _busy.value = false
            }
        }
    }
}

data class QuickRecordListItem(
    val entity: QuickRecordEntity,
    val category: CategoryEntity?,
    val account: AccountEntity?
)

data class QuickRecordManageUiState(
    val items: List<QuickRecordListItem> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface QuickRecordManageEvent {
    data object Saved : QuickRecordManageEvent
    data object Deleted : QuickRecordManageEvent
    data class Error(val message: String) : QuickRecordManageEvent
}
