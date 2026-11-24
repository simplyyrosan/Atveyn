package com.atveyn.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atveyn.app.core.database.entity.AppSettingsEntity
import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.model.ClassScheduleItem
import com.atveyn.app.core.model.OverallAttendanceSummary
import com.atveyn.app.core.model.SessionStatus
import com.atveyn.app.core.model.SubjectAttendanceSummary
import com.atveyn.app.core.util.DateTimeUtils
import com.atveyn.app.domain.repository.AttendanceRepository
import com.atveyn.app.domain.repository.SettingsRepository
import com.atveyn.app.domain.repository.SubjectRepository
import com.atveyn.app.domain.usecase.CalculateAttendanceUseCase
import com.atveyn.app.domain.usecase.GetTodayScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val overallSummary: OverallAttendanceSummary? = null,
    val subjectSummaries: List<SubjectAttendanceSummary> = emptyList(),
    val todayClasses: List<ClassScheduleItem> = emptyList(),
    val pendingSessions: List<ClassSessionEntity> = emptyList(),
    val settings: AppSettingsEntity? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val subjectRepository: SubjectRepository,
    private val settingsRepository: SettingsRepository,
    private val getTodayScheduleUseCase: GetTodayScheduleUseCase,
    private val calculateAttendanceUseCase: CalculateAttendanceUseCase
) : ViewModel() {

    // For undo support
    private var lastUpdatedSessionId: Long? = null
    private var previousStatus: SessionStatus? = null

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        subjectRepository.getActiveSubjects(),
        attendanceRepository.getAllSessions(),
        getTodayScheduleUseCase.invoke(DateTimeUtils.todayDate()),
        attendanceRepository.getPendingSessions(),
        settingsRepository.getSettings()
    ) { activeSubjects, allSessions, todaySchedule, pendingList, settings ->
        val target = settings.defaultThreshold
        val overall = calculateAttendanceUseCase.calculateOverall(activeSubjects, allSessions, target)
        val subjectSummaries = activeSubjects.map { subject ->
            calculateAttendanceUseCase.calculateForSubject(subject, allSessions)
        }

        val pastPending = pendingList.filter { DateTimeUtils.isPastTime(it.date, it.endTime) }

        DashboardUiState(
            overallSummary = overall,
            subjectSummaries = subjectSummaries,
            todayClasses = todaySchedule,
            pendingSessions = pastPending,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun markSessionStatus(sessionId: Long, status: SessionStatus) {
        viewModelScope.launch {
            val session = attendanceRepository.getSessionByIdSync(sessionId)
            if (session != null) {
                lastUpdatedSessionId = sessionId
                previousStatus = session.status
                _canUndo.value = true
                attendanceRepository.updateStatus(sessionId, status)
            }
        }
    }

    fun undoLastAction() {
        val id = lastUpdatedSessionId ?: return
        val prev = previousStatus ?: return
        viewModelScope.launch {
            attendanceRepository.updateStatus(id, prev)
            lastUpdatedSessionId = null
            previousStatus = null
            _canUndo.value = false
        }
    }
}
