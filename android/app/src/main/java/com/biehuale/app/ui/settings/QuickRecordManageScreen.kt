package com.biehuale.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.QuickRecordEntity
import com.biehuale.app.ui.common.EmptyState
import com.biehuale.app.ui.common.LedgerConfirm
import com.biehuale.app.ui.common.LedgerField
import com.biehuale.app.ui.common.LoadingState
import com.biehuale.app.ui.common.ManageListRow
import com.biehuale.app.ui.common.SectionPanel
import com.biehuale.app.ui.common.SubScreenScaffold
import com.biehuale.app.ui.icons.BhlIcons
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.ui.theme.MoneyFontFamily
import com.biehuale.app.util.Money.toDisplayString
import com.biehuale.app.util.Money.toMoneyString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRecordManageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuickRecordManageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editTarget by remember { mutableStateOf<QuickEditTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<QuickRecordEntity?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                QuickRecordManageEvent.Saved -> {
                    editTarget = null
                    snackbarHostState.showSnackbar("已保存")
                }
                QuickRecordManageEvent.Deleted -> {
                    deleteTarget = null
                    snackbarHostState.showSnackbar("已删除")
                }
                is QuickRecordManageEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    SubScreenScaffold(
        title = "快速记账",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editTarget = QuickEditTarget.New },
                icon = { Icon(BhlIcons.Add, contentDescription = null) },
                text = { Text("新建") }
            )
        }
    ) { innerPadding ->
        QuickRecordManageBody(
            uiState = uiState,
            modifier = Modifier.padding(innerPadding),
            onCreate = { editTarget = QuickEditTarget.New },
            onEdit = { editTarget = QuickEditTarget.Edit(it) },
            onDelete = { deleteTarget = it }
        )
    }

    editTarget?.let { target ->
        QuickRecordEditDialog(
            target = target,
            expenseCategories = uiState.expenseCategories,
            accounts = uiState.accounts,
            confirmEnabled = !busy,
            onDismiss = { if (!busy) editTarget = null },
            onConfirm = { categoryId, accountId, amount, description ->
                when (target) {
                    QuickEditTarget.New ->
                        viewModel.create(categoryId, accountId, amount, description)
                    is QuickEditTarget.Edit ->
                        viewModel.update(target.entity.id, categoryId, accountId, amount, description)
                }
            }
        )
    }

    deleteTarget?.let { entity ->
        LedgerConfirm(
            title = "删除这条快速记账？",
            message = "删除后不可恢复，不影响已记流水。",
            confirmText = "删除",
            onConfirm = { viewModel.delete(entity.id) },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun QuickRecordManageBody(
    uiState: QuickRecordManageUiState,
    onCreate: () -> Unit,
    onEdit: (QuickRecordEntity) -> Unit,
    onDelete: (QuickRecordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> LoadingState(modifier = modifier)
        uiState.items.isEmpty() -> {
            EmptyState(
                title = "还没有快速记账",
                subtitle = "例如：交通 · 坐地铁 · ¥4.00，记账页一点即记",
                icon = BhlIcons.Add,
                actionText = "新建快速记账",
                onAction = onCreate,
                modifier = modifier
            )
        }
        else -> {
            SectionPanel(
                contentPadding = 0.dp,
                modifier = modifier
                    .fillMaxSize()
                    .padding(vertical = AppSpacing.sm)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.items, key = { it.entity.id }) { item ->
                        QuickRecordRow(
                            item = item,
                            onClick = { onEdit(item.entity) },
                            onDelete = { onDelete(item.entity) }
                        )
                    }
                }
            }
        }
    }
}

private sealed interface QuickEditTarget {
    data object New : QuickEditTarget
    data class Edit(val entity: QuickRecordEntity) : QuickEditTarget
}

@Composable
private fun QuickRecordRow(
    item: QuickRecordListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val category = item.category
    ManageListRow(
        title = category?.name ?: "类别已失效",
        subtitle = buildString {
            append(item.entity.description?.takeIf { it.isNotBlank() } ?: "无说明")
            append(" · ")
            append(item.account?.name ?: "账户已失效")
        },
        iconKey = category?.icon,
        colorHex = category?.colorHex,
        onClick = onClick,
        trailingContent = {
            Text(
                text = item.entity.amount.toDisplayString(),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MoneyFontFamily),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        trailing = {
            IconButton(onClick = onDelete) {
                Icon(
                    BhlIcons.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun QuickRecordEditDialog(
    target: QuickEditTarget,
    expenseCategories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long, accountId: Long, amountYuan: String, description: String) -> Unit
) {
    val existing = (target as? QuickEditTarget.Edit)?.entity
    var categoryId by remember(existing?.id, expenseCategories) {
        mutableStateOf(
            existing?.categoryId?.takeIf { id -> expenseCategories.any { it.id == id } }
                ?: expenseCategories.firstOrNull()?.id
        )
    }
    var accountId by remember(existing?.id, accounts) {
        mutableStateOf(
            existing?.accountId?.takeIf { id -> accounts.any { it.id == id } }
                ?: accounts.firstOrNull()?.id
        )
    }
    var amount by remember(existing?.id) {
        mutableStateOf(existing?.amount?.toMoneyString() ?: "")
    }
    var description by remember(existing?.id) {
        mutableStateOf(existing?.description.orEmpty())
    }
    val categoryValid = categoryId != null && expenseCategories.any { it.id == categoryId }
    val accountValid = accountId != null && accounts.any { it.id == accountId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "新建快速记账" else "编辑快速记账")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                IdDropdownField(
                    label = "类别",
                    value = expenseCategories.firstOrNull { it.id == categoryId }?.name
                        ?: "选择类别",
                    options = expenseCategories.map { it.id to it.name },
                    onSelect = { categoryId = it }
                )
                IdDropdownField(
                    label = "账户",
                    value = accounts.firstOrNull { it.id == accountId }?.name ?: "选择账户",
                    options = accounts.map { it.id to it.name },
                    onSelect = { accountId = it }
                )
                LedgerField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "金额",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                LedgerField(
                    value = description,
                    onValueChange = { if (it.length <= 200) description = it },
                    label = "说明",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cId = categoryId
                    val aId = accountId
                    if (cId == null || aId == null) return@TextButton
                    onConfirm(cId, aId, amount, description)
                },
                enabled = confirmEnabled && categoryValid && accountValid && amount.isNotBlank()
            ) {
                Text(if (confirmEnabled) "保存" else "保存中…")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = confirmEnabled) { Text("取消") }
        }
    )
}

@Composable
private fun IdDropdownField(
    label: String,
    value: String,
    options: List<Pair<Long, String>>,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        LedgerField(
            value = value,
            onValueChange = {},
            label = label,
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailing = { Text("▾") },
            onClick = {
                if (options.isNotEmpty()) expanded = true
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}
