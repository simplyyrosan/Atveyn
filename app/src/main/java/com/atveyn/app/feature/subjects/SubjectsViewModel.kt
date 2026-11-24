package com.atveyn.app.feature.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.model.SessionSource
import com.atveyn.app.core.model.SessionStatus
import com.atveyn.app.core.model.SubjectAttendanceSummary
import com.atveyn.app.domain.repository.AttendanceRepository
import com.atveyn.app.domain.repository.SubjectRepository
import com.atveyn.app.domain.repository.TimetableRepository
import com.atveyn.app.domain.usecase.CalculateAttendanceUseCase
import com.atveyn.app.domain.usecase.ReconcileScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SubjectsUiState(
    val summaries: List<SubjectAttendanceSummary> = emptyList(),
    val archivedSubjects: List<SubjectEntity> = emptyList(),
    val showArchived: Boolean = false
)

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository,
    private val timetableRepository: TimetableRepository,
    private val calculateAttendanceUseCase: CalculateAttendanceUseCase,
    private val reconcileScheduleUseCase: ReconcileScheduleUseCase
) : ViewModel() {

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    val uiState: StateFlow<SubjectsUiState> = combine(
        subjectRepository.getAllSubjects(),
        attendanceRepository.getAllSessions(),
        _showArchived
    ) { allSubjects, allSessions, showArchivedFlag ->
        val active = allSubjects.filter { !it.isArchived }
        val archived = allSubjects.filter { it.isArchived }

        val displayedSubjects = if (showArchivedFlag) allSubjects else active

        val summaries = displayedSubjects.map { subject ->
            calculateAttendanceUseCase.calculateForSubject(subject, allSessions)
        }

        SubjectsUiState(
            summaries = summaries,
            archivedSubjects = archived,
            showArchived = showArchivedFlag
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubjectsUiState()
    )

    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }

    fun addSubject(name: String, code: String, colorHex: String, targetThreshold: Double) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val trimmedCode = code.trim().ifEmpty { trimmedName.take(4).uppercase() }

            val existing = subjectRepository.findExistingSubject(trimmedName, trimmedCode)
            if (existing != null) {
                // Update existing subject in place instead of creating duplicates
                subjectRepository.updateSubject(
                    existing.copy(
                        name = trimmedName,
                        code = trimmedCode,
                        colorHex = colorHex,
                        targetAttendancePercentage = targetThreshold
                    )
                )
            } else {
                val subject = SubjectEntity(
                    name = trimmedName,
                    code = trimmedCode,
                    colorHex = colorHex,
                    targetAttendancePercentage = targetThreshold
                )
                subjectRepository.saveSubject(subject)
            }
            reconcileScheduleUseCase.invoke()
        }
    }

    fun updateSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            subjectRepository.updateSubject(subject)
            reconcileScheduleUseCase.invoke()
        }
    }

    fun setArchived(subjectId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            subjectRepository.setArchived(subjectId, isArchived)
        }
    }

    fun deleteSubject(subjectId: Long) {
        viewModelScope.launch {
            val subject = subjectRepository.getSubjectByIdSync(subjectId)
            if (subject != null) {
                subjectRepository.deleteSubject(subject)
            }
        }
    }

    fun updateSessionStatus(sessionId: Long, status: SessionStatus) {
        viewModelScope.launch {
            attendanceRepository.updateStatus(sessionId, status)
            reconcileScheduleUseCase.invoke()
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            attendanceRepository.deleteSession(sessionId)
            reconcileScheduleUseCase.invoke()
        }
    }

    fun markDateAttendance(
        subjectId: Long,
        date: String,
        status: SessionStatus
    ) {
        viewModelScope.launch {
            val existingSessions = attendanceRepository.getSessionsForSubjectSync(subjectId).filter { it.date == date }
            if (existingSessions.isNotEmpty()) {
                existingSessions.forEach { session ->
                    attendanceRepository.updateStatus(session.id, status)
                }
            } else {
                val localDate = try { LocalDate.parse(date) } catch (e: Exception) { null }
                val dayOfWeek = localDate?.dayOfWeek?.value ?: 1
                val timetableEntries = timetableRepository.getEntriesForDaySync(dayOfWeek)
                    .filter { it.subjectId == subjectId && it.isActive }

                if (timetableEntries.isNotEmpty()) {
                    timetableEntries.forEach { entry ->
                        attendanceRepository.saveSession(
                            ClassSessionEntity(
                                subjectId = subjectId,
                                timetableEntryId = entry.id,
                                date = date,
                                startTime = entry.startTime,
                                endTime = entry.endTime,
                                status = status,
                                source = SessionSource.SCHEDULED
                            )
                        )
                    }
                } else {
                    attendanceRepository.saveSession(
                        ClassSessionEntity(
                            subjectId = subjectId,
                            date = date,
                            startTime = "09:00",
                            endTime = "10:00",
                            status = status,
                            source = SessionSource.MANUAL
                        )
                    )
                }
            }
            reconcileScheduleUseCase.invoke()
        }
    }

    fun clearDateAttendance(
        subjectId: Long,
        date: String
    ) {
        viewModelScope.launch {
            val existingSessions = attendanceRepository.getSessionsForSubjectSync(subjectId).filter { it.date == date }
            existingSessions.forEach { session ->
                attendanceRepository.deleteSession(session.id)
            }
            reconcileScheduleUseCase.invoke()
        }
    }

    fun logPastClass(
        subjectId: Long,
        date: String,
        startTime: String,
        endTime: String,
        status: SessionStatus
    ) {
        viewModelScope.launch {
            attendanceRepository.saveSession(
                ClassSessionEntity(
                    subjectId = subjectId,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    status = status,
                    source = com.atveyn.app.core.model.SessionSource.MANUAL
                )
            )
            reconcileScheduleUseCase.invoke()
        }
    }

    fun getScheduledDaysForSubject(subjectId: Long): Flow<Set<Int>> {
        return timetableRepository.getAllActiveEntries().map { entries ->
            entries.filter { it.subjectId == subjectId && it.isActive }
                .map { it.dayOfWeek }
                .toSet()
        }
    }
}

