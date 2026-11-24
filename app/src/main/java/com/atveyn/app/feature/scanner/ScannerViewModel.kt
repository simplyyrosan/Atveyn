package com.atveyn.app.feature.scanner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atveyn.app.core.database.entity.SubjectEntity
import com.atveyn.app.core.database.entity.TimetableEntryEntity
import com.atveyn.app.core.designsystem.theme.SubjectPalette
import com.atveyn.app.core.util.ImageUtils
import com.atveyn.app.domain.repository.SubjectRepository
import com.atveyn.app.domain.repository.TimetableRepository
import com.atveyn.app.domain.usecase.ReconcileScheduleUseCase
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface ScannerUiState {
    object Idle : ScannerUiState
    object Processing : ScannerUiState
    data class Review(val entries: List<ParsedTimetableEntry>) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
    object Saved : ScannerUiState
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subjectRepository: SubjectRepository,
    private val timetableRepository: TimetableRepository,
    private val reconcileScheduleUseCase: ReconcileScheduleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _entries = MutableStateFlow<List<ParsedTimetableEntry>>(emptyList())
    val entries: StateFlow<List<ParsedTimetableEntry>> = _entries.asStateFlow()

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun startManualEntry() {
        val initialList = listOf(
            ParsedTimetableEntry(
                dayOfWeek = 1,
                subjectName = "",
                subjectCode = "",
                startTime = "09:00",
                endTime = "10:00",
                room = ""
            )
        )
        _entries.value = initialList
        _uiState.value = ScannerUiState.Review(initialList)
    }

    fun processImageUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Processing
            try {
                val image = withContext(Dispatchers.IO) {
                    try {
                        InputImage.fromFilePath(context, uri)
                    } catch (e: Exception) {
                        val bitmap = ImageUtils.decodeAndNormalizeBitmap(context, uri, maxDimension = 1800)
                        if (bitmap != null) InputImage.fromBitmap(bitmap, 0) else null
                    }
                }

                if (image == null) {
                    _uiState.value = ScannerUiState.Error("Failed to decode image safely. You can enter classes manually.")
                    return@launch
                }

                val result = withContext(Dispatchers.IO) { Tasks.await(textRecognizer.process(image)) }
                val parsed = TimetableParser.parseFromMlKitText(result)

                if (parsed.isEmpty()) {
                    val rawText = result.text
                    val fallback = listOf(
                        ParsedTimetableEntry(
                            dayOfWeek = 1,
                            subjectName = rawText.lines().firstOrNull { l -> l.isNotBlank() } ?: "Subject 1",
                            startTime = "09:00",
                            endTime = "10:00"
                        )
                    )
                    _entries.value = fallback
                    _uiState.value = ScannerUiState.Review(fallback)
                } else {
                    _entries.value = parsed
                    _uiState.value = ScannerUiState.Review(parsed)
                }
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error("OCR processing: ${e.localizedMessage}. You can add classes manually.")
            }
        }
    }

    fun processPdfUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Processing
            try {
                val bitmaps = withContext(Dispatchers.IO) {
                    PdfRendererHelper.renderPdfToBitmaps(context, uri)
                }

                if (bitmaps.isEmpty()) {
                    _uiState.value = ScannerUiState.Error("Could not render PDF. You can enter classes manually.")
                    return@launch
                }

                val allParsed = mutableListOf<ParsedTimetableEntry>()
                for (bitmap in bitmaps) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val result = withContext(Dispatchers.IO) { Tasks.await(textRecognizer.process(image)) }
                    val parsed = TimetableParser.parseFromMlKitText(result)
                    allParsed.addAll(parsed)
                }

                if (allParsed.isEmpty()) {
                    startManualEntry()
                } else {
                    _entries.value = allParsed
                    _uiState.value = ScannerUiState.Review(allParsed)
                }
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error("PDF processing failed: ${e.localizedMessage}")
            }
        }
    }

    fun updateEntry(index: Int, updated: ParsedTimetableEntry) {
        val current = _entries.value.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _entries.value = current
            _uiState.value = ScannerUiState.Review(current)
        }
    }

    fun addEntry() {
        val current = _entries.value.toMutableList()
        current.add(
            ParsedTimetableEntry(
                dayOfWeek = 1,
                subjectName = "",
                subjectCode = "",
                startTime = "10:00",
                endTime = "11:00",
                room = ""
            )
        )
        _entries.value = current
        _uiState.value = ScannerUiState.Review(current)
    }

    fun removeEntry(index: Int) {
        val current = _entries.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _entries.value = current
            _uiState.value = ScannerUiState.Review(current)
        }
    }

    fun saveTimetable() {
        val itemsToSave = _entries.value.filter { it.subjectName.isNotBlank() }
        if (itemsToSave.isEmpty()) {
            _uiState.value = ScannerUiState.Error("Please enter at least one valid subject name.")
            return
        }

        val report = TimetableValidator.validateEntries(itemsToSave)
        if (!report.isValid) {
            val firstError = report.errors.firstOrNull()?.message ?: "Invalid timetable entries detected."
            _uiState.value = ScannerUiState.Error("Cannot save: $firstError")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingSubjects = subjectRepository.getAllSubjectsSync().toMutableList()
                val subjectMap = mutableMapOf<String, Long>()

                fun norm(str: String): String = str.lowercase().replace("[^a-z0-9]".toRegex(), "")

                for (s in existingSubjects) {
                    val nName = norm(s.name)
                    val nCode = norm(s.code)
                    if (nName.isNotEmpty()) subjectMap[nName] = s.id
                    if (nCode.isNotEmpty()) subjectMap[nCode] = s.id
                }

                var colorIdx = 0
                val timetableEntries = mutableListOf<TimetableEntryEntity>()

                for (item in itemsToSave) {
                    val nName = norm(item.subjectName)
                    val nCode = norm(item.subjectCode)

                    var subjectId = subjectMap[nCode] ?: (if (nName.isNotEmpty()) subjectMap[nName] else null)

                    if (subjectId == null) {
                        val colorHex = String.format("#%06X", (0xFFFFFF and SubjectPalette[colorIdx % SubjectPalette.size].hashCode()))
                        colorIdx++

                        val newSubject = SubjectEntity(
                            name = item.subjectName.trim(),
                            code = item.subjectCode.trim().ifEmpty { item.subjectName.take(4).uppercase() },
                            colorHex = colorHex,
                            targetAttendancePercentage = 75.0
                        )
                        subjectId = subjectRepository.saveSubject(newSubject)
                        if (nName.isNotEmpty()) subjectMap[nName] = subjectId
                        if (nCode.isNotEmpty()) subjectMap[nCode] = subjectId
                    }

                    timetableEntries.add(
                        TimetableEntryEntity(
                            subjectId = subjectId,
                            dayOfWeek = item.dayOfWeek,
                            startTime = item.startTime,
                            endTime = item.endTime,
                            room = item.room.trim(),
                            isActive = true
                        )
                    )
                }

                timetableRepository.saveEntries(timetableEntries)
                reconcileScheduleUseCase.invoke(daysPast = 0)

                _uiState.value = ScannerUiState.Saved
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error("Failed to save timetable: ${e.localizedMessage}")
            }
        }
    }

    fun reset() {
        _entries.value = emptyList()
        _uiState.value = ScannerUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        textRecognizer.close()
    }
}
