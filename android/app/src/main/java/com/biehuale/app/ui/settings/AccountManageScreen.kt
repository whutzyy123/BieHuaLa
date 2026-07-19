package com.biehuale.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.biehuale.app.ui.common.EmptyState
import com.biehuale.app.ui.common.IconColorPickerSection
import com.biehuale.app.ui.common.IconColorPresets
import com.biehuale.app.ui.common.LedgerConfirm
import com.biehuale.app.ui.common.LoadingState
import com.biehuale.app.ui.common.ManageListRow
import com.biehuale.app.ui.common.SectionPanel
import com.biehuale.app.ui.common.SubScreenScaffold
import com.biehuale.app.ui.icons.BhlIcons
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.ui.theme.MoneyFontFamily
import com.biehuale.app.util.Money.toDisplayString
import com.biehuale.app.util.Money.toMoneyString

/**
 * 账户管理 Screen
 *
 * 关键交互：列表（图标 + 名字 + 余额）、FAB 新建、点击编辑、菜单归档。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountManageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditDialog by remember { mutableStateOf<EditTarget?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AccountManageEvent.AccountSaved -> {
                    showEditDialog = null
                    snackbarHostState.showSnackbar("已保存")
                }
                AccountManageEvent.AccountArchived -> {
                    snackbarHostState.showSnackbar("已归档")
                }
                is AccountManageEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    SubScreenScaffold(
        title = "账户管理",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEditDialog = EditTarget.New },
                icon = { Icon(BhlIcons.Add, contentDescription = null) },
                text = { Text("新建账户") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = AppSpacing.sm)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.accounts.isEmpty() -> {
                    EmptyState(
                        title = "还没有账户",
                        subtitle = "点下方按钮新建你的第一个账户",
                        icon = BhlIcons.Wallet,
                        actionText = "新建账户",
                        onAction = { showEditDialog = EditTarget.New }
                    )
                }
                else -> {
                    SectionPanel(
                        contentPadding = 0.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.accounts, key = { it.account.id }) { item ->
                                AccountListItem(
                                    item = item,
                                    onClick = { showEditDialog = EditTarget.Edit(item.account) },
                                    onArchive = { viewModel.archive(item.account.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    showEditDialog?.let { target ->
        AccountEditDialog(
            target = target,
            onDismiss = { showEditDialog = null },
            onConfirm = { name, balance, icon, color ->
                when (target) {
                    is EditTarget.New -> viewModel.create(name, balance, icon, color)
                    is EditTarget.Edit -> viewModel.update(target.account.id, name, balance, icon, color)
                }
            }
        )
    }
}

private sealed interface EditTarget {
    data object New : EditTarget
    data class Edit(val account: AccountEntity) : EditTarget
}

@Composable
private fun AccountListItem(
    item: AccountItem,
    onClick: () -> Unit,
    onArchive: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    ManageListRow(
        title = item.account.name,
        subtitle = "初始余额 ${item.account.initialBalance.toDisplayString()}",
        iconKey = item.account.icon,
        colorHex = item.account.colorHex,
        onClick = onClick,
        onLongClick = { menuExpanded = true },
        trailingContent = {
            Text(
                text = item.balance.toDisplayString(),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = MoneyFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = if (item.balance < 0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        trailing = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(BhlIcons.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(BhlIcons.Edit, null) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("归档", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(BhlIcons.Delete, null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuExpanded = false
                            showArchiveConfirm = true
                        }
                    )
                }
            }
        }
    )

    if (showArchiveConfirm) {
        LedgerConfirm(
            title = "归档账户？",
            message = "归档后「${item.account.name}」将从列表隐藏，但历史账仍会显示该账户名称。",
            confirmText = "归档",
            confirmIsDestructive = true,
            onConfirm = {
                showArchiveConfirm = false
                onArchive()
            },
            onDismiss = { showArchiveConfirm = false }
        )
    }
}

@Composable
private fun AccountEditDialog(
    target: EditTarget,
    onDismiss: () -> Unit,
    onConfirm: (name: String, balance: String, icon: String?, colorHex: String?) -> Unit
) {
    val initialName = when (target) {
        is EditTarget.New -> ""
        is EditTarget.Edit -> target.account.name
    }
    val initialBalance = when (target) {
        is EditTarget.New -> "0"
        is EditTarget.Edit -> target.account.initialBalance.toMoneyString()
    }
    val initialIcon = when (target) {
        is EditTarget.New -> IconColorPresets.ACCOUNT_ICONS.first()
        is EditTarget.Edit -> target.account.icon ?: IconColorPresets.ACCOUNT_ICONS.first()
    }
    val initialColor = when (target) {
        is EditTarget.New -> IconColorPresets.COLORS[6]
        is EditTarget.Edit -> target.account.colorHex ?: IconColorPresets.COLORS[6]
    }

    var name by remember { mutableStateOf(initialName) }
    var balance by remember { mutableStateOf(initialBalance) }
    var icon by remember { mutableStateOf(initialIcon) }
    var colorHex by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (target is EditTarget.New) "新建账户" else "编辑账户")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    label = { Text("账户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("初始余额（元）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                IconColorPickerSection(
                    colorHex = colorHex,
                    onColorChange = { colorHex = it },
                    icon = icon,
                    onIconChange = { icon = it },
                    icons = IconColorPresets.ACCOUNT_ICONS
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, balance, icon, colorHex) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
