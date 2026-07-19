package com.biehuale.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.domain.model.CategoryType
import com.biehuale.app.ui.common.IconColorPresets
import com.biehuale.app.ui.theme.BieHuaLeTheme

/**
 * 类别管理 Screen
 *
 * 详见 docs/DEV_PLAN.md §5 Task 2.2
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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("类别管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "重置默认")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showEditDialog = CategoryEditTarget.New(null) }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建类别")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                    content = { Text(text = "\u52a0\u8f7d\u4e2d\u2026") }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader("支出 (${uiState.expenseCategories.size})")
                    }
                    items(uiState.expenseCategories, key = { it.id }) { category ->
                        CategoryListItem(
                            category = category,
                            onEdit = { showEditDialog = CategoryEditTarget.Edit(category) },
                            onArchive = { viewModel.archive(category.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeader("收入 (${uiState.incomeCategories.size})")
                    }
                    items(uiState.incomeCategories, key = { it.id }) { category ->
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
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("重置默认类别？") },
            text = {
                Text("仅将内置类别恢复为默认名称/图标/颜色，并取消归档。\n\n自建类别与历史账的类别关联不会改动。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    viewModel.resetDefaults()
                }) {
                    Text("确认重置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private sealed interface CategoryEditTarget {
    data class New(val type: CategoryType?) : CategoryEditTarget
    data class Edit(val category: CategoryEntity) : CategoryEditTarget
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun CategoryListItem(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(parseColorOrDefault(category.colorHex, MaterialTheme.colorScheme.primary)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Category,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (category.isBuiltin) {
                        Spacer(modifier = Modifier.size(8.dp))
                        BuiltinBadge()
                    }
                }
            }

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
                            onEdit()
                        }
                    )
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
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text("归档类别？") },
            text = {
                val builtinHint = if (category.isBuiltin) "（内置类别也可归档）" else ""
                Text("归档后「${category.name}」将从列表隐藏，但历史账仍会显示该类别。\n\n$builtinHint")
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
                Text("颜色", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconColorPresets.COLORS.forEach { c ->
                        val selected = c.equals(colorHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseColorOrDefault(c, MaterialTheme.colorScheme.primary))
                                .clickable { colorHex = c }
                                .then(
                                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }
                Text("图标 key：$icon", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconColorPresets.CATEGORY_ICONS.take(6).forEach { key ->
                        FilterChip(
                            selected = icon == key,
                            onClick = { icon = key },
                            label = { Text(key.take(4), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
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

private fun parseColorOrDefault(hex: String?, default: Color): Color = try {
    if (hex.isNullOrBlank()) default else Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    default
}

@Preview(showBackground = true)
@Composable
private fun CategoryManageScreenPreview() {
    BieHuaLeTheme {
        // Preview 需 Hilt
    }
}
