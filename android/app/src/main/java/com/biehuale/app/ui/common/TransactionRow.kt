package com.biehuale.app.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biehuale.app.data.db.entity.AccountEntity
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.data.db.entity.TransactionEntity
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.util.DateExt.toDateString

/**
 * 流水行：色点 | 类别+说明 / 金额+日期。无 Card。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    account: AccountEntity?,
    toAccount: AccountEntity? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    rowModifier: Modifier = Modifier
) {
    val title = remember(
        transaction.type,
        category?.name,
        category?.isArchived,
        account?.name,
        account?.isArchived,
        toAccount?.name,
        toAccount?.isArchived
    ) {
        when (transaction.type) {
            TransactionType.TRANSFER -> {
                val from = account?.let {
                    if (it.isArchived) "${it.name}（已归档）" else it.name
                } ?: "?"
                val to = toAccount?.let {
                    if (it.isArchived) "${it.name}（已归档）" else it.name
                } ?: "?"
                "$from → $to"
            }
            else -> {
                val name = category?.name ?: "未分类"
                if (category?.isArchived == true) "$name（已归档）" else name
            }
        }
    }
    val subtitle = remember(account?.name, account?.isArchived, transaction.description) {
        val accountLabel = account?.let {
            if (it.isArchived) "${it.name}（已归档）" else it.name
        } ?: "未指定账户"
        buildString {
            append(accountLabel)
            if (!transaction.description.isNullOrBlank()) {
                append(" · ")
                append(transaction.description)
            }
        }
    }
    val fallback = MaterialTheme.colorScheme.primary
    val dotColor = remember(category?.colorHex, fallback) {
        parseHexColor(category?.colorHex) ?: fallback
    }

    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = rowModifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            Column(horizontalAlignment = Alignment.End) {
                AmountText(
                    amountCents = transaction.amount,
                    type = transaction.type,
                    role = AmountRole.Row
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.occurredAt.toDateString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

private fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val raw = hex.trim().removePrefix("#")
    return runCatching {
        when (raw.length) {
            6 -> Color(android.graphics.Color.parseColor("#$raw"))
            8 -> Color(android.graphics.Color.parseColor("#$raw"))
            else -> null
        }
    }.getOrNull()
}
