package com.biehuale.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.data.repository.TransactionRepository
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.util.Money
import com.biehuale.app.util.Money.toMoneyString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 记账 Tab - ViewModel
 *
 * Phase 1: 支出/收入
 * Phase 2: 加 TRANSFER 转账 + 编辑模式
 */
@HiltViewModel
class RecordViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RecordEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RecordEvent> = _events.asSharedFlow()

    /** 当前是否编辑模式（null = 新建） */
    private val editingId: MutableStateFlow<Long?> = MutableStateFlow(null)
    val isEditing: StateFlow<Long?> = editingId.asStateFlow()

    init {
        combine(
            categoryRepository.observeAllActive(),
            accountRepository.observeActive()
        ) { categories, accounts ->
            categories to accounts
        }.onEach { (categories, accounts) ->
            val mode = _uiState.value.mode
            _uiState.value = _uiState.value.copy(
                expenseCategories = categories.filter { it.type == CategoryType.EXPENSE },
                incomeCategories = categories.filter { it.type == CategoryType.INCOME },
                accounts = accounts,
                selectedCategoryId = _uiState.value.selectedCategoryId
                    ?: categories.firstOrNull { it.type == mode.toCategoryType() }?.id,
                selectedAccountId = _uiState.value.selectedAccountId
                    ?: accounts.firstOrNull()?.id,
                toAccountId = _uiState.value.toAccountId
                    ?: accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id
            )
        }.launchIn(viewModelScope)
    }

    fun loadForEdit(transactionId: Long) {
        if (editingId.value == transactionId) return
        viewModelScope.launch {
            val tx = transactionRepository.getById(transactionId) ?: run {
                _events.emit(RecordEvent.Error("\u4ea4\u6613\u4e0d\u5b58\u5728"))
                return@launch
            }
            if (tx.deletedAt != null) {
                _events.emit(RecordEvent.Error("\u5df2\u5220\u9664\u7684\u8bb0\u5f55\u4e0d\u53ef\u7f16\u8f91\uff0c\u8bf7\u5148\u5728\u56de\u6536\u7ad9\u6062\u590d"))
                return@launch
            }
            editingId.value = transactionId
            val mode = when (tx.type) {
                TransactionType.INCOME -> RecordMode.INCOME
                TransactionType.EXPENSE -> RecordMode.EXPENSE
                TransactionType.TRANSFER -> RecordMode.TRANSFER
            }
            _uiState.value = _uiState.value.copy(
                mode = mode,
                amountDisplay = tx.amount.toMoneyString(),
                selectedCategoryId = tx.categoryId,
                selectedAccountId = tx.accountId,
                toAccountId = tx.toAccountId,
                description = tx.description ?: "",
                occurredAt = tx.occurredAt
            )
        }
    }

    fun resetToNewMode() {
        editingId.value = null
        _uiState.value = _uiState.value.copy(
            mode = RecordMode.EXPENSE,
            amountDisplay = "0",
            description = "",
            occurredAt = System.currentTimeMillis()
        )
    }

    fun onModeChange(mode: RecordMode) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            selectedCategoryId = _uiState.value.run {
                when (mode) {
                    RecordMode.EXPENSE -> expenseCategories.firstOrNull { it.id == selectedCategoryId }?.id
                        ?: expenseCategories.firstOrNull()?.id
                    RecordMode.INCOME -> incomeCategories.firstOrNull { it.id == selectedCategoryId }?.id
                        ?: incomeCategories.firstOrNull()?.id
                    RecordMode.TRANSFER -> null
                }
            },
            toAccountId = _uiState.value.run {
                if (mode == RecordMode.TRANSFER && toAccountId == selectedAccountId) {
                    accounts.firstOrNull { it.id != selectedAccountId }?.id
                } else toAccountId
            }
        )
    }

    fun onDigit(digit: Char) {
        val current = _uiState.value
        if (current.isSaving) return
        if (digit == '.') {
            if (current.amountDisplay.contains('.')) return
            val withDot = if (current.amountDisplay.isEmpty() || current.amountDisplay == "0") "0." else current.amountDisplay + "."
            _uiState.value = current.copy(amountDisplay = withDot)
            return
        }
        val base = if (current.amountDisplay == "0") "" else current.amountDisplay
        val newDisplay = base + digit
        val dotIndex = newDisplay.indexOf('.')
        if (dotIndex >= 0 && newDisplay.length - dotIndex - 1 > 2) return
        val probe = Money.parseToCents(newDisplay)
        if (probe != null && probe > Money.MAX_CENTS) return
        _uiState.value = current.copy(amountDisplay = newDisplay.ifEmpty { "0" })
    }

    fun onDeleteChar() {
        val current = _uiState.value
        if (current.amountDisplay.length <= 1) {
            _uiState.value = current.copy(amountDisplay = "0")
            return
        }
        _uiState.value = current.copy(
            amountDisplay = current.amountDisplay.dropLast(1).ifEmpty { "0" }
        )
    }

    fun onClear() {
        _uiState.value = _uiState.value.copy(amountDisplay = "0")
    }

    fun onCategorySelect(id: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = id)
    }

    fun onAccountSelect(id: Long) {
        val current = _uiState.value
        if (current.mode == RecordMode.TRANSFER && id == current.toAccountId) {
            val fallback = current.accounts.firstOrNull { it.id != id }?.id
            _uiState.value = current.copy(
                selectedAccountId = id,
                toAccountId = fallback
            )
        } else {
            _uiState.value = current.copy(selectedAccountId = id)
        }
    }

    fun onToAccountSelect(id: Long) {
        val current = _uiState.value
        if (id == current.selectedAccountId) {
            _events.tryEmit(RecordEvent.Error("转入账户必须与转出不同"))
            return
        }
        _uiState.value = current.copy(toAccountId = id)
    }

    fun onDescriptionChange(text: String) {
        if (text.length > MAX_DESCRIPTION) return
        _uiState.value = _uiState.value.copy(description = text)
    }

    fun onTimeChange(millis: Long) {
        _uiState.value = _uiState.value.copy(occurredAt = millis)
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving) return
        val validated = validate(state)
        if (validated is RecordValidation.Error) {
            _events.tryEmit(RecordEvent.Error(validated.message))
            return
        }
        val ok = validated as RecordValidation.Ok

        viewModelScope.launch {
            val wasEditing = editingId.value != null
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val type = when (state.mode) {
                    RecordMode.EXPENSE -> TransactionType.EXPENSE
                    RecordMode.INCOME -> TransactionType.INCOME
                    RecordMode.TRANSFER -> TransactionType.TRANSFER
                }

                val editId = this@RecordViewModel.editingId.value
                val transaction = if (editId != null) {
                    val existing = transactionRepository.getById(editId)
                        ?: throw IllegalStateException("\u539f\u4ea4\u6613\u4e0d\u5b58\u5728")
                    if (existing.deletedAt != null) {
                        throw IllegalStateException("\u5df2\u5220\u9664\u7684\u8bb0\u5f55\u4e0d\u53ef\u7f16\u8f91\uff0c\u8bf7\u5148\u5728\u56de\u6536\u7ad9\u6062\u590d")
                    }
                    existing.copy(
                        amount = ok.cents,
                        type = type,
                        categoryId = ok.categoryId,
                        accountId = ok.accountId,
                        toAccountId = ok.toAccountId,
                        description = state.description.takeIf { it.isNotBlank() },
                        occurredAt = state.occurredAt,
                        deletedAt = null
                    )
                } else {
                    TransactionEntity(
                        id = 0L,
                        amount = ok.cents,
                        type = type,
                        categoryId = ok.categoryId,
                        accountId = ok.accountId,
                        toAccountId = ok.toAccountId,
                        description = state.description.takeIf { it.isNotBlank() },
                        occurredAt = state.occurredAt,
                        createdAt = 0L,
                        updatedAt = 0L,
                        deletedAt = null
                    )
                }
                transactionRepository.save(transaction)
                _uiState.value = _uiState.value.copy(
                    amountDisplay = "0",
                    description = "",
                    occurredAt = System.currentTimeMillis()
                )
                _events.emit(RecordEvent.SaveSuccess(wasEditing = wasEditing))
                if (wasEditing) {
                    this@RecordViewModel.editingId.value = null
                }
            } catch (e: Exception) {
                _events.emit(RecordEvent.Error(e.message ?: "保存失败"))
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    companion object {
        const val MAX_DESCRIPTION = 200

        internal fun validate(state: RecordUiState): RecordValidation {
            if (state.accounts.size < if (state.mode == RecordMode.TRANSFER) 2 else 1) {
                val need = if (state.mode == RecordMode.TRANSFER) "至少 2 个" else "1 个"
                return RecordValidation.Error("请先到设置新建账户（$need）")
            }
            val cents = Money.parseToCents(state.amountDisplay)
            if (cents == null || cents <= 0L) {
                return RecordValidation.Error("请输入有效金额")
            }
            if (cents > Money.MAX_CENTS) {
                return RecordValidation.Error("金额超出上限")
            }
            val accountId = state.selectedAccountId
                ?: return RecordValidation.Error(
                    "请选择${if (state.mode == RecordMode.TRANSFER) "转出账户" else "账户"}"
                )
            val categoryId = when (state.mode) {
                RecordMode.TRANSFER -> null
                else -> state.selectedCategoryId
                    ?: return RecordValidation.Error("请选择类别")
            }
            val toAccountId = when (state.mode) {
                RecordMode.TRANSFER -> state.toAccountId
                    ?: return RecordValidation.Error("请选择转入账户")
                else -> null
            }
            if (state.mode == RecordMode.TRANSFER && toAccountId == accountId) {
                return RecordValidation.Error("转入账户必须与转出不同")
            }
            return RecordValidation.Ok(
                cents = cents,
                accountId = accountId,
                categoryId = categoryId,
                toAccountId = toAccountId
            )
        }
    }
}

sealed interface RecordValidation {
    data class Ok(
        val cents: Long,
        val accountId: Long,
        val categoryId: Long?,
        val toAccountId: Long?
    ) : RecordValidation

    data class Error(val message: String) : RecordValidation
}

enum class RecordMode { EXPENSE, INCOME, TRANSFER }

fun RecordMode.toCategoryType(): CategoryType = when (this) {
    RecordMode.EXPENSE -> CategoryType.EXPENSE
    RecordMode.INCOME -> CategoryType.INCOME
    RecordMode.TRANSFER -> CategoryType.EXPENSE
}

fun RecordMode.toType(): TransactionType = when (this) {
    RecordMode.EXPENSE -> TransactionType.EXPENSE
    RecordMode.INCOME -> TransactionType.INCOME
    RecordMode.TRANSFER -> TransactionType.TRANSFER
}

data class RecordUiState(
    val mode: RecordMode = RecordMode.EXPENSE,
    val amountDisplay: String = "0",
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val toAccountId: Long? = null,
    val description: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false
) {
    val amountCents: Long
        get() = Money.parseToCents(amountDisplay) ?: 0L

    val currentCategories: List<CategoryEntity>
        get() = when (mode) {
            RecordMode.EXPENSE -> expenseCategories
            RecordMode.INCOME -> incomeCategories
            RecordMode.TRANSFER -> emptyList()
        }

    val canSave: Boolean
        get() {
            if (isSaving) return false
            if (amountCents !in 1..Money.MAX_CENTS) return false
            if (selectedAccountId == null) return false
            return when (mode) {
                RecordMode.TRANSFER -> toAccountId != null && toAccountId != selectedAccountId
                else -> selectedCategoryId != null
            }
        }
}

sealed interface RecordEvent {
    data class SaveSuccess(val wasEditing: Boolean) : RecordEvent
    data class Error(val message: String) : RecordEvent
}
