package com.biehuale.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 流水长按操作：编辑 / 删除（LedgerSheet 壳）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionActionSheet(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    LedgerSheet(onDismiss = onDismiss) {
        Text(
            text = "编辑",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onEdit()
                    onDismiss()
                }
                .padding(vertical = 14.dp)
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = "删除",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onDelete()
                    onDismiss()
                }
                .padding(vertical = 14.dp)
        )
    }
}
