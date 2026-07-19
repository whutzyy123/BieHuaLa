package com.biehuale.app.ui.record.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.util.DateExt.toDateTimeString
import com.biehuale.app.util.Money
import com.biehuale.app.util.Money.toDisplayString

@Composable
fun TransferSection(
    amountDisplay: String,
    fromAccountId: Long?,
    toAccountId: Long?,
    accounts: List<AccountEntity>,
    onFromAccountSelect: (Long) -> Unit,
    onToAccountSelect: (Long) -> Unit
) {
    val fromName = accounts.firstOrNull { it.id == fromAccountId }?.name ?: "转出账户"
    val toName = accounts.firstOrNull { it.id == toAccountId }?.name ?: "转入账户"
    val amountText = run {
        val cents = Money.parseToCents(
            if (amountDisplay.endsWith(".")) amountDisplay + "0" else amountDisplay
        ) ?: 0L
        if (cents > 0L) cents.toDisplayString() else "¥0.00"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "$amountText → $fromName -$amountText → $toName +$amountText",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "转账不影响本月总支出",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccountDropdown(
                label = "转出",
                accounts = accounts.filter { it.id != toAccountId },
                selectedId = fromAccountId,
                onSelect = onFromAccountSelect,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AccountDropdown(
                label = "转入",
                accounts = accounts.filter { it.id != fromAccountId },
                selectedId = toAccountId,
                onSelect = onToAccountSelect,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CategoryGrid(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        Box(
            modifier = modifier
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无类别，请到「设置」添加",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.heightIn(max = 200.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryChip(
                category = category,
                selected = category.id == selectedId,
                onClick = { onSelect(category.id) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: CategoryEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = containerColor,
        border = if (selected) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun AccountAndTimeRow(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    occurredAt: Long,
    onAccountSelect: (Long) -> Unit,
    onTimeChange: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AccountDropdown(
            label = "账户",
            accounts = accounts,
            selectedId = selectedAccountId,
            onSelect = onAccountSelect,
            modifier = Modifier.weight(1f)
        )
        TimeField(
            occurredAt = occurredAt,
            onTimeChange = onTimeChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AccountDropdown(
    label: String,
    accounts: List<AccountEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    val displayName = selected?.name ?: if (accounts.isEmpty()) "无账户" else "选择账户"

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = { expanded = true }) {
                    Text("▾")
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (accounts.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("请到「设置」新建账户") },
                    onClick = { expanded = false }
                )
            } else {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = { Text(account.name) },
                        onClick = {
                            onSelect(account.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TimeField(
    occurredAt: Long,
    onTimeChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = occurredAt.toDateTimeString(),
        onValueChange = {},
        readOnly = true,
        label = { Text("时间") },
        modifier = modifier,
        trailingIcon = {
            TextButton(onClick = { onTimeChange(System.currentTimeMillis()) }) {
                Text("现在")
            }
        }
    )
}

@Composable
fun DescriptionField(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("说明（可选）") },
        placeholder = { Text("写点什么吧…") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine = true,
        maxLines = 1,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )
}
