package com.atveyn.app.core.util

import com.atveyn.app.core.model.AttendanceStatusType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun testConductedClassesCalculation() {
        // Conducted = Present + Absent
        assertEquals(10, AttendanceCalculator.calculateConducted(present = 8, absent = 2))
        assertEquals(0, AttendanceCalculator.calculateConducted(present = 0, absent = 0))
        assertEquals(5, AttendanceCalculator.calculateConducted(present = 5, absent = 0))
        assertEquals(3, AttendanceCalculator.calculateConducted(present = 0, absent = 3))
    }

    @Test
    fun testPercentageCalculationZeroDenominator() {
        // When conducted = 0, percentage must be null (N/A)
        val percentage = AttendanceCalculator.calculatePercentage(present = 0, absent = 0)
        assertNull(percentage)
    }

    @Test
    fun testPercentageCalculationStandardCases() {
        // 8 present, 2 absent = 80.0%
        val p1 = AttendanceCalculator.calculatePercentage(present = 8, absent = 2)
        assertEquals(80.0, p1!!, 0.001)

        // 3 present, 1 absent = 75.0%
        val p2 = AttendanceCalculator.calculatePercentage(present = 3, absent = 1)
        assertEquals(75.0, p2!!, 0.001)

        // 10 present, 0 absent = 100.0%
        val p3 = AttendanceCalculator.calculatePercentage(present = 10, absent = 0)
        assertEquals(100.0, p3!!, 0.001)

        // 0 present, 5 absent = 0.0%
        val p4 = AttendanceCalculator.calculatePercentage(present = 0, absent = 5)
        assertEquals(0.0, p4!!, 0.001)
    }

    @Test
    fun testClassesNeededWhenBelowTarget() {
        // Target = 75% (T = 0.75).
        // Current: 6 present, 4 absent -> conducted = 10. Current % = 60%.
        // Formula: ceil((0.75 * 10 - 6) / (1 - 0.75)) = ceil((7.5 - 6) / 0.25) = ceil(1.5 / 0.25) = ceil(6.0) = 6.
        // Check: if 6 more attended: present = 12, conducted = 16. 12/16 = 75.0%. Exactly reaches target!
        val needed1 = AttendanceCalculator.calculateClassesNeeded(
            targetPercentage = 75.0,
            present = 6,
            conducted = 10
        )
        assertEquals(6, needed1)

        // Target = 75%.
        // Current: 10 present, 5 absent -> conducted = 15. % = 66.67%.
        // Formula: ceil((0.75 * 15 - 10) / 0.25) = ceil((11.25 - 10) / 0.25) = ceil(1.25 / 0.25) = 5.
        // Check: 10+5 = 15 present, 15+5 = 20 conducted -> 15/20 = 75%.
        val needed2 = AttendanceCalculator.calculateClassesNeeded(
            targetPercentage = 75.0,
            present = 10,
            conducted = 15
        )
        assertEquals(5, needed2)

        // Target = 80%.
        // Current: 7 present, 3 absent -> conducted = 10. % = 70%.
        // ceil((0.80 * 10 - 7) / 0.20) = ceil((8 - 7) / 0.20) = ceil(1.0 / 0.20) = 5.
        // Check: 7+5 = 12, 10+5 = 15 -> 12/15 = 80.0%.
        val needed3 = AttendanceCalculator.calculateClassesNeeded(
            targetPercentage = 80.0,
            present = 7,
            conducted = 10
        )
        assertEquals(5, needed3)
    }

    @Test
    fun testClassesNeededWhenAlreadyAboveTarget() {
        // Target = 75%. Present = 8, Conducted = 10 (80%).
        val needed = AttendanceCalculator.calculateClassesNeeded(
            targetPercentage = 75.0,
            present = 8,
            conducted = 10
        )
        assertEquals(0, needed)
    }

    @Test
    fun testMarginClassesSafeToMiss() {
        // Target = 75%. Present = 12, Conducted = 12 (100%).
        // Can miss: floor((12 - 0.75 * 12) / 0.75) = floor((12 - 9) / 0.75) = floor(3 / 0.75) = 4.
        // Check: if 4 missed: present = 12, conducted = 16 -> 12/16 = 75.0% (still >= 75%).
        val margin1 = AttendanceCalculator.calculateMarginClasses(
            targetPercentage = 75.0,
            present = 12,
            conducted = 12
        )
        assertEquals(4, margin1)

        // Target = 75%. Present = 6, Conducted = 10 (60% < 75%).
        val margin2 = AttendanceCalculator.calculateMarginClasses(
            targetPercentage = 75.0,
            present = 6,
            conducted = 10
        )
        assertEquals(0, margin2)
    }

    @Test
    fun testStatusAlertLevels() {
        // Target = 75.0%
        // Below target -> SHORTAGE
        assertEquals(
            AttendanceStatusType.SHORTAGE,
            AttendanceCalculator.calculateStatusType(percentage = 74.9, targetPercentage = 75.0)
        )
        assertEquals(
            AttendanceStatusType.SHORTAGE,
            AttendanceCalculator.calculateStatusType(percentage = 60.0, targetPercentage = 75.0)
        )

        // Near target (75.0% <= % < 80.0%) -> WARNING
        assertEquals(
            AttendanceStatusType.WARNING,
            AttendanceCalculator.calculateStatusType(percentage = 75.0, targetPercentage = 75.0)
        )
        assertEquals(
            AttendanceStatusType.WARNING,
            AttendanceCalculator.calculateStatusType(percentage = 78.5, targetPercentage = 75.0)
        )
        assertEquals(
            AttendanceStatusType.WARNING,
            AttendanceCalculator.calculateStatusType(percentage = 79.9, targetPercentage = 75.0)
        )

        // Above target (% >= 80.0%) -> ON_TRACK
        assertEquals(
            AttendanceStatusType.ON_TRACK,
            AttendanceCalculator.calculateStatusType(percentage = 80.0, targetPercentage = 75.0)
        )
        assertEquals(
            AttendanceStatusType.ON_TRACK,
            AttendanceCalculator.calculateStatusType(percentage = 95.0, targetPercentage = 75.0)
        )

        // No data -> NO_DATA
        assertEquals(
            AttendanceStatusType.NO_DATA,
            AttendanceCalculator.calculateStatusType(percentage = null, targetPercentage = 75.0)
        )
    }
}
