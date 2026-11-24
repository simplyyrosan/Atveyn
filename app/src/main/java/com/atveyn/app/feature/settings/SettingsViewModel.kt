package com.atveyn.app.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atveyn.app.core.database.AtveynDatabase
import com.atveyn.app.core.database.entity.AppSettingsEntity
import com.atveyn.app.core.scheduler.AlarmScheduler
import com.atveyn.app.core.util.FileBackupHelper
import com.atveyn.app.domain.repository.SettingsRepository
import com.atveyn.app.domain.usecase.ReconcileScheduleUseCase
import com.atveyn.app.core.update.AppUpdateManager
import com.atveyn.app.core.update.UpdateInfo
import com.atveyn.app.core.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val database: AtveynDatabase,
    private val alarmScheduler: AlarmScheduler,
    private val reconcileScheduleUseCase: ReconcileScheduleUseCase,
    private val updateManager: AppUpdateManager
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _githubRepo = MutableStateFlow("simplyyrosan/Atveyn")
    val githubRepo: StateFlow<String> = _githubRepo.asStateFlow()

    val settings: StateFlow<AppSettingsEntity> = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsEntity()
        )

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExact()

    fun setDefaultThreshold(threshold: Double) {
        viewModelScope.launch {
            settingsRepository.setDefaultThreshold(threshold)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            reconcileScheduleUseCase.invoke()
        }
    }

    fun setReminderDelay(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setReminderDelayMinutes(minutes)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setSemesterDates(start: String?, end: String?) {
        viewModelScope.launch {
            settingsRepository.setSemesterDates(start, end)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val success = FileBackupHelper.exportToJson(context, database, uri)
            _message.emit(if (success) "Backup exported successfully!" else "Failed to export backup.")
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val success = FileBackupHelper.importFromJson(context, database, uri)
            if (success) {
                reconcileScheduleUseCase.invoke()
                _message.emit("Data restored successfully!")
            } else {
                _message.emit("Failed to restore backup. Invalid file format.")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            database.classSessionDao().clearAll()
            database.timetableDao().clearAll()
            database.subjectDao().clearAll()
            database.holidayDao().clearAll()
            _message.emit("All app data cleared.")
        }
    }

    fun setGithubRepo(repo: String) {
        _githubRepo.value = repo.trim()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            val parts = _githubRepo.value.split("/")
            val owner = parts.getOrNull(0)?.trim()?.ifEmpty { "simplyyrosan" } ?: "simplyyrosan"
            val repo = parts.getOrNull(1)?.trim()?.ifEmpty { "Atveyn" } ?: "Atveyn"

            val result = updateManager.checkForUpdate(owner, repo)
            result.fold(
                onSuccess = { info ->
                    _updateState.value = if (info.isUpdateAvailable) {
                        UpdateState.Available(info)
                    } else {
                        UpdateState.UpToDate(info.currentVersion)
                    }
                },
                onFailure = { error ->
                    _updateState.value = UpdateState.Error(error.localizedMessage ?: "Failed to check for updates.")
                }
            )
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        val downloadUrl = info.downloadUrl
        if (downloadUrl.isNullOrBlank()) {
            _updateState.value = UpdateState.Error("No APK asset found in the latest release. Tap 'View on GitHub' to download.")
            return
        }

        viewModelScope.launch {
            _updateState.value = UpdateState.Downloading(progress = 0f, bytesRead = 0L, totalBytes = 0L)
            val result = updateManager.downloadApk(
                downloadUrl = downloadUrl,
                version = info.latestVersion,
                onProgress = { progress, bytesRead, totalBytes ->
                    _updateState.value = UpdateState.Downloading(progress, bytesRead, totalBytes)
                }
            )

            result.fold(
                onSuccess = { file ->
                    _updateState.value = UpdateState.ReadyToInstall(file, info)
                },
                onFailure = { error ->
                    _updateState.value = UpdateState.Error(error.localizedMessage ?: "Failed to download update APK.")
                }
            )
        }
    }

    fun installUpdate(file: File) {
        val result = updateManager.installApk(file)
        result.onFailure { error ->
            viewModelScope.launch {
                _message.emit(error.localizedMessage ?: "Could not launch package installer.")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }
}

