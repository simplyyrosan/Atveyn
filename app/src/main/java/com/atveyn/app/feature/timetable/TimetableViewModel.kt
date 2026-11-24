package com.atveyn.app.feature.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atveyn.app.core.database.entity.ClassSessionEntity
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.database.entity.TimetableEntryEntity
import com.atveyn.app.core.model.DayTimetableItem
import com.atveyn.app.core.model.SessionSource
import com.atveyn.app.core.model.SessionStatus
import com.atveyn.app.core.util.DateTimeUtils
import com.atveyn.app.domain.repository.AttendanceRepository
import com.atveyn.app.domain.repository.SubjectRepository
import com.atveyn.app.domain.repository.TimetableRepository
import com.atveyn.app.domain.usecase.ReconcileScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TimetableUiState(
    val selectedDay: Int = DateTimeUtils.dayOfWeekInt(LocalDate.now()),
    val itemsForSelectedDay: List<DayTimetableItem> = emptyList(),
    val overlappingEntryIds: Set<Long> = emptySet(),
    val subjects: List<SubjectEntity> = emptyList()
)

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository,
    private val reconcileScheduleUseCase: ReconcileScheduleUseCase
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(DateTimeUtils.dayOfWeekInt(LocalDate.now()))
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    val uiState: StateFlow<TimetableUiState> = combine(
        _selectedDay,
        timetableRepository.getAllActiveEntries(),
        subjectRepository.getActiveSubjects()
    ) { day, allEntries, subjects ->
        val subjectsMap = subjects.associateBy { it.id }
        val dayEntries = allEntries.filter { it.dayOfWeek == day }.sortedBy { it.startTime }

        // Detect overlaps
        val overlaps = mutableSetOf<Long>()
        for (i in dayEntries.indices) {
            for (j in i + 1 until dayEntries.size) {
                val e1 = dayEntries[i]
                val e2 = dayEntries[j]
                val s1 = DateTimeUtils.parseTime(e1.startTime)
                val end1 = DateTimeUtils.parseTime(e1.endTime)
                val s2 = DateTimeUtils.parseTime(e2.startTime)
                val end2 = DateTimeUtils.parseTime(e2.endTime)

                if (s1.isBefore(end2) && s2.isBefore(end1)) {
                    overlaps.add(e1.id)
                    overlaps.add(e2.id)
                }
            }
        }

        val items = dayEntries.map { entry ->
            val sub = subjectsMap[entry.subjectId]
            DayTimetableItem(
                entryId = entry.id,
                subjectId = entry.subjectId,
                subjectName = sub?.name ?: "Unknown Subject",
                subjectCode = sub?.code ?: "",
                colorHex = sub?.colorHex ?: "#6366F1",
                dayOfWeek = entry.dayOfWeek,
                startTime = entry.startTime,
                endTime = entry.endTime,
                room = entry.room,
                isActive = entry.isActive
            )
        }

        TimetableUiState(
            selectedDay = day,
            itemsForSelectedDay = items,
            overlappingEntryIds = overlaps,
            subjects = subjects
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimetableUiState()
    )

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun addOrUpdateEntry(
        id: Long = 0L,
        subjectId: Long,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String
    ) {
        viewModelScope.launch {
            val entry = TimetableEntryEntity(
                id = id,
                subjectId = subjectId,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                room = room,
                isActive = true
            )
            if (id == 0L) {
                timetableRepository.saveEntry(entry)
            } else {
                timetableRepository.updateEntry(entry)
            }
            reconcileScheduleUseCase.invoke()
        }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            attendanceRepository.deletePendingSessionsForTimetableEntry(id)
            timetableRepository.deleteEntry(id)
            reconcileScheduleUseCase.invoke()
        }
    }

    fun addExtraClass(
        subjectId: Long,
        date: String,
        startTime: String,
        endTime: String,
        room: String
    ) {
        viewModelScope.launch {
            val session = ClassSessionEntity(
                subjectId = subjectId,
                timetableEntryId = null,
                date = date,
                startTime = startTime,
                endTime = endTime,
                status = SessionStatus.PENDING,
                source = SessionSource.EXTRA_CLASS,
                notes = if (room.isNotBlank()) "Room: $room (Extra Class)" else "Extra Class"
            )
            attendanceRepository.saveSession(session)
            reconcileScheduleUseCase.invoke()
        }
    }

    fun cancelClassForDate(subjectId: Long, date: String, startTime: String) {
        viewModelScope.launch {
            val session = attendanceRepository.findExistingScheduledSession(subjectId, date, startTime)
            if (session != null) {
                attendanceRepository.updateStatus(session.id, SessionStatus.CANCELLED)
            }
        }
    }
}

