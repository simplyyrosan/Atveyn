package com.atveyn.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atveyn.app.core.designsystem.theme.StatusAmber
import com.atveyn.app.core.designsystem.theme.StatusAmberBg
import com.atveyn.app.core.designsystem.theme.StatusCancelled
import com.atveyn.app.core.designsystem.theme.StatusGreen
import com.atveyn.app.core.designsystem.theme.StatusGreenBg
import com.atveyn.app.core.designsystem.theme.StatusPending
import com.atveyn.app.core.designsystem.theme.StatusRed
import com.atveyn.app.core.designsystem.theme.StatusRedBg
import com.atveyn.app.core.model.AttendanceStatusType

@Composable
fun StatusBadge(
    status: AttendanceStatusType,
    modifier: Modifier = Modifier
) {
    val (text, textColor, bgColor) = when (status) {
        AttendanceStatusType.ON_TRACK -> Triple("ON TRACK", StatusGreen, StatusGreenBg)
        AttendanceStatusType.WARNING -> Triple("WARNING", StatusAmber, StatusAmberBg)
        AttendanceStatusType.SHORTAGE -> Triple("SHORTAGE", StatusRed, StatusRedBg)
        AttendanceStatusType.NO_DATA -> Triple("NO DATA", Color.Gray, Color.Gray.copy(alpha = 0.15f))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SessionStatusBadge(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}
