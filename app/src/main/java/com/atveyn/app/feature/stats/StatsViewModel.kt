package com.atveyn.app.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atveyn.app.core.database.entity.AppSettingsEntity
import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.model.OverallAttendanceSummary
import com.atveyn.app.core.model.SessionStatus
import com.atveyn.app.core.model.SubjectAttendanceSummary
import com.atveyn.app.core.util.AttendanceCalculator
import com.atveyn.app.core.util.DateTimeUtils
import com.atveyn.app.domain.repository.AttendanceRepository
import com.atveyn.app.domain.repository.SettingsRepository
import com.atveyn.app.domain.repository.SubjectRepository
import com.atveyn.app.domain.usecase.CalculateAttendanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class WeeklyTrendItem(
    val weekLabel: String,
    val presentCount: Int,
    val absentCount: Int,
    val percentage: Double?
)

data class StatsUiState(
    val overallSummary: OverallAttendanceSummary? = null,
    val subjectSummaries: List<SubjectAttendanceSummary> = emptyList(),
    val weeklyTrends: List<WeeklyTrendItem> = emptyList(),
    val settings: AppSettingsEntity? = null
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val subjectRepository: SubjectRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateAttendanceUseCase: CalculateAttendanceUseCase
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        subjectRepository.getActiveSubjects(),
        attendanceRepository.getAllSessions(),
        settingsRepository.getSettings()
    ) { activeSubjects, allSessions, settings ->
        val target = settings.defaultThreshold
        val overall = calculateAttendanceUseCase.calculateOverall(activeSubjects, allSessions, target)
        val subjectSummaries = activeSubjects.map { subject ->
            calculateAttendanceUseCase.calculateForSubject(subject, allSessions)
        }

        // Compute 4 weekly trend buckets (past 4 weeks)
        val today = LocalDate.now()
        val weeklyTrends = mutableListOf<WeeklyTrendItem>()

        for (w in 3 downTo 0) {
            val weekStart = today.minusDays((w * 7 + 6).toLong())
            val weekEnd = today.minusDays((w * 7).toLong())
            val startStr = DateTimeUtils.formatDate(weekStart)
            val endStr = DateTimeUtils.formatDate(weekEnd)

            val weekSessions = allSessions.filter { it.date in startStr..endStr }
            var p = 0
            var a = 0
            for (s in weekSessions) {
                if (s.status == SessionStatus.PRESENT) p++
                if (s.status == SessionStatus.ABSENT) a++
            }
            val pct = AttendanceCalculator.calculatePercentage(p, a)
            val label = if (w == 0) "This Week" else "${w}w ago"

            weeklyTrends.add(
                WeeklyTrendItem(
                    weekLabel = label,
                    presentCount = p,
                    absentCount = a,
                    percentage = pct
                )
            )
        }

        StatsUiState(
            overallSummary = overall,
            subjectSummaries = subjectSummaries,
            weeklyTrends = weeklyTrends,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )
}
