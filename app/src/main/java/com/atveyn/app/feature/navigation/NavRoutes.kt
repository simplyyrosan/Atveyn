package com.atveyn.app.feature.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val TIMETABLE = "timetable"
    const val STATS = "stats"
    const val SUBJECTS = "subjects"
    const val SUBJECT_DETAIL = "subject_detail/{subjectId}"
    const val SCANNER = "scanner"
    const val HOLIDAYS = "holidays"
    const val SETTINGS = "settings"

    fun subjectDetail(subjectId: Long) = "subject_detail/$subjectId"
}

enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME(NavRoutes.HOME, "Home", Icons.Default.Home),
    TIMETABLE(NavRoutes.TIMETABLE, "Timetable", Icons.Default.CalendarMonth),
    STATS(NavRoutes.STATS, "Stats", Icons.Default.Insights),
    SUBJECTS(NavRoutes.SUBJECTS, "Subjects", Icons.AutoMirrored.Filled.MenuBook),
    SETTINGS(NavRoutes.SETTINGS, "Settings", Icons.Default.Settings)
}
