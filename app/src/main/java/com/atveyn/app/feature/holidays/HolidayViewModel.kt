package com.atveyn.app.feature.holidays

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atveyn.app.core.database.entity.HolidayEntity
import com.atveyn.app.core.util.DateTimeUtils
import com.atveyn.app.domain.repository.HolidayRepository
import com.atveyn.app.domain.usecase.ReconcileScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class HolidayViewModel @Inject constructor(
    private val holidayRepository: HolidayRepository,
    private val reconcileScheduleUseCase: ReconcileScheduleUseCase
) : ViewModel() {

    val holidays: StateFlow<List<HolidayEntity>> = holidayRepository.getAllHolidays()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addSingleHoliday(date: String, name: String) {
        viewModelScope.launch {
            val holiday = HolidayEntity(date = date.trim(), name = name.trim(), isFullDay = true)
            holidayRepository.saveHoliday(holiday)
            reconcileScheduleUseCase.invoke()
        }
    }

    fun addHolidayRange(startDate: String, endDate: String, name: String) {
        viewModelScope.launch {
            try {
                val start = DateTimeUtils.parseDate(startDate)
                val end = DateTimeUtils.parseDate(endDate)
                val days = ChronoUnit.DAYS.between(start, end)

                val holidayList = mutableListOf<HolidayEntity>()
                for (i in 0..days) {
                    val d = start.plusDays(i)
                    holidayList.add(
                        HolidayEntity(
                            date = DateTimeUtils.formatDate(d),
                            name = name.trim(),
                            isFullDay = true
                        )
                    )
                }
                holidayRepository.saveHolidays(holidayList)
                reconcileScheduleUseCase.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteHoliday(holiday: HolidayEntity) {
        viewModelScope.launch {
            holidayRepository.deleteHoliday(holiday)
            reconcileScheduleUseCase.invoke()
        }
    }
}

