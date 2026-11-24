package com.atveyn.app.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atveyn.app.core.designsystem.theme.StatusGreen
import com.atveyn.app.core.designsystem.theme.StatusRed
import java.util.Locale
import kotlin.math.max

@Composable
fun TrendChart(
    weeklyTrends: List<WeeklyTrendItem>,
    modifier: Modifier = Modifier
) {
    val maxCount = max(5, weeklyTrends.maxOfOrNull { it.presentCount + it.absentCount } ?: 5)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Weekly Activity Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem("Present", StatusGreen)
                LegendItem("Absent", StatusRed)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (weeklyTrends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No attendance data logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Bars Canvas area (isolated from labels)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val numItems = weeklyTrends.size

                val slotWidth = canvasWidth / numItems
                val barWidth = 14.dp.toPx()
                val gap = 4.dp.toPx()
                val topPadding = 12.dp.toPx()
                val usableHeight = canvasHeight - topPadding

                // Subtle guide lines at 50% and 100%
                val y50 = canvasHeight - (usableHeight * 0.5f)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.12f),
                    start = Offset(0f, y50),
                    end = Offset(canvasWidth, y50),
                    strokeWidth = 1.dp.toPx()
                )

                // Baseline at bottom of canvas
                drawLine(
                    color = Color.Gray.copy(alpha = 0.35f),
                    start = Offset(0f, canvasHeight - 1.dp.toPx()),
                    end = Offset(canvasWidth, canvasHeight - 1.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )

                for (i in 0 until numItems) {
                    val item = weeklyTrends[i]
                    val centerX = slotWidth * i + (slotWidth / 2)

                    val presentHeight = if (maxCount > 0) (item.presentCount.toFloat() / maxCount) * usableHeight else 0f
                    val absentHeight = if (maxCount > 0) (item.absentCount.toFloat() / maxCount) * usableHeight else 0f

                    val pLeft = centerX - barWidth - (gap / 2)
                    val pTop = canvasHeight - presentHeight
                    if (presentHeight > 0) {
                        drawRoundRect(
                            color = StatusGreen,
                            topLeft = Offset(pLeft, pTop),
                            size = Size(barWidth, presentHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    val aLeft = centerX + (gap / 2)
                    val aTop = canvasHeight - absentHeight
                    if (absentHeight > 0) {
                        drawRoundRect(
                            color = StatusRed,
                            topLeft = Offset(aLeft, aTop),
                            size = Size(barWidth, absentHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text labels row strictly BELOW the canvas baseline
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                for (item in weeklyTrends) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.percentage?.let { String.format(Locale.US, "%.0f%%", it) } ?: "—",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = item.percentage?.let { if (it >= 75.0) StatusGreen else StatusRed }
                                ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.weekLabel,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
