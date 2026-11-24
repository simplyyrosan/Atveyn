package com.atveyn.app.feature.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.atveyn.app.feature.dashboard.DashboardScreen
import com.atveyn.app.feature.dashboard.DashboardViewModel
import com.atveyn.app.feature.holidays.HolidayScreen
import com.atveyn.app.feature.holidays.HolidayViewModel
import com.atveyn.app.feature.onboarding.OnboardingScreen
import com.atveyn.app.feature.scanner.ScannerScreen
import com.atveyn.app.feature.scanner.ScannerViewModel
import com.atveyn.app.feature.settings.SettingsScreen
import com.atveyn.app.feature.settings.SettingsViewModel
import com.atveyn.app.feature.stats.StatsScreen
import com.atveyn.app.feature.stats.StatsViewModel
import com.atveyn.app.feature.subjects.SubjectDetailScreen
import com.atveyn.app.feature.subjects.SubjectsScreen
import com.atveyn.app.feature.subjects.SubjectsViewModel
import com.atveyn.app.feature.timetable.TimetableScreen
import com.atveyn.app.feature.timetable.TimetableViewModel

@Composable
fun AtveynNavHost(
    navController: NavHostController,
    startDestination: String,
    onCompleteOnboarding: (Boolean) -> Unit,
    subjectRepository: com.atveyn.app.domain.repository.SubjectRepository,
    attendanceRepository: com.atveyn.app.domain.repository.AttendanceRepository
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        NavRoutes.HOME,
        NavRoutes.TIMETABLE,
        NavRoutes.STATS,
        NavRoutes.SUBJECTS,
        NavRoutes.SETTINGS
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AtveynBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else innerPadding.calculateBottomPadding())
        ) {
            composable(NavRoutes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = { startWithScan ->
                        onCompleteOnboarding(startWithScan)
                        if (startWithScan) {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                            }
                            navController.navigate(NavRoutes.SCANNER)
                        } else {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(NavRoutes.HOME) {
                val viewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToScanner = { navController.navigate(NavRoutes.SCANNER) },
                    onNavigateToSubjectDetail = { subId -> navController.navigate(NavRoutes.subjectDetail(subId)) },
                    onNavigateToHolidays = { navController.navigate(NavRoutes.HOLIDAYS) },
                    onNavigateToTimetable = { navController.navigate(NavRoutes.TIMETABLE) }
                )
            }

            composable(NavRoutes.TIMETABLE) {
                val viewModel: TimetableViewModel = hiltViewModel()
                TimetableScreen(
                    viewModel = viewModel,
                    onNavigateToScanner = { navController.navigate(NavRoutes.SCANNER) }
                )
            }

            composable(NavRoutes.STATS) {
                val viewModel: StatsViewModel = hiltViewModel()
                StatsScreen(
                    viewModel = viewModel,
                    onNavigateToSubjectDetail = { subId -> navController.navigate(NavRoutes.subjectDetail(subId)) }
                )
            }

            composable(NavRoutes.SUBJECTS) {
                val viewModel: SubjectsViewModel = hiltViewModel()
                SubjectsScreen(
                    viewModel = viewModel,
                    onNavigateToSubjectDetail = { subId -> navController.navigate(NavRoutes.subjectDetail(subId)) }
                )
            }

            composable(
                route = NavRoutes.SUBJECT_DETAIL,
                arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
            ) { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: -1L
                val viewModel: SubjectsViewModel = hiltViewModel()
                SubjectDetailScreen(
                    subjectId = subjectId,
                    viewModel = viewModel,
                    subjectRepository = subjectRepository,
                    attendanceRepository = attendanceRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.SCANNER) {
                val viewModel: ScannerViewModel = hiltViewModel()
                ScannerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onTimetableSaved = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(NavRoutes.HOLIDAYS) {
                val viewModel: HolidayViewModel = hiltViewModel()
                HolidayScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.SETTINGS) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToHolidays = { navController.navigate(NavRoutes.HOLIDAYS) }
                )
            }
        }
    }
}
