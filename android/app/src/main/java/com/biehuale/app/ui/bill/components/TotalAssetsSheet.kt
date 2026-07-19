package com.biehuale.app.ui.bill.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.biehuale.app.ui.bill.AccountAssetLine
import com.biehuale.app.ui.common.LedgerSheet
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.ui.theme.MoneyRowStyle
import com.biehuale.app.util.Money.toDisplayString

/**
 * 总资产分户明细（只读）；余额为时点净值。
 */
@Composable
fun TotalAssetsSheet(
    totalAssetsCents: Long,
    breakdown: List<AccountAssetLine>,
    onDismiss: () -> Unit
) {
    LedgerSheet(onDismiss = onDismiss) {
        Text(
            text = "总资产",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(AppSpacing.xs))
        Text(
            text = totalAssetsCents.toDisplayString(),
            style = MoneyRowStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "各账户实时余额合计，与所选月份无关",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = AppSpacing.xs)
        )
        Spacer(modifier = Modifier.height(AppSpacing.md))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        if (breakdown.isEmpty()) {
            Text(
                text = "暂无账户",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = AppSpacing.lg)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                breakdown.forEach { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = line.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = line.balanceCents.toDisplayString(),
                            style = MoneyRowStyle.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize
                            ),
                            color = if (line.balanceCents < 0L) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}
