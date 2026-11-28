package com.atveyn.app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.atveyn.app.core.designsystem.theme.StatusAmberBorder
import com.atveyn.app.core.designsystem.theme.StatusGreen
import com.atveyn.app.core.designsystem.theme.StatusGreenBg
import com.atveyn.app.core.designsystem.theme.StatusGreenBorder
import com.atveyn.app.core.designsystem.theme.StatusRed
import com.atveyn.app.core.designsystem.theme.StatusRedBg
import com.atveyn.app.core.designsystem.theme.StatusRedBorder
import com.atveyn.app.core.model.AttendanceStatusType

@Composable
fun StatusBadge(
    status: AttendanceStatusType,
    modifier: Modifier = Modifier
) {
    val (text, textColor, bgColor, borderColor) = when (status) {
        AttendanceStatusType.ON_TRACK -> Quad("ON TRACK", StatusGreen, StatusGreenBg, StatusGreenBorder)
        AttendanceStatusType.WARNING -> Quad("WARNING", StatusAmber, StatusAmberBg, StatusAmberBorder)
        AttendanceStatusType.SHORTAGE -> Quad("SHORTAGE", StatusRed, StatusRedBg, StatusRedBorder)
        AttendanceStatusType.NO_DATA -> Quad("NO DATA", Color.Gray, Color.Gray.copy(alpha = 0.15f), Color.Gray.copy(alpha = 0.3f))
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(9.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
fun SessionStatusBadge(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    borderColor: Color = textColor.copy(alpha = 0.35f),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(9.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text.uppercase(),
            color = textColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
