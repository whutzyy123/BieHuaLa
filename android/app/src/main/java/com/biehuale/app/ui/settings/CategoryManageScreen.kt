package com.biehuale.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.ui.common.IconColorPickerSection
import com.biehuale.app.ui.common.IconColorPresets
import com.biehuale.app.ui.common.LedgerConfirm
import com.biehuale.app.ui.common.LoadingState
import com.biehuale.app.ui.common.ManageListRow
import com.biehuale.app.ui.common.SectionPanel
import com.biehuale.app.ui.common.SubScreenScaffold
import com.biehuale.app.ui.icons.BhlIcons
import com.biehuale.app.ui.theme.AppSpacing

/**
 * 类别管理 Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryManageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditDialog by remember { mutableStateOf<CategoryEditTarget?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CategoryManageEvent.Saved -> {
                    showEditDialog = null
                    snackbarHostState.showSnackbar("已保存")
                }
                CategoryManageEvent.Archived -> snackbarHostState.showSnackbar("已归档")
                CategoryManageEvent.DefaultsReset -> snackbarHostState.showSnackbar("已重置默认类别")
                is CategoryManageEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    SubScreenScaffold(
        title = "类别管理",
        onBack = onBack,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = { showResetConfirm = true }) {
                Icon(BhlIcons.Restart, contentDescription = "重置默认")
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEditDialog = CategoryEditTarget.New(null) },
                icon = { Icon(BhlIcons.Add, contentDescription = null) },
                text = { Text("新建类别") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                LoadingState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = AppSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    item(key = "expense") {
                        SectionPanel(
                            title = "支出 (${uiState.expenseCategories.size})",
                            contentPadding = 0.dp
                        ) {
                            uiState.expenseCategories.forEach { category ->
                                CategoryListItem(
                                    category = category,
                                    onEdit = { showEditDialog = CategoryEditTarget.Edit(category) },
                                    onArchive = { viewModel.archive(category.id) }
                                )
                            }
                        }
                    }
                    item(key = "income") {
                        SectionPanel(
                            title = "收入 (${uiState.incomeCategories.size})",
                            contentPadding = 0.dp
                        ) {
                            uiState.incomeCategories.forEach { category ->
                                CategoryListItem(
                                    category = category,
                                    onEdit = { showEditDialog = CategoryEditTarget.Edit(category) },
                                    onArchive = { viewModel.archive(category.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    showEditDialog?.let { target ->
        CategoryEditDialog(
            target = target,
            onDismiss = { showEditDialog = null },
            onConfirm = { name, type, icon, color ->
                when (target) {
                    is CategoryEditTarget.New -> viewModel.create(name, type, icon, color)
                    is CategoryEditTarget.Edit -> viewModel.update(target.category.id, name, icon, color)
                }
            }
        )
    }

    if (showResetConfirm) {
        LedgerConfirm(
            title = "重置默认类别？",
            message = "仅将内置类别恢复为默认名称/图标/颜色，并取消归档。\n\n自建类别与历史账的类别关联不会改动。",
            confirmText = "确认重置",
            onConfirm = {
                showResetConfirm = false
                viewModel.resetDefaults()
            },
            onDismiss = { showResetConfirm = false }
        )
    }
}

private sealed interface CategoryEditTarget {
    data class New(val type: CategoryType?) : CategoryEditTarget
    data class Edit(val category: CategoryEntity) : CategoryEditTarget
}

@Composable
private fun CategoryListItem(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    ManageListRow(
        title = category.name,
        iconKey = category.icon,
        colorHex = category.colorHex,
        onClick = onEdit,
        titleAccessory = if (category.isBuiltin) {
            { BuiltinBadge() }
        } else {
            null
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
        val builtinHint = if (category.isBuiltin) "（内置类别也可归档）" else ""
        LedgerConfirm(
            title = "归档类别？",
            message = "归档后「${category.name}」将从列表隐藏，但历史账仍会显示该类别。\n\n$builtinHint",
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
private fun BuiltinBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "内置",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun CategoryEditDialog(
    target: CategoryEditTarget,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: CategoryType, icon: String?, colorHex: String?) -> Unit
) {
    val initialName = when (target) {
        is CategoryEditTarget.New -> ""
        is CategoryEditTarget.Edit -> target.category.name
    }
    val initialType = when (target) {
        is CategoryEditTarget.New -> target.type ?: CategoryType.EXPENSE
        is CategoryEditTarget.Edit -> target.category.type
    }
    val initialIcon = when (target) {
        is CategoryEditTarget.New -> IconColorPresets.CATEGORY_ICONS.first()
        is CategoryEditTarget.Edit -> target.category.icon ?: IconColorPresets.CATEGORY_ICONS.first()
    }
    val initialColor = when (target) {
        is CategoryEditTarget.New -> IconColorPresets.COLORS.first()
        is CategoryEditTarget.Edit -> target.category.colorHex ?: IconColorPresets.COLORS.first()
    }

    var name by remember { mutableStateOf(initialName) }
    var type by remember { mutableStateOf(initialType) }
    var icon by remember { mutableStateOf(initialIcon) }
    var colorHex by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (target is CategoryEditTarget.New) "新建类别" else "编辑类别")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 10) name = it },
                    label = { Text("类别名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (target is CategoryEditTarget.New) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == CategoryType.EXPENSE,
                            onClick = { type = CategoryType.EXPENSE },
                            label = { Text("支出") }
                        )
                        FilterChip(
                            selected = type == CategoryType.INCOME,
                            onClick = { type = CategoryType.INCOME },
                            label = { Text("收入") }
                        )
                    }
                }
                IconColorPickerSection(
                    colorHex = colorHex,
                    onColorChange = { colorHex = it },
                    icon = icon,
                    onIconChange = { icon = it },
                    icons = IconColorPresets.CATEGORY_ICONS
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, type, icon, colorHex) }) {
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
