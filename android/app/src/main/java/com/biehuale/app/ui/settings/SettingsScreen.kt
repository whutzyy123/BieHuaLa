package com.biehuale.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.preferences.ThemeMode
import com.biehuale.app.ui.common.SettingsGroup
import com.biehuale.app.ui.common.SettingsRow
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.ui.theme.BrandSerif
import com.biehuale.app.ui.theme.ScreenTitleStyle

@Composable
fun SettingsScreen(
    onNavigateToAccountManage: () -> Unit = {},
    onNavigateToCategoryManage: () -> Unit = {},
    onNavigateToRecycleBin: () -> Unit = {},
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val accounts by settingsViewModel.accounts.collectAsStateWithLifecycle()
    val backupState by backupViewModel.state.collectAsStateWithLifecycle()
    val themeMode by appearanceViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by appearanceViewModel.dynamicColor.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showImportPreview by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) backupViewModel.export(uri)
    }
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) backupViewModel.previewImport(uri)
    }

    LaunchedEffect(Unit) {
        backupViewModel.events.collect { event ->
            when (event) {
                BackupEvent.ExportSuccess -> snackbarHostState.showSnackbar("导出成功")
                is BackupEvent.ImportSuccess -> {
                    val r = event.result
                    val extras = buildList {
                        if (r.transactionsRestored > 0) add("恢复 ${r.transactionsRestored} 笔")
                        if (r.transactionsSoftDeleted > 0) add("同步软删 ${r.transactionsSoftDeleted} 笔")
                        if (r.transactionsSkipped > 0) add("跳过 ${r.transactionsSkipped} 笔")
                        if (r.categoriesSkipped > 0) add("跳过 ${r.categoriesSkipped} 个类别")
                    }.joinToString("，").let { if (it.isEmpty()) "" else "，$it" }
                    snackbarHostState.showSnackbar(
                        "已导入 ${r.transactionsInserted} 笔账，${r.accountsInserted} 个账户，${r.categoriesInserted} 个类别$extras"
                    )
                }
                is BackupEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    LaunchedEffect(backupState.pendingPreview) {
        if (backupState.pendingPreview != null) showImportPreview = true
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = AppSpacing.md, bottom = AppSpacing.xl)
        ) {
            SettingsGroup(title = "账户") {
                SettingsRow(
                    title = "管理账户",
                    subtitle = if (accounts.isEmpty()) "暂无账户，点这里新建" else "共 ${accounts.size} 个账户",
                    onClick = onNavigateToAccountManage,
                    showDivider = false
                )
            }

            SettingsGroup(title = "类别") {
                SettingsRow(
                    title = "管理类别",
                    subtitle = "改名 / 归档 / 重置默认",
                    onClick = onNavigateToCategoryManage,
                    showDivider = false
                )
            }

            SettingsGroup(title = "数据") {
                SettingsRow(
                    title = "导出备份",
                    subtitle = if (backupState.isExporting) "导出中…" else "保存到 JSON 文件",
                    onClick = if (!backupState.isBusy) {
                        { createDocumentLauncher.launch(backupViewModel.generateFileName()) }
                    } else null
                )
                SettingsRow(
                    title = "导入备份",
                    subtitle = when {
                        backupState.isPreviewing -> "读取中…"
                        backupState.isImporting -> "导入中…"
                        else -> "从 JSON 文件合并"
                    },
                    onClick = if (!backupState.isBusy) {
                        { openDocumentLauncher.launch(arrayOf("application/json", "*/*")) }
                    } else null
                )
                SettingsRow(
                    title = "回收站",
                    subtitle = "30 天内的软删除账",
                    onClick = onNavigateToRecycleBin,
                    showDivider = false
                )
            }

            SettingsGroup(title = "外观") {
                val themeLabel = when (themeMode) {
                    ThemeMode.SYSTEM -> "跟随系统"
                    ThemeMode.LIGHT -> "浅色"
                    ThemeMode.DARK -> "深色"
                }
                SettingsRow(
                    title = "主题",
                    subtitle = "当前：$themeLabel",
                    onClick = { showThemeDialog = true }
                )
                SettingsRow(
                    title = "跟随壁纸取色",
                    subtitle = "Android 12+ Material You（默认关）",
                    trailing = {
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = appearanceViewModel::setDynamicColor
                        )
                    },
                    showChevron = false,
                    showDivider = false
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.lg)
            ) {
                Text(
                    text = "别花乐",
                    style = ScreenTitleStyle,
                    fontFamily = BrandSerif,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = "完全本地 · 不联网",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppSpacing.xs))
                Text(
                    text = "v${com.biehuale.app.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (backupState.isBusy) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = when {
                            backupState.isExporting -> "导出中…"
                            backupState.isPreviewing -> "读取中…"
                            else -> "导入中…"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    backupState.pendingPreview?.let { preview ->
        if (showImportPreview) {
            ImportPreviewDialog(
                preview = preview,
                isImporting = backupState.isImporting,
                onConfirm = { backupViewModel.confirmImport(preview) },
                onDismiss = {
                    showImportPreview = false
                    backupViewModel.cancelImport()
                }
            )
        }
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            current = themeMode,
            onSelect = { appearanceViewModel.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun ImportPreviewDialog(
    preview: com.biehuale.app.data.backup.ImportPreview,
    isImporting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title = { Text("确认导入") },
        text = {
            Column {
                Text("将合并以下数据：")
                Spacer(modifier = Modifier.height(8.dp))
                Text("• 账户 ${preview.accountsCount} 个")
                Text("• 类别 ${preview.categoriesCount} 个")
                Text("• 交易 ${preview.transactionsCount} 笔")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "说明：同名账户/类别会复用；期初余额仅在本地为 0 时采用备份。交易按内容去重：本地软删+备份未删则恢复；备份已软删则同步软删。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isImporting) {
                Text(if (isImporting) "导入中…" else "确认导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isImporting) { Text("取消") }
        }
    )
}

@Composable
private fun ThemePickerDialog(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(
                    label = "跟随系统",
                    icon = Icons.Filled.AutoAwesome,
                    selected = current == ThemeMode.SYSTEM,
                    onClick = { onSelect(ThemeMode.SYSTEM) }
                )
                ThemeOption(
                    label = "浅色",
                    icon = Icons.Filled.LightMode,
                    selected = current == ThemeMode.LIGHT,
                    onClick = { onSelect(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    label = "深色",
                    icon = Icons.Filled.DarkMode,
                    selected = current == ThemeMode.DARK,
                    onClick = { onSelect(ThemeMode.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
private fun ThemeOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) }
    )
}
