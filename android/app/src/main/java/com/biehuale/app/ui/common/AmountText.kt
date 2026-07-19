package com.biehuale.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biehuale.app.domain.model.TransactionType
import com.biehuale.app.ui.theme.AppMotion
import com.biehuale.app.ui.theme.AppSemanticColors
import com.biehuale.app.ui.theme.MoneyHeroStyle
import com.biehuale.app.ui.theme.MoneyRowStyle
import com.biehuale.app.util.Money.toDisplayString
import kotlinx.coroutines.delay

enum class AmountRole { Hero, Row, Detail }

/**
 * 统一金额展示：Mono tabular + 语义色 + 可选入场动效（fade + 上移）。
 */
@Composable
fun AmountText(
    amountCents: Long,
    type: TransactionType? = null,
    role: AmountRole = AmountRole.Row,
    color: Color? = null,
    showSign: Boolean = true,
    animateEntrance: Boolean = false,
    modifier: Modifier = Modifier
) {
    val semantic = when (type) {
        TransactionType.INCOME -> AppSemanticColors.income
        TransactionType.EXPENSE -> AppSemanticColors.expense
        TransactionType.TRANSFER -> AppSemanticColors.transfer
        null -> color
    }
    val resolvedColor = color ?: semantic ?: AppSemanticColors.expense
    val sign = when {
        !showSign || type == null || type == TransactionType.TRANSFER -> ""
        type == TransactionType.INCOME -> "+"
        else -> "-"
    }
    val style: TextStyle = when (role) {
        AmountRole.Hero -> MoneyHeroStyle
        AmountRole.Row -> MoneyRowStyle
        AmountRole.Detail -> MaterialTheme.typography.displayMedium
    }
    val text = "$sign${amountCents.toDisplayString()}"
    val weight = when (role) {
        AmountRole.Row -> FontWeight.Medium
        else -> FontWeight.SemiBold
    }

    // 入场只播一次，避免账单切月/金额刷新时反复闪动
    var hasPlayedEntrance by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(!animateEntrance) }
    LaunchedEffect(animateEntrance) {
        if (animateEntrance && !hasPlayedEntrance) {
            visible = false
            delay(16)
            visible = true
            hasPlayedEntrance = true
        } else if (!animateEntrance) {
            visible = true
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = AppMotion.amount(),
        label = "amountAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else AppMotion.AmountEnterOffset.value,
        animationSpec = AppMotion.amount(),
        label = "amountOffset"
    )

    Text(
        text = text,
        style = style,
        color = resolvedColor,
        fontWeight = weight,
        modifier = modifier
            .alpha(alpha)
            .offset(y = offsetY.dp)
    )
}
