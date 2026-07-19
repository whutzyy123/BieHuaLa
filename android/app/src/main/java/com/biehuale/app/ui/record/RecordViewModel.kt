package com.biehuale.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.QuickRecordEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.data.repository.AccountRepository
import com.biehuale.app.data.repository.CategoryRepository
import com.biehuale.app.data.repository.QuickRecordRepository
import com.biehuale.app.data.repository.TransactionRepository
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.util.Money
import com.biehuale.app.util.Money.toMoneyString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 记账 Tab - ViewModel
 *
 * Phase 1: 支出/收入
 * Phase 2: 加 TRANSFER 转账 + 编辑模式
 * Phase+: 快速记账一键落账（支出）
 */
@HiltViewModel
class RecordViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val quickRecordRepository: QuickRecordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordUiState())
    /** 完整状态（测试 / 校验用）；UI 请订阅细粒度 Flow，避免键盘改数整屏重组 */
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    val amountDisplay: StateFlow<String> = _uiState
        .map { it.amountDisplay }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "0")

    val canSave: StateFlow<Boolean> = _uiState
        .map { it.computeCanSave() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isSaving: StateFlow<Boolean> = _uiState
        .map { it.isSaving }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 表单区（不含金额），金额变更时尽量不触发重组 */
    val formState: StateFlow<RecordFormState> = _uiState
        .map { it.toFormState() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordFormState())

    private val _events = MutableSharedFlow<RecordEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RecordEvent> = _events.asSharedFlow()

    /** 当前是否编辑模式（null = 新建） */
    private val editingId: MutableStateFlow<Long?> = MutableStateFlow(null)
    val isEditing: StateFlow<Long?> = editingId.asStateFlow()

    init {
        combine(
            categoryRepository.observeAllActive(),
            accountRepository.observeActive(),
            quickRecordRepository.observeAll()
        ) { categories, accounts, quickRecords ->
            val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
            val incomeCategories = categories.filter { it.type == CategoryType.INCOME }
            val categoryMap = categories.associateBy { it.id }
            val accountIds = accounts.map { it.id }.toSet()
            val usableQuickRecords = quickRecords.mapNotNull { entity ->
                val category = categoryMap[entity.categoryId]
                if (category == null ||
                    category.type != CategoryType.EXPENSE ||
                    entity.accountId !in accountIds ||
                    entity.amount !in 1..Money.MAX_CENTS
                ) {
                    null
                } else {
                    QuickRecordOption(entity = entity, category = category)
                }
            }
            RecordCatalog(
                expenseCategories = expenseCategories,
                incomeCategories = incomeCategories,
                accounts = accounts,
                accountIds = accountIds,
                quickRecords = usableQuickRecords
            )
        }
            .flowOn(Dispatchers.Default)
            .onEach { catalog ->
                _uiState.update { prev ->
                    val modeCategories = when (prev.mode) {
                        RecordMode.EXPENSE -> catalog.expenseCategories
                        RecordMode.INCOME -> catalog.incomeCategories
                        RecordMode.TRANSFER -> emptyList()
                    }
                    // 失效 id 置 null 强制重选，勿静默换成第一条（避免改账误记）
                    val selectedCategoryId = when {
                        prev.selectedCategoryId == null -> modeCategories.firstOrNull()?.id
                        modeCategories.any { it.id == prev.selectedCategoryId } ->
                            prev.selectedCategoryId
                        else -> null
                    }
                    val selectedAccountId = when {
                        prev.selectedAccountId == null -> catalog.accounts.firstOrNull()?.id
                        prev.selectedAccountId in catalog.accountIds -> prev.selectedAccountId
                        else -> null
                    }
                    val toAccountId = when {
                        prev.toAccountId == null ->
                            catalog.accounts.firstOrNull { it.id != selectedAccountId }?.id
                                ?: catalog.accounts.firstOrNull()?.id
                        prev.toAccountId in catalog.accountIds &&
                            prev.toAccountId != selectedAccountId ->
                            prev.toAccountId
                        else ->
                            catalog.accounts.firstOrNull { it.id != selectedAccountId }?.id
                    }
                    prev.copy(
                        expenseCategories = catalog.expenseCategories,
                        incomeCategories = catalog.incomeCategories,
                        accounts = catalog.accounts,
                        quickRecords = catalog.quickRecords,
                        selectedCategoryId = selectedCategoryId,
                        selectedAccountId = selectedAccountId,
                        toAccountId = toAccountId
                    )
                }
            }
            .launchIn(viewModelScope)
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
                feeDisplay = if (tx.fee > 0L) tx.fee.toMoneyString() else "0",
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
            feeDisplay = "0",
            description = "",
            occurredAt = System.currentTimeMillis()
        )
    }

    fun onModeChange(mode: RecordMode) {
        if (_uiState.value.isSaving) return
        _uiState.update { current ->
            current.copy(
                mode = mode,
                feeDisplay = if (mode == RecordMode.TRANSFER) current.feeDisplay else "0",
                selectedCategoryId = when (mode) {
                    RecordMode.EXPENSE -> current.expenseCategories
                        .firstOrNull { it.id == current.selectedCategoryId }?.id
                        ?: current.expenseCategories.firstOrNull()?.id
                    RecordMode.INCOME -> current.incomeCategories
                        .firstOrNull { it.id == current.selectedCategoryId }?.id
                        ?: current.incomeCategories.firstOrNull()?.id
                    RecordMode.TRANSFER -> null
                },
                toAccountId = when {
                    mode == RecordMode.TRANSFER &&
                        current.toAccountId == current.selectedAccountId ->
                        current.accounts.firstOrNull { it.id != current.selectedAccountId }?.id
                    else -> current.toAccountId
                }
            )
        }
    }

    fun onFeeChange(raw: String) {
        if (_uiState.value.isSaving) return
        val normalized = normalizeMoneyInput(raw) ?: return
        _uiState.update { it.copy(feeDisplay = normalized) }
    }

    fun onDigit(digit: Char) {
        val current = _uiState.value
        if (current.isSaving) return
        if (digit == '.') {
            if (current.amountDisplay.contains('.')) return
            val withDot = if (current.amountDisplay.isEmpty() || current.amountDisplay == "0") "0." else current.amountDisplay + "."
            _uiState.update { it.copy(amountDisplay = withDot) }
            return
        }
        val base = if (current.amountDisplay == "0") "" else current.amountDisplay
        val newDisplay = base + digit
        val dotIndex = newDisplay.indexOf('.')
        if (dotIndex >= 0 && newDisplay.length - dotIndex - 1 > 2) return
        val probe = Money.parseToCents(newDisplay)
        if (probe != null && probe > Money.MAX_CENTS) return
        _uiState.update { it.copy(amountDisplay = newDisplay.ifEmpty { "0" }) }
    }

    fun onDeleteChar() {
        if (_uiState.value.isSaving) return
        _uiState.update { current ->
            if (current.amountDisplay.length <= 1) {
                current.copy(amountDisplay = "0")
            } else {
                current.copy(amountDisplay = current.amountDisplay.dropLast(1).ifEmpty { "0" })
            }
        }
    }

    fun onClear() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(amountDisplay = "0") }
    }

    fun onCategorySelect(id: Long) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(selectedCategoryId = id) }
    }

    fun onAccountSelect(id: Long) {
        if (_uiState.value.isSaving) return
        _uiState.update { current ->
            if (current.mode == RecordMode.TRANSFER && id == current.toAccountId) {
                val fallback = current.accounts.firstOrNull { it.id != id }?.id
                current.copy(selectedAccountId = id, toAccountId = fallback)
            } else {
                current.copy(selectedAccountId = id)
            }
        }
    }

    fun onToAccountSelect(id: Long) {
        if (_uiState.value.isSaving) return
        val current = _uiState.value
        if (id == current.selectedAccountId) {
            _events.tryEmit(RecordEvent.Error("转入账户必须与转出不同"))
            return
        }
        _uiState.update { it.copy(toAccountId = id) }
    }

    fun onDescriptionChange(text: String) {
        if (_uiState.value.isSaving) return
        if (text.length > MAX_DESCRIPTION) return
        _uiState.update { it.copy(description = text) }
    }

    fun onTimeChange(millis: Long) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(occurredAt = millis) }
    }

    /**
     * 支出 Tab：点选快速记账模板后立刻落一笔账（不经表单）。
     */
    fun onQuickRecord(templateId: Long) {
        if (editingId.value != null) return
        if (!_uiState.compareAndSetSaving(true)) return
        if (_uiState.value.mode != RecordMode.EXPENSE) {
            _uiState.update { it.copy(isSaving = false) }
            _events.tryEmit(RecordEvent.Error("快速记账仅用于支出"))
            return
        }
        viewModelScope.launch {
            try {
                val template = quickRecordRepository.getById(templateId)
                    ?: throw IllegalStateException("快速记账不存在")
                val category = categoryRepository.getById(template.categoryId)
                    ?: throw IllegalStateException("类别不存在或已删除")
                if (category.isArchived || category.type != CategoryType.EXPENSE) {
                    throw IllegalStateException("类别不可用，请到设置更新快速记账")
                }
                val account = accountRepository.getById(template.accountId)
                    ?: throw IllegalStateException("账户不存在或已删除")
                if (account.isArchived) {
                    throw IllegalStateException("账户已归档，请到设置更新快速记账")
                }
                if (template.amount !in 1..Money.MAX_CENTS) {
                    throw IllegalStateException("金额无效")
                }
                val now = System.currentTimeMillis()
                transactionRepository.save(
                    TransactionEntity(
                        id = 0L,
                        amount = template.amount,
                        type = TransactionType.EXPENSE,
                        categoryId = template.categoryId,
                        accountId = template.accountId,
                        toAccountId = null,
                        description = template.description,
                        occurredAt = now,
                        createdAt = 0L,
                        updatedAt = 0L,
                        deletedAt = null
                    )
                )
                _uiState.update {
                    it.copy(
                        amountDisplay = "0",
                        description = "",
                        occurredAt = System.currentTimeMillis()
                    )
                }
                _events.emit(RecordEvent.SaveSuccess(wasEditing = false))
            } catch (e: Exception) {
                _events.emit(RecordEvent.Error(e.message ?: "快速记账失败"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
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
        // 在 launch 前同步置位，防止连点双 insert；用 update 避免覆盖 combine 刷新
        if (!_uiState.compareAndSetSaving(true)) return

        val description = state.description
        val occurredAt = state.occurredAt
        val mode = state.mode

        viewModelScope.launch {
            val wasEditing = editingId.value != null
            try {
                val type = when (mode) {
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
                        fee = ok.feeCents,
                        description = description.takeIf { it.isNotBlank() },
                        occurredAt = occurredAt,
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
                        fee = ok.feeCents,
                        description = description.takeIf { it.isNotBlank() },
                        occurredAt = occurredAt,
                        createdAt = 0L,
                        updatedAt = 0L,
                        deletedAt = null
                    )
                }
                transactionRepository.save(transaction)
                _uiState.update {
                    it.copy(
                        amountDisplay = "0",
                        feeDisplay = "0",
                        description = "",
                        occurredAt = System.currentTimeMillis()
                    )
                }
                _events.emit(RecordEvent.SaveSuccess(wasEditing = wasEditing))
                if (wasEditing) {
                    this@RecordViewModel.editingId.value = null
                }
            } catch (e: Exception) {
                _events.emit(RecordEvent.Error(e.message ?: "保存失败"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    companion object {
        const val MAX_DESCRIPTION = 200

        /** 允许空 / 数字 / 一位小数点；非法输入返回 null 表示忽略 */
        internal fun normalizeMoneyInput(raw: String): String? {
            if (raw.isEmpty()) return "0"
            if (raw.any { it !in '0'..'9' && it != '.' }) return null
            if (raw.count { it == '.' } > 1) return null
            val dot = raw.indexOf('.')
            if (dot >= 0 && raw.length - dot - 1 > 2) return null
            if (raw.startsWith(".")) return "0$raw"
            val probe = Money.parseToCents(
                if (raw.endsWith(".")) raw + "0" else raw
            )
            if (probe != null && probe > Money.MAX_CENTS) return null
            // 去掉多余前导零，保留 "0" / "0.x"
            val trimmed = when {
                raw == "0" || raw.startsWith("0.") -> raw
                raw.all { it == '0' } -> "0"
                raw.startsWith("0") && !raw.startsWith("0.") -> raw.trimStart('0').ifEmpty { "0" }
                else -> raw
            }
            return trimmed
        }

        internal fun parseFeeCents(feeDisplay: String): Long? {
            val raw = feeDisplay.trim().ifEmpty { "0" }
            return Money.parseToCents(if (raw.endsWith(".")) raw + "0" else raw)
        }

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
            if (state.accounts.none { it.id == accountId }) {
                return RecordValidation.Error("请重新选择账户")
            }
            val categoryId = when (state.mode) {
                RecordMode.TRANSFER -> null
                else -> {
                    val id = state.selectedCategoryId
                        ?: return RecordValidation.Error("请选择类别")
                    if (state.currentCategories.none { it.id == id }) {
                        return RecordValidation.Error("请重新选择类别")
                    }
                    id
                }
            }
            val toAccountId = when (state.mode) {
                RecordMode.TRANSFER -> {
                    val id = state.toAccountId
                        ?: return RecordValidation.Error("请选择转入账户")
                    if (state.accounts.none { it.id == id }) {
                        return RecordValidation.Error("请重新选择转入账户")
                    }
                    id
                }
                else -> null
            }
            if (state.mode == RecordMode.TRANSFER && toAccountId == accountId) {
                return RecordValidation.Error("转入账户必须与转出不同")
            }
            val feeCents = when (state.mode) {
                RecordMode.TRANSFER -> {
                    val fee = parseFeeCents(state.feeDisplay)
                        ?: return RecordValidation.Error("请输入有效手续费")
                    if (fee < 0L) {
                        return RecordValidation.Error("手续费不能为负")
                    }
                    if (fee >= cents) {
                        return RecordValidation.Error("手续费必须小于转账金额")
                    }
                    fee
                }
                else -> 0L
            }
            return RecordValidation.Ok(
                cents = cents,
                feeCents = feeCents,
                accountId = accountId,
                categoryId = categoryId,
                toAccountId = toAccountId
            )
        }
    }
}

/** 仅当当前未在保存时成功置位，避免连点与 combine 竞态覆盖 */
private fun MutableStateFlow<RecordUiState>.compareAndSetSaving(saving: Boolean): Boolean {
    while (true) {
        val prev = value
        if (saving && prev.isSaving) return false
        if (!saving && !prev.isSaving) return false
        if (compareAndSet(prev, prev.copy(isSaving = saving))) return true
    }
}

sealed interface RecordValidation {
    data class Ok(
        val cents: Long,
        val feeCents: Long = 0L,
        val accountId: Long,
        val categoryId: Long?,
        val toAccountId: Long?
    ) : RecordValidation

    data class Error(val message: String) : RecordValidation
}

enum class RecordMode { EXPENSE, INCOME, TRANSFER }

/** combine 在 Default 上算好的类别/账户/快速记账快照 */
private data class RecordCatalog(
    val expenseCategories: List<CategoryEntity>,
    val incomeCategories: List<CategoryEntity>,
    val accounts: List<AccountEntity>,
    val accountIds: Set<Long>,
    val quickRecords: List<QuickRecordOption>
)

data class QuickRecordOption(
    val entity: QuickRecordEntity,
    val category: CategoryEntity?
)

data class RecordFormState(
    val mode: RecordMode = RecordMode.EXPENSE,
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val quickRecords: List<QuickRecordOption> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val toAccountId: Long? = null,
    val feeDisplay: String = "0",
    val description: String = "",
    val occurredAt: Long = System.currentTimeMillis()
) {
    val currentCategories: List<CategoryEntity>
        get() = when (mode) {
            RecordMode.EXPENSE -> expenseCategories
            RecordMode.INCOME -> incomeCategories
            RecordMode.TRANSFER -> emptyList()
        }
}

data class RecordUiState(
    val mode: RecordMode = RecordMode.EXPENSE,
    val amountDisplay: String = "0",
    val feeDisplay: String = "0",
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val quickRecords: List<QuickRecordOption> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val toAccountId: Long? = null,
    val description: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false
) {
    val amountCents: Long
        get() = Money.parseToCents(amountDisplay) ?: 0L

    val feeCents: Long
        get() = RecordViewModel.parseFeeCents(feeDisplay) ?: -1L

    val currentCategories: List<CategoryEntity>
        get() = when (mode) {
            RecordMode.EXPENSE -> expenseCategories
            RecordMode.INCOME -> incomeCategories
            RecordMode.TRANSFER -> emptyList()
        }

    val canSave: Boolean
        get() = computeCanSave()

    fun computeCanSave(): Boolean {
        if (isSaving) return false
        if (amountCents !in 1..Money.MAX_CENTS) return false
        val accountId = selectedAccountId ?: return false
        if (accounts.none { it.id == accountId }) return false
        return when (mode) {
            RecordMode.TRANSFER -> {
                val toId = toAccountId ?: return false
                if (toId == accountId || accounts.none { it.id == toId }) return false
                feeCents in 0L until amountCents
            }
            else -> {
                val categoryId = selectedCategoryId ?: return false
                currentCategories.any { it.id == categoryId }
            }
        }
    }

    fun toFormState(): RecordFormState = RecordFormState(
        mode = mode,
        expenseCategories = expenseCategories,
        incomeCategories = incomeCategories,
        accounts = accounts,
        quickRecords = quickRecords,
        selectedCategoryId = selectedCategoryId,
        selectedAccountId = selectedAccountId,
        toAccountId = toAccountId,
        feeDisplay = feeDisplay,
        description = description,
        occurredAt = occurredAt
    )
}

sealed interface RecordEvent {
    data class SaveSuccess(val wasEditing: Boolean) : RecordEvent
    data class Error(val message: String) : RecordEvent
}
