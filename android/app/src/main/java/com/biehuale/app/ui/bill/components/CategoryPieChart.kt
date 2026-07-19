package com.biehuale.app.ui.bill.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biehuale.app.data.db.dao.CategoryTotal
import com.biehuale.app.data.db.entity.CategoryEntity
import com.biehuale.app.ui.theme.AppSpacing
import com.biehuale.app.ui.theme.MoneyFontFamily
import com.biehuale.app.util.Money.toDisplayString
import kotlin.math.atan2

/** 分类占比：缩小视觉权重，无 Card。 */
@Composable
fun CategoryPieChart(
    data: List<CategoryTotal>,
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategoryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.totalCents }
    if (total == 0L) return

    val byId = categories.associateBy { it.id }
    val entries = data.map { ct ->
        val category = byId[ct.categoryId]
        val name = when {
            category == null -> "未分类"
            category.isArchived -> "${category.name}（已归档）"
            else -> category.name
        }
        PieEntry(
            categoryId = ct.categoryId,
            name = name,
            total = ct.totalCents,
            color = parseColor(category?.colorHex) ?: Color(0xFF9E9E9E)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
    ) {
        Text(
            text = "花在哪",
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppSpacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            BoxWithConstraints(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                val sizePx = with(LocalDensity.current) { 96.dp.toPx() }
                val strokeWidth = sizePx * 0.14f
                val arcSize = sizePx - strokeWidth
                val topLeft = Offset((sizePx - arcSize) / 2, (sizePx - arcSize) / 2)
                val arcSize2 = Size(arcSize, arcSize)

                Canvas(
                    modifier = Modifier
                        .size(96.dp)
                        .pointerInput(entries, total) {
                            detectTapGestures { offset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                angle = (angle + 90f + 360f) % 360f
                                var start = 0f
                                for (entry in entries) {
                                    val sweep = (entry.total.toFloat() / total.toFloat()) * 360f
                                    if (angle >= start && angle < start + sweep) {
                                        onCategoryClick(entry.categoryId)
                                        break
                                    }
                                    start += sweep
                                }
                            }
                        }
                ) {
                    var startAngle = -90f
                    entries.forEach { entry ->
                        val sweep = (entry.total.toFloat() / total.toFloat()) * 360f
                        val isSelected = selectedCategoryId == entry.categoryId
                        drawArc(
                            color = if (isSelected) entry.color else entry.color.copy(alpha = 0.4f),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize2,
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweep
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .height(96.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                entries.forEach { entry ->
                    LegendItem(
                        entry = entry,
                        total = total,
                        isSelected = selectedCategoryId == entry.categoryId,
                        onClick = { onCategoryClick(entry.categoryId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    entry: PieEntry,
    total: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pct = (entry.total.toFloat() / total.toFloat()) * 100f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isSelected) entry.color else entry.color.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
            Text(
                text = "${"%.1f".format(pct)}% \u00b7 ${entry.total.toDisplayString()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = MoneyFontFamily
            )
        }
    }
}

private data class PieEntry(
    val categoryId: Long,
    val name: String,
    val total: Long,
    val color: Color
)

private fun parseColor(hex: String?): Color? = try {
    if (hex.isNullOrBlank()) null else Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    null
}
