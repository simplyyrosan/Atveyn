package com.atveyn.app.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atveyn.app.core.designsystem.theme.Cyan500
import com.atveyn.app.core.designsystem.theme.Indigo500
import com.atveyn.app.core.designsystem.theme.StatusAmber
import com.atveyn.app.core.designsystem.theme.StatusGreen
import com.atveyn.app.core.designsystem.theme.StatusRed
import java.util.Locale

@Composable
fun CircularAttendanceProgress(
    percentage: Double?,
    targetThreshold: Double,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    strokeWidth: Dp = 12.dp
) {
    val sweepAngle = percentage?.let {
        ((it.coerceIn(0.0, 100.0) / 100.0) * 360f).toFloat()
    } ?: 0f

    val animatedSweep by animateFloatAsState(
        targetValue = sweepAngle,
        animationSpec = tween(durationMillis = 900),
        label = "sweep_progress"
    )

    val progressColor = when {
        percentage == null -> Color.Gray
        percentage >= targetThreshold -> StatusGreen
        percentage >= targetThreshold - 5.0 -> StatusAmber
        else -> StatusRed
    }

    val gradient = Brush.sweepGradient(
        listOf(
            progressColor.copy(alpha = 0.8f),
            progressColor
        )
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val radius = (size.toPx() - strokePx) / 2

            // Track background
            drawCircle(
                color = Color.Gray.copy(alpha = 0.15f),
                radius = radius,
                style = Stroke(width = strokePx)
            )

            // Progress arc
            if (animatedSweep > 0f) {
                drawArc(
                    brush = gradient,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        val percentFontSize = when {
            size <= 80.dp -> 13.sp
            size <= 100.dp -> 16.sp
            size <= 120.dp -> 19.sp
            else -> 24.sp
        }

        val targetFontSize = when {
            size <= 80.dp -> 8.sp
            size <= 100.dp -> 9.sp
            size <= 120.dp -> 10.sp
            else -> 11.sp
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = (strokeWidth + 2.dp))
        ) {
            Text(
                text = percentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = percentFontSize
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Text(
                text = "Target ${String.format(Locale.US, "%.0f%%", targetThreshold)}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = targetFontSize
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
