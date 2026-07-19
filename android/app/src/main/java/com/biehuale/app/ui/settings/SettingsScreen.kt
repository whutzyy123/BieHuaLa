package com.biehuale.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.preferences.ThemeMode
import com.biehuale.app.ui.theme.BieHuaLeTheme

/**
 * 设置 Tab - Screen
 *
 * Phase 4 完整版：
 *  - 账户管理 / 类别管理（Phase 2）
 *  - 备份导出 / 导入（Phase 4）
 *  - 回收站（Phase 4）
 *  - 主题切换（Phase 4）
 *  - 关于
 */
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

    val snackbarHostState = remember { SnackbarHostState() }
    var showImportPreview by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // SAF 启动器
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            backupViewModel.export(uri)
        }
    }
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            backupViewModel.previewImport(uri)
        }
    }

    LaunchedEffect(Unit) {
        backupViewModel.events.collect { event ->
            when (event) {
                BackupEvent.ExportSuccess -> snackbarHostState.showSnackbar("导出成功")
                is BackupEvent.ImportSuccess -> {
                    val r = event.result
                    val extras = buildList {
                        if (r.transactionsRestored > 0) add("恢复 ${r.transactionsRestored} 笔")
                        if (r.transactionsSkipped > 0) add("跳过 ${r.transactionsSkipped} 笔")
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
        if (backupState.pendingPreview != null) {
            showImportPreview = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = "账户") {
                SettingsItem(
                    text = "管理账户",
                    subtitle = if (accounts.isEmpty()) "暂无账户，点这里新建" else "共 ${accounts.size} 个账户",
                    onClick = onNavigateToAccountManage
                )
            }

            SectionCard(title = "类别") {
                SettingsItem(
                    text = "管理类别",
                    subtitle = "改名 / 归档 / 重置默认",
                    onClick = onNavigateToCategoryManage
                )
            }

            SectionCard(title = "数据") {
                SettingsItem(
                    text = "导出备份",
                    subtitle = if (backupState.isExporting) "导出中…" else "保存到 JSON 文件",
                    onClick = {
                        createDocumentLauncher.launch(backupViewModel.generateFileName())
                    },
                    enabled = !backupState.isExporting && !backupState.isImporting
                )
                HorizontalDivider()
                SettingsItem(
                    text = "导入备份",
                    subtitle = if (backupState.isImporting) "导入中…" else "从 JSON 文件合并",
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    enabled = !backupState.isExporting && !backupState.isImporting
                )
                HorizontalDivider()
                SettingsItem(
                    text = "回收站",
                    subtitle = "30 天内的软删除账",
                    onClick = onNavigateToRecycleBin
                )
            }

            SectionCard(title = "外观") {
                val themeLabel = when (themeMode) {
                    ThemeMode.SYSTEM -> "跟随系统"
                    ThemeMode.LIGHT -> "浅色"
                    ThemeMode.DARK -> "深色"
                }
                SettingsItem(
                    text = "主题",
                    subtitle = "当前：$themeLabel",
                    onClick = { showThemeDialog = true }
                )
            }

            SectionCard(title = "关于") {
                SettingsItem(
                    text = "别花乐",
                    subtitle = "v${com.biehuale.app.BuildConfig.VERSION_NAME} · 个人记账 App"
                )
                HorizontalDivider()
                SettingsItem(text = "隐私", subtitle = "完全本地 · 不联网 · 无追踪")
            }

            if (backupState.isExporting || backupState.isImporting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = if (backupState.isExporting) "导出中…" else "导入中…",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // 导入预览对话框
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

    // 主题选择对话框
    if (showThemeDialog) {
        ThemePickerDialog(
            current = themeMode,
            onSelect = { appearanceViewModel.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
}

// ============================================================
// 子组件：对话框
// ============================================================

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
                Spacer(Modifier.height(8.dp))
                Text("• 账户 ${preview.accountsCount} 个")
                Text("• 类别 ${preview.categoriesCount} 个")
                Text("• 交易 ${preview.transactionsCount} 笔")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "说明：同名账户/类别会复用；内容相同的交易去重，本地已软删的会恢复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isImporting
            ) {
                Text(if (isImporting) "导入中…" else "确认导入")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting
            ) {
                Text("取消")
            }
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
                    description = "Android 12+ 跟系统壁纸取色",
                    icon = Icons.Filled.AutoAwesome,
                    selected = current == ThemeMode.SYSTEM,
                    onClick = { onSelect(ThemeMode.SYSTEM) }
                )
                ThemeOption(
                    label = "浅色",
                    description = "始终浅色主题",
                    icon = Icons.Filled.LightMode,
                    selected = current == ThemeMode.LIGHT,
                    onClick = { onSelect(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    label = "深色",
                    description = "始终深色主题",
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
    description: String,
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

// ============================================================
// 基础组件
// ============================================================

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    text: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val clickableModifier = if (onClick != null && enabled) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Row(
        modifier = clickableModifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 600)
@Composable
private fun SettingsScreenPreview() {
    BieHuaLeTheme {
        // Preview 无 Hilt
    }
}
