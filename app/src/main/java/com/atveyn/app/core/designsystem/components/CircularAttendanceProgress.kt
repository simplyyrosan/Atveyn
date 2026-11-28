package com.atveyn.app.core.designsystem.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atveyn.app.core.designsystem.theme.AmberGradient
import com.atveyn.app.core.designsystem.theme.BrandGradient
import com.atveyn.app.core.designsystem.theme.GreenGradient
import com.atveyn.app.core.designsystem.theme.RedGradient
import com.atveyn.app.core.designsystem.theme.StatusAmber
import com.atveyn.app.core.designsystem.theme.StatusGreen
import com.atveyn.app.core.designsystem.theme.StatusRed
import java.util.Locale

@Composable
fun CircularAttendanceProgress(
    percentage: Double?,
    targetThreshold: Double,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    strokeWidth: Dp = 13.dp
) {
    val sweepAngle = percentage?.let {
        ((it.coerceIn(0.0, 100.0) / 100.0) * 360f).toFloat()
    } ?: 0f

    val animatedSweep by animateFloatAsState(
        targetValue = sweepAngle,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "sweep_progress"
    )

    val (gradient, statusColor) = when {
        percentage == null -> Pair(
            Brush.sweepGradient(listOf(Color.Gray, Color.DarkGray)),
            Color.Gray
        )
        percentage >= targetThreshold -> Pair(GreenGradient, StatusGreen)
        percentage >= targetThreshold - 5.0 -> Pair(AmberGradient, StatusAmber)
        else -> Pair(RedGradient, StatusRed)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val radius = (size.toPx() - strokePx) / 2

            // Track background - deep rich dark tone
            drawCircle(
                color = Color(0xFF1E202E),
                radius = radius,
                style = Stroke(width = strokePx)
            )

            // Progress arc with gradient and round caps
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
            size <= 120.dp -> 20.sp
            else -> 26.sp
        }

        val targetFontSize = when {
            size <= 80.dp -> 8.5.sp
            size <= 100.dp -> 9.5.sp
            size <= 120.dp -> 10.sp
            else -> 11.sp
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = (strokeWidth + 2.dp))
        ) {
            Text(
                text = percentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A",
                fontWeight = FontWeight.Bold,
                fontSize = percentFontSize,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.14f))
                    .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Target ${String.format(Locale.US, "%.0f%%", targetThreshold)}",
                    fontSize = targetFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    maxLines = 1
                )
            }
        }
    }
}
