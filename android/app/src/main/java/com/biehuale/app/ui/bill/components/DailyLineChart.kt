package com.biehuale.app.ui.bill.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biehuale.app.data.db.dao.DailyTotal
import com.biehuale.app.ui.bill.TrendGranularity
import com.biehuale.app.ui.theme.AppSemanticColors
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.util.Money.toMoneyString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 自绘支出趋势折线图（按日 / 周 / 月）— 无 Card，粒度用文字 Segment。
 */
@Composable
fun DailyLineChart(
    data: List<DailyTotal>,
    granularity: TrendGranularity,
    onGranularityChange: (TrendGranularity) -> Unit,
    showMonthGranularity: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOf { it.totalCents }.coerceAtLeast(100L)
    val displayMax = (maxValue * 1.2f).toLong()
    val lineColor = AppSemanticColors.expense

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm + AppSpacing.xs)) {
                GranularitySegment("日", TrendGranularity.DAY, granularity, onGranularityChange)
                GranularitySegment("周", TrendGranularity.WEEK, granularity, onGranularityChange)
                if (showMonthGranularity) {
                    GranularitySegment("月", TrendGranularity.MONTH, granularity, onGranularityChange)
                }
            }
        }
        Spacer(modifier = Modifier.height(AppSpacing.sm))

            val density = LocalDensity.current
            val labelTextSizePx = with(density) { 10.sp.toPx() }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val padding = 16.dp.toPx()
                val chartWidth = size.width - padding * 2
                val chartHeight = size.height - padding * 2
                val originX = padding
                val originY = padding + chartHeight

                val gridColor = lineColor.copy(alpha = 0.1f)
                val dashPath = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                for (i in 1..3) {
                    val y = originY - chartHeight * i / 4
                    drawLine(
                        color = gridColor,
                        start = Offset(originX, y),
                        end = Offset(originX + chartWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashPath
                    )
                }

                val xStep = chartWidth / (data.size - 1).coerceAtLeast(1)
                if (data.size >= 2) {
                    val path = Path()
                    data.forEachIndexed { i, point ->
                        val x = originX + i * xStep
                        val ratio = point.totalCents.toFloat() / displayMax.toFloat()
                        val y = originY - ratio * chartHeight
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                data.forEachIndexed { i, point ->
                    val x = originX + i * xStep
                    val ratio = point.totalCents.toFloat() / displayMax.toFloat()
                    val y = originY - ratio * chartHeight
                    drawCircle(
                        color = lineColor,
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                val maxIndex = data.indices.maxBy { data[it].totalCents }
                if (data[maxIndex].totalCents > 0) {
                    val x = originX + maxIndex * xStep
                    val ratio = data[maxIndex].totalCents.toFloat() / displayMax.toFloat()
                    val y = originY - ratio * chartHeight
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = lineColor.toArgb()
                            textSize = labelTextSizePx
                            isAntiAlias = true
                        }
                        drawText(
                            data[maxIndex].totalCents.toMoneyString(),
                            x + 6.dp.toPx(),
                            y - 4.dp.toPx(),
                            paint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatBucket(data.first().dayStart, granularity),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (data.size > 5) {
                    Text(
                        text = formatBucket(data[data.size / 2].dayStart, granularity),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatBucket(data.last().dayStart, granularity),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
    }
}

@Composable
private fun GranularitySegment(
    label: String,
    value: TrendGranularity,
    selected: TrendGranularity,
    onSelect: (TrendGranularity) -> Unit
) {
    val isSelected = selected == value
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.clickable { onSelect(value) }
    )
}

private fun formatBucket(millis: Long, granularity: TrendGranularity): String {
    val pattern = when (granularity) {
        TrendGranularity.DAY -> "MM-dd"
        TrendGranularity.WEEK -> "MM-dd"
        TrendGranularity.MONTH -> "yyyy-MM"
    }
    return SimpleDateFormat(pattern, Locale.CHINA).format(Date(millis))
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)
