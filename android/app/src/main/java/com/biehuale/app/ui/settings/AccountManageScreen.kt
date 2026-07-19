package com.biehuale.app.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.ui.common.IconColorPickerSection
import com.biehuale.app.ui.common.IconColorPresets
import com.biehuale.app.ui.common.parseColorOrDefault
import com.biehuale.app.ui.theme.BieHuaLeTheme
import com.biehuale.app.ui.theme.MoneyFontFamily
import com.biehuale.app.util.Money.toDisplayString
import com.biehuale.app.util.Money.toMoneyString

/**
 * 账户管理 Screen
 *
 * 详见 docs/DEV_PLAN.md §5 Task 2.1
 *
 * 关键交互：
 *  - 列表：图标 + 名字 + 余额
 *  - FAB 新建
 *  - 点击行 → 弹编辑对话框
 *  - "..." 菜单 → 归档
 *  - TopAppBar 返回按钮
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("账户管理", style = com.biehuale.app.ui.theme.ScreenTitleStyle)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEditDialog = EditTarget.New },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("新建账户") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加载中…")
                    }
                }
                uiState.accounts.isEmpty() -> {
                    com.biehuale.app.ui.common.EmptyState(
                        title = "还没有账户",
                        subtitle = "点下方按钮新建你的第一个账户",
                        icon = Icons.Filled.AccountBalanceWallet,
                        actionText = "新建账户",
                        onAction = { showEditDialog = EditTarget.New }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(com.biehuale.app.ui.theme.AppSpacing.md)
                    ) {
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(parseColorOrDefault(item.account.colorHex, MaterialTheme.colorScheme.primary))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.account.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "初始余额 ${item.account.initialBalance.toDisplayString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.balance.toDisplayString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = MoneyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            // 菜单按钮
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("归档", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            showArchiveConfirm = true
                        }
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("归档账户？") },
            text = {
                Text("归档后「${item.account.name}」将从列表隐藏，但历史账仍会显示该账户名称。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showArchiveConfirm = false
                    onArchive()
                }) {
                    Text("归档", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) {
                    Text("取消")
                }
            }
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

@Preview(showBackground = true)
@Composable
private fun AccountManageScreenPreview() {
    BieHuaLeTheme {
        // Preview 无 Hilt
    }
}
