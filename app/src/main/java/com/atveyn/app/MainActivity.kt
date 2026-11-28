package com.atveyn.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.atveyn.app.core.designsystem.theme.AtveynTheme
import com.atveyn.app.core.designsystem.theme.ThemeMode
import com.atveyn.app.domain.repository.AttendanceRepository
import com.atveyn.app.domain.repository.SettingsRepository
import com.atveyn.app.domain.repository.SubjectRepository
import com.atveyn.app.feature.navigation.AtveynNavHost
import com.atveyn.app.feature.navigation.NavRoutes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var subjectRepository: SubjectRepository

    @Inject
    lateinit var attendanceRepository: AttendanceRepository

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check and request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val settings by settingsRepository.getSettings().collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            val themeMode = when (settings?.themeMode) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }

            val startDestination = if (settings != null && !settings!!.hasCompletedOnboarding) {
                NavRoutes.ONBOARDING
            } else {
                NavRoutes.HOME
            }

            AtveynTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AtveynNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        onCompleteOnboarding = {
                            scope.launch {
                                settingsRepository.setOnboardingCompleted(true)
                            }
                        },
                        subjectRepository = subjectRepository,
                        attendanceRepository = attendanceRepository
                    )
                }
            }
        }
    }
}
