package com.atveyn.app.feature.scanner

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.math.max

data class ParsedTimetableEntry(
    val id: String = UUID.randomUUID().toString(),
    var dayOfWeek: Int = 1, // 1=Mon .. 7=Sun
    var subjectName: String = "",
    var subjectCode: String = "",
    var startTime: String = "09:00",
    var endTime: String = "10:00",
    var room: String = "",
    var colorHex: String = "#6366F1"
)

object TimetableParser {

    private val DAYS_MAP = mapOf(
        "monday" to 1, "mon" to 1,
        "tuesday" to 2, "tue" to 2, "tues" to 2,
        "wednesday" to 3, "wed" to 3,
        "thursday" to 4, "thu" to 4, "thur" to 4, "thurs" to 4,
        "friday" to 5, "fri" to 5,
        "saturday" to 6, "sat" to 6,
        "sunday" to 7, "sun" to 7
    )

    // Matches e.g. "8.00AM-9.00AM", "11.00AM-12.00PM", "12.00PM-1.00PM", "1.50PM-2.50PM", "09:00 - 10:00"
    private val TIME_RANGE_REGEX = Pattern.compile(
        "\\b(\\d{1,2})[.:](\\d{2})\\s*(AM|PM)?\\s*[-–—to]+\\s*(\\d{1,2})[.:](\\d{2})\\s*(AM|PM)?\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val TIME_TOKEN_REGEX = Pattern.compile(
        "\\b(\\d{1,2})[.:](\\d{2})\\s*(AM|PM)?\\b",
        Pattern.CASE_INSENSITIVE
    )

    // Matches header default room e.g. "ROOM NO: - E-224", "ROOM NO: - SC 202", "ROOM NO: - E 510", "ROOM NO: - E-304"
    private val DEFAULT_ROOM_REGEX = Pattern.compile(
        "\\bROOM\\s*(?:NO)?[.:\\s-]+([A-Za-z0-9]+(?:[\\s-][A-Za-z0-9]+)?)",
        Pattern.CASE_INSENSITIVE
    )

    // Matches parenthesized/bracketed cell room e.g. "(E-309)", "[C-230]", "(SC 202)", "(SC-306)", "(E-014)"
    private val CELL_ROOM_REGEX = Pattern.compile(
        "[\\[\\(]([A-Za-z0-9]+(?:[\\s-][A-Za-z0-9]+)?)[\\)\\]]"
    )

    data class TextItemWithBox(
        val text: String,
        val rect: Rect = Rect(),
        val left: Float = try { rect.left.toFloat() } catch (_: Throwable) { 0f },
        val top: Float = try { rect.top.toFloat() } catch (_: Throwable) { 0f },
        val right: Float = try { rect.right.toFloat() } catch (_: Throwable) { 0f },
        val bottom: Float = try { rect.bottom.toFloat() } catch (_: Throwable) { 0f },
        val xc: Float = (left + right) / 2f,
        val yc: Float = (top + bottom) / 2f
    )

    data class DayRow(
        val dayName: String,
        val dayNum: Int,
        val yc: Float,
        val right: Float
    )

    data class TimeColumn(
        var xc: Float,
        val startTime: String,
        val endTime: String,
        var width: Float = 0f
    )

    fun normalizeTimeText(text: String): String {
        return TimeNormalizer.normalizeRawText(text)
    }

    fun extractTimeRange(text: String): Pair<String, String>? {
        return TimeNormalizer.extractTimeRange(text)
    }

    private fun extractTimeTokens(text: String): List<String> {
        return TimeNormalizer.extractTimeTokens(text)
    }

    fun isFreeMarker(text: String): Boolean {
        val u = text.trim().uppercase(Locale.US).replace("\\s+".toRegex(), "")
        // Notice: dash '-' is placed at end of character class so it does not match A-Z letters like PS!
        if (u.matches("[XK–—*+\\.><\\-]{1,15}".toRegex())) {
            return true
        }
        if (u in listOf("NIL", "FREE", "OFF", "RECESS", "LUNCH", "BREAK", "NILL", "NOON", "NIL.")) {
            return true
        }
        if (u.matches("[\\[\\(][XK–—*+\\-][\\]\\)]".toRegex())) {
            return true
        }
        return false
    }

    fun cleanSubjectAndRoom(rawText: String, defaultRoom: String): Pair<String, String> {
        var cleanSubj = rawText.trim()
        var cellRoom = defaultRoom

        val roomMatcher = CELL_ROOM_REGEX.matcher(cleanSubj)
        if (roomMatcher.find()) {
            cellRoom = roomMatcher.group(1)?.trim() ?: defaultRoom
            cleanSubj = CELL_ROOM_REGEX.matcher(cleanSubj).replaceAll("").trim()
        } else {
            // Check trailing room code: e.g. "MLW1 E-309" or "AD 1 C-230"
            val trailingRegex = Pattern.compile("\\b([A-Z]{1,2}[-\\s]?[0-9]{3}[A-Za-z]?)\\b")
            val tm = trailingRegex.matcher(cleanSubj)
            var lastMatch = ""
            while (tm.find()) {
                lastMatch = tm.group(1) ?: ""
            }
            if (lastMatch.isNotEmpty() && cleanSubj.length > lastMatch.length + 2) {
                cellRoom = lastMatch.trim()
                cleanSubj = cleanSubj.replace(lastMatch, "").trim()
            }
        }

        if (cellRoom.isBlank()) {
            cellRoom = defaultRoom
        }

        // Clean subject name
        cleanSubj = cleanSubj.replace("[-–—,:;()\\[\\]]+$".toRegex(), "").trim()
        cleanSubj = cleanSubj.replace("^[-–—,:;()\\[\\]]+".toRegex(), "").trim()
        cleanSubj = cleanSubj.replace("\\s+".toRegex(), " ")

        // OCR misread normalizations
        cleanSubj = cleanSubj.replace("(?i)\\bAl\\s*1\\b".toRegex(), "AI 1")
        cleanSubj = cleanSubj.replace("(?i)\\bMSD\\s*LAB\\b".toRegex(), "MCSD LAB")
        cleanSubj = cleanSubj.replace("(?i)\\bMSD\\b".toRegex(), "MCSD")
        cleanSubj = cleanSubj.replace("(?i)\\bSCWP\\s*I\\b".toRegex(), "SCWP 1")
        cleanSubj = cleanSubj.replace("(?i)\\bCSW\\s*I\\b".toRegex(), "CSW 1")
        cleanSubj = cleanSubj.replace("(?i)\\bAD\\s*I\\b".toRegex(), "AD 1")

        return Pair(cleanSubj.trim(), cellRoom.trim())
    }

    fun parseFromMlKitText(mlKitText: Text): List<ParsedTimetableEntry> {
        val items = mutableListOf<TextItemWithBox>()
        val rawLines = mutableListOf<String>()

        for (block in mlKitText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                if (box != null && line.text.isNotBlank()) {
                    items.add(
                        TextItemWithBox(
                            text = line.text.trim(),
                            rect = box,
                            left = box.left.toFloat(),
                            top = box.top.toFloat(),
                            right = box.right.toFloat(),
                            bottom = box.bottom.toFloat()
                        )
                    )
                }
                rawLines.add(line.text)
            }
        }

        // Try 2D Grid parser first
        val gridResult = parseFromGridItems(items)
        if (gridResult.isNotEmpty()) {
            return gridResult
        }

        // Fallback to linear line parser
        return parseFromLines(rawLines)
    }

    fun parseFromGridItems(items: List<TextItemWithBox>): List<ParsedTimetableEntry> {
        if (items.isEmpty()) return emptyList()

        // 1. Extract default room from header
        var defaultRoom = ""
        for (item in items) {
            val rm = DEFAULT_ROOM_REGEX.matcher(item.text)
            if (rm.find()) {
                defaultRoom = rm.group(1)?.trim() ?: ""
                break
            }
        }

        // 2. Detect Day Rows
        val dayRows = mutableListOf<DayRow>()
        for (item in items) {
            val clean = item.text.lowercase(Locale.US).replace("[^a-z]".toRegex(), "")
            for ((dName, dNum) in DAYS_MAP) {
                if (clean == dName && dName.length > 3) {
                    if (dayRows.none { it.dayNum == dNum }) {
                        dayRows.add(DayRow(dName.replaceFirstChar { it.uppercase() }, dNum, item.yc, item.right))
                    }
                    break
                }
            }
        }
        dayRows.sortBy { it.yc }

        if (dayRows.isEmpty()) {
            return emptyList()
        }

        val firstDayY = dayRows.first().yc
        val lastDayY = dayRows.last().yc
        val rowHeight = if (dayRows.size > 1) {
            (lastDayY - firstDayY) / (dayRows.size - 1)
        } else {
            50f
        }

        val dayColRight = dayRows.map { it.right }.maxOrNull() ?: 100f

        // Table bottom boundary: anything below Saturday belongs to course legend/teacher names and is discarded!
        val tableBottomY = lastDayY + rowHeight * 0.55f

        // 3. Detect Time Slot Columns in the header area (above the first day and to the right of DAY column)
        val headerYMin = firstDayY - rowHeight * 2.2f
        val headerYMax = firstDayY - rowHeight * 0.25f
        val headerXMin = dayColRight - 10f

        val headerItems = items.filter { it.yc in headerYMin..headerYMax && it.xc > headerXMin }
        val timeColumns = mutableListOf<TimeColumn>()

        // Estimate column width from header items or day rows
        val allGridItems = items.filter { it.yc in headerYMin..tableBottomY && it.xc > headerXMin }
        val tableRight = allGridItems.map { it.right }.maxOrNull() ?: (dayColRight + 1000f)
        val estimatedColWidth = max((tableRight - dayColRight) / 10f, 30f)
        val clusterThreshold = estimatedColWidth * 0.45f

        // Group header items into column clusters by X coordinate
        val columnClusters = mutableListOf<MutableList<TextItemWithBox>>()
        for (hItem in headerItems.sortedBy { it.xc }) {
            val hasTime = hItem.text.contains("AM", ignoreCase = true) ||
                    hItem.text.contains("PM", ignoreCase = true) ||
                    hItem.text.contains("NOON", ignoreCase = true) ||
                    hItem.text.matches(".*\\d+[.:]\\d+.*".toRegex()) ||
                    hItem.text.contains(".50")

            if (!hasTime) continue

            var matched = false
            for (cluster in columnClusters) {
                val clusterXc = cluster.map { it.xc }.average().toFloat()
                if (abs(clusterXc - hItem.xc) < clusterThreshold) {
                    cluster.add(hItem)
                    matched = true
                    break
                }
            }
            if (!matched) {
                columnClusters.add(mutableListOf(hItem))
            }
        }

        for (cluster in columnClusters) {
            val clusterXc = cluster.map { it.xc }.average().toFloat()
            val clusterText = cluster.sortedBy { it.yc }.joinToString(" ") { it.text }
            val timeRange = extractTimeRange(clusterText) ?: run {
                val tokens = extractTimeTokens(clusterText)
                if (tokens.size >= 2) {
                    Pair(tokens[0], tokens[1])
                } else if (clusterText.contains(".50PM", ignoreCase = true) || clusterText.contains("1.50", ignoreCase = true)) {
                    Pair("13:50", "14:50")
                } else {
                    null
                }
            }

            if (timeRange != null) {
                timeColumns.add(
                    TimeColumn(
                        xc = clusterXc,
                        startTime = timeRange.first,
                        endTime = timeRange.second
                    )
                )
            }
        }

        timeColumns.sortBy { it.xc }

        // If fewer than 2 columns detected, return empty (fallback to linear parser)
        if (timeColumns.size < 2) {
            return emptyList()
        }

        // Canonical 10-slot templates for university timetables
        val isVariantA = headerItems.any {
            it.text.contains(".50", ignoreCase = true) ||
                    it.text.contains("1.50", ignoreCase = true) ||
                    it.text.contains("6.50", ignoreCase = true)
        }
        val standardSlots = if (isVariantA) {
            listOf(
                Pair("08:00", "09:00"),
                Pair("09:00", "10:00"),
                Pair("10:00", "11:00"),
                Pair("11:00", "12:00"),
                Pair("12:00", "13:00"),
                Pair("13:50", "14:50"),
                Pair("14:50", "15:50"),
                Pair("15:50", "16:50"),
                Pair("16:50", "17:50"),
                Pair("17:50", "18:50")
            )
        } else {
            listOf(
                Pair("08:00", "09:00"),
                Pair("09:00", "10:00"),
                Pair("10:00", "11:00"),
                Pair("11:00", "12:00"),
                Pair("12:00", "13:00"),
                Pair("13:00", "14:00"),
                Pair("14:00", "15:00"),
                Pair("15:00", "16:00"),
                Pair("16:00", "17:00"),
                Pair("17:00", "18:00")
            )
        }

        // If timeColumns has between 3 and 9 columns, reconstruct missing columns from the standard grid template
        val finalColumns = if (timeColumns.size in 3..9) {
            val matchedIndices = mutableListOf<Pair<Int, TimeColumn>>()
            for (col in timeColumns) {
                val sIdx = standardSlots.indexOfFirst { it.first == col.startTime }
                if (sIdx >= 0) {
                    matchedIndices.add(Pair(sIdx, col))
                }
            }
            if (matchedIndices.size >= 3) {
                val firstMatch = matchedIndices.first()
                val lastMatch = matchedIndices.last()
                val slotDelta = lastMatch.first - firstMatch.first
                if (slotDelta > 0) {
                    val stepWidth = (lastMatch.second.xc - firstMatch.second.xc) / slotDelta
                    val col0Xc = firstMatch.second.xc - firstMatch.first * stepWidth
                    val reconstructed = mutableListOf<TimeColumn>()
                    for (i in 0 until 10) {
                        val existing = matchedIndices.find { it.first == i }?.second
                        val xc = existing?.xc ?: (col0Xc + i * stepWidth)
                        reconstructed.add(
                            TimeColumn(
                                xc = xc,
                                startTime = standardSlots[i].first,
                                endTime = standardSlots[i].second,
                                width = stepWidth
                            )
                        )
                    }
                    reconstructed
                } else {
                    timeColumns
                }
            } else {
                timeColumns
            }
        } else {
            timeColumns
        }

        finalColumns.sortBy { it.xc }

        val avgColWidth = if (finalColumns.size > 1) {
            (finalColumns.last().xc - finalColumns.first().xc) / (finalColumns.size - 1)
        } else {
            estimatedColWidth
        }
        for (col in finalColumns) {
            col.width = avgColWidth
        }

        // 4. Map Grid Cells Per Day Row (Strictly within table boundaries; footer legend is excluded)
        val entries = mutableListOf<ParsedTimetableEntry>()

        for (dayRow in dayRows) {
            val rowYMin = dayRow.yc - rowHeight * 0.48f
            val rowYMax = dayRow.yc + rowHeight * 0.48f

            val itemsInDay = items.filter { it.yc in rowYMin..rowYMax && it.xc > dayColRight && it.yc <= tableBottomY }
            if (itemsInDay.isEmpty()) continue

            // Separate free markers and raw class items
            val freeItems = mutableListOf<TextItemWithBox>()
            val rawClassItems = mutableListOf<TextItemWithBox>()

            for (item in itemsInDay) {
                if (isFreeMarker(item.text)) {
                    freeItems.add(item)
                } else {
                    rawClassItems.add(item)
                }
            }

            // Cluster multi-line text boxes within the same table cell (e.g. line 1: PSD LAB, line 2: (E-301))
            val mergedClassCells = mutableListOf<TextItemWithBox>()
            val cellClusters = mutableListOf<MutableList<TextItemWithBox>>()
            for (cItem in rawClassItems.sortedBy { it.xc }) {
                var matched = false
                for (cluster in cellClusters) {
                    val clusterXc = cluster.map { it.xc }.average().toFloat()
                    val xOverlap = !(cItem.right < cluster.minOf { it.left } || cItem.left > cluster.maxOf { it.right })
                    if (abs(clusterXc - cItem.xc) < avgColWidth * 0.45f || xOverlap) {
                        cluster.add(cItem)
                        matched = true
                        break
                    }
                }
                if (!matched) {
                    cellClusters.add(mutableListOf(cItem))
                }
            }

            for (cluster in cellClusters) {
                cluster.sortBy { it.top }
                val mergedText = cluster.joinToString(" ") { it.text }
                val minLeft = cluster.minOf { it.left }
                val minTop = cluster.minOf { it.top }
                val maxRight = cluster.maxOf { it.right }
                val maxBottom = cluster.maxOf { it.bottom }
                mergedClassCells.add(
                    TextItemWithBox(
                        text = mergedText,
                        rect = Rect(),
                        left = minLeft,
                        top = minTop,
                        right = maxRight,
                        bottom = maxBottom
                    )
                )
            }

            // Determine which column indices are occupied by free markers (handles single X and multi-X lines like "X X X X X")
            val freeColIndices = mutableSetOf<Int>()
            for (f in freeItems) {
                for (c in finalColumns.indices) {
                    val colXc = finalColumns[c].xc
                    val inRange = colXc in (f.left - avgColWidth * 0.35f)..(f.right + avgColWidth * 0.35f)
                    val nearCenter = abs(colXc - f.xc) < avgColWidth * 0.65f
                    if (inRange || nearCenter) {
                        freeColIndices.add(c)
                    }
                }
            }

            // Split into Morning Session (0..4) and Afternoon Session (5..9) separated by Lunch Break!
            // Morning classes NEVER bleed across lunch into afternoon, and afternoon classes NEVER bleed into morning.
            val sessionRanges = if (finalColumns.size >= 10) {
                listOf(0..4, 5 until finalColumns.size)
            } else {
                val lunchIdx = finalColumns.indexOfFirst { it.endTime in listOf("13:00", "12:00") }
                if (lunchIdx in 1 until finalColumns.size - 1) {
                    listOf(0..lunchIdx, (lunchIdx + 1) until finalColumns.size)
                } else {
                    listOf(0 until finalColumns.size)
                }
            }

            for (sessionRange in sessionRanges) {
                val activeBlocks = mutableListOf<IntRange>()
                var currentBlockStart: Int? = null

                for (c in sessionRange) {
                    if (c !in freeColIndices) {
                        if (currentBlockStart == null) {
                            currentBlockStart = c
                        }
                    } else {
                        if (currentBlockStart != null) {
                            activeBlocks.add(currentBlockStart until c)
                            currentBlockStart = null
                        }
                    }
                }
                if (currentBlockStart != null) {
                    activeBlocks.add(currentBlockStart..sessionRange.last)
                }

                // Process each active block of columns and assign them to the contained class cell(s)
                for (block in activeBlocks) {
                    val blockStart = block.first
                    val blockEnd = block.last
                    val blockLeft = finalColumns[blockStart].xc - avgColWidth * 0.6f
                    val blockRight = finalColumns[blockEnd].xc + avgColWidth * 0.6f

                    val cellsInBlock = mergedClassCells.filter { it.xc in blockLeft..blockRight }.sortedBy { it.xc }
                    if (cellsInBlock.isEmpty()) continue

                    // Assign each column in the active block to the closest cell in the block
                    val cellColsMap = cellsInBlock.associateWith { mutableListOf<Int>() }
                    for (c in blockStart..blockEnd) {
                        val colXc = finalColumns[c].xc
                        val closestCell = cellsInBlock.minByOrNull { abs(it.xc - colXc) } ?: continue
                        if (abs(closestCell.xc - colXc) <= avgColWidth * 2.2f) {
                            cellColsMap[closestCell]?.add(c)
                        }
                    }

                    for (cell in cellsInBlock) {
                        val assignedCols = cellColsMap[cell] ?: emptyList()
                        val (startCol, endCol) = if (assignedCols.isNotEmpty()) {
                            Pair(assignedCols.minOrNull()!!, assignedCols.maxOrNull()!!)
                        } else {
                            val fallbackCol = finalColumns.indices.minByOrNull { abs(finalColumns[it].xc - cell.xc) } ?: blockStart
                            Pair(fallbackCol, fallbackCol)
                        }

                        val startTime = finalColumns[startCol].startTime
                        val endTime = finalColumns[endCol].endTime

                        val (cleanSubj, cellRoom) = cleanSubjectAndRoom(cell.text, defaultRoom)

                        if (cleanSubj.isBlank() || cleanSubj.length < 2) continue

                        // Short subject code directly from table cells, no legend expansion, no teacher names
                        entries.add(
                            ParsedTimetableEntry(
                                dayOfWeek = dayRow.dayNum,
                                subjectName = cleanSubj,
                                subjectCode = cleanSubj,
                                startTime = startTime,
                                endTime = endTime,
                                room = cellRoom
                            )
                        )
                    }
                }
            }
        }

        return entries.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
    }

    fun parseFromRawString(text: String): List<ParsedTimetableEntry> {
        val lines = text.split("\n", "\r\n").map { it.trim() }.filter { it.isNotEmpty() }
        return parseFromLines(lines)
    }

    fun parseFromLines(lines: List<String>): List<ParsedTimetableEntry> {
        val entries = mutableListOf<ParsedTimetableEntry>()
        var currentDay = 1

        for (line in lines) {
            val lower = line.lowercase(Locale.US)
            val detectedDay = detectDayInLine(lower)
            if (detectedDay != null && line.length < 25 && !hasTimePattern(line)) {
                currentDay = detectedDay
                continue
            }

            val timeRange = extractTimeRange(line)
            if (timeRange != null) {
                val dayForThisLine = detectedDay ?: currentDay
                val (subject, room) = cleanSubjectAndRoom(line, "")

                if (subject.isNotBlank()) {
                    entries.add(
                        ParsedTimetableEntry(
                            dayOfWeek = dayForThisLine,
                            subjectName = subject,
                            subjectCode = subject,
                            startTime = timeRange.first,
                            endTime = timeRange.second,
                            room = room
                        )
                    )
                }
            } else if (detectedDay != null) {
                currentDay = detectedDay
            }
        }

        return entries
    }

    private fun detectDayInLine(line: String): Int? {
        val tokens = line.split(Regex("[^a-zA-Z]+"))
        for (token in tokens) {
            val d = DAYS_MAP[token.lowercase(Locale.US)]
            if (d != null) return d
        }
        return null
    }

    private fun hasTimePattern(line: String): Boolean {
        return TIME_RANGE_REGEX.matcher(normalizeTimeText(line)).find()
    }
}
