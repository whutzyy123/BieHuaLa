package com.biehuale.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.ui.icons.BhlIcons
import com.biehuale.app.ui.theme.AppSpacing

/**
 * 筛选 BottomSheet：勾选即时写回；底部「完成」关 Sheet、「清除」清空。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    selectedTypes: Set<TransactionType>,
    selectedAccountIds: Set<Long>,
    selectedCategoryIds: Set<Long>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onApply: (types: Set<TransactionType>, accountIds: Set<Long>, categoryIds: Set<Long>) -> Unit,
    onClearApplied: () -> Unit
) {
    LedgerSheet(
        onDismiss = onDismiss,
        sheetState = sheetState
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "筛选",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onClearApplied) {
                    Text("清除")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FilterSection(title = "类型") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip(
                        label = "支出",
                        icon = BhlIcons.Expense,
                        selected = TransactionType.EXPENSE in selectedTypes,
                        onClick = {
                            onApply(
                                selectedTypes.toggle(TransactionType.EXPENSE),
                                selectedAccountIds,
                                selectedCategoryIds
                            )
                        }
                    )
                    TypeChip(
                        label = "收入",
                        icon = BhlIcons.Income,
                        selected = TransactionType.INCOME in selectedTypes,
                        onClick = {
                            onApply(
                                selectedTypes.toggle(TransactionType.INCOME),
                                selectedAccountIds,
                                selectedCategoryIds
                            )
                        }
                    )
                    TypeChip(
                        label = "转账",
                        icon = BhlIcons.Transfer,
                        selected = TransactionType.TRANSFER in selectedTypes,
                        onClick = {
                            onApply(
                                selectedTypes.toggle(TransactionType.TRANSFER),
                                selectedAccountIds,
                                selectedCategoryIds
                            )
                        }
                    )
                }
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            FilterSection(title = "账户") {
                if (accounts.isEmpty()) {
                    Text(
                        text = "暂无账户",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(accounts, key = { it.id }) { account ->
                            CheckboxRow(
                                label = account.name,
                                icon = BhlIcons.Wallet,
                                checked = account.id in selectedAccountIds,
                                onToggle = {
                                    onApply(
                                        selectedTypes,
                                        selectedAccountIds.toggle(account.id),
                                        selectedCategoryIds
                                    )
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            FilterSection(title = "类别") {
                if (categories.isEmpty()) {
                    Text(
                        text = "暂无类别",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(categories, key = { it.id }) { category ->
                            CheckboxRow(
                                label = category.name,
                                icon = BhlIcons.Category,
                                checked = category.id in selectedCategoryIds,
                                onToggle = {
                                    onApply(
                                        selectedTypes,
                                        selectedAccountIds,
                                        selectedCategoryIds.toggle(category.id)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("完成")
            }
            Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> {
    val next = toMutableSet()
    if (!next.add(item)) next.remove(item)
    return next
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = AppSpacing.sm)) {
        ListSectionHeader(
            title = title,
            color = MaterialTheme.colorScheme.primary,
            horizontalPadding = 0.dp,
            verticalPadding = AppSpacing.xs
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

@Composable
private fun CheckboxRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
