package com.example.hangsha_android.ui.view.timetable

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableAddCourseValidatorTest {
    @Test
    fun validate_rejectsBlankTitle() {
        val result = validate(
            title = " ",
            timeSlots = listOf(slot(1, TimetableDayOfWeek.MON, 600, 660))
        )

        assertFalse(result.canSubmit)
    }

    @Test
    fun validate_rejectsEmptyTimeSlots() {
        val result = validate(
            title = "자료구조",
            timeSlots = emptyList()
        )

        assertFalse(result.canSubmit)
    }

    @Test
    fun validate_rejectsEndAtBeforeOrEqualStartAt() {
        val result = validate(
            title = "자료구조",
            timeSlots = listOf(slot(1, TimetableDayOfWeek.MON, 660, 660))
        )

        assertFalse(result.canSubmit)
    }

    @Test
    fun validate_rejectsExistingOverlapOnSameDay() {
        val result = validate(
            title = "자료구조",
            timeSlots = listOf(slot(1, TimetableDayOfWeek.MON, 600, 660)),
            existingTimeSlots = listOf(slot(2, TimetableDayOfWeek.MON, 630, 700))
        )

        assertFalse(result.canSubmit)
    }

    @Test
    fun validate_allowsTouchingTimeSlots() {
        val result = validate(
            title = "자료구조",
            timeSlots = listOf(slot(1, TimetableDayOfWeek.MON, 600, 660)),
            existingTimeSlots = listOf(slot(2, TimetableDayOfWeek.MON, 660, 720))
        )

        assertTrue(result.canSubmit)
    }

    @Test
    fun validate_allowsSameTimeOnDifferentDay() {
        val result = validate(
            title = "자료구조",
            timeSlots = listOf(slot(1, TimetableDayOfWeek.MON, 600, 660)),
            existingTimeSlots = listOf(slot(2, TimetableDayOfWeek.TUE, 600, 660))
        )

        assertTrue(result.canSubmit)
    }

    @Test
    fun validate_rejectsInternalOverlap() {
        val result = validate(
            title = "자료구조",
            timeSlots = listOf(
                slot(1, TimetableDayOfWeek.MON, 600, 660),
                slot(2, TimetableDayOfWeek.MON, 630, 700)
            )
        )

        assertFalse(result.canSubmit)
    }

    @Test
    fun validate_rejectsSubmittingState() {
        val result = validate(
            title = "자료구조",
            timeSlots = listOf(slot(1, TimetableDayOfWeek.MON, 600, 660)),
            isSubmitting = true
        )

        assertFalse(result.canSubmit)
    }

    private fun validate(
        title: String,
        timeSlots: List<EditableTimeSlot>,
        existingTimeSlots: List<EditableTimeSlot> = emptyList(),
        isSubmitting: Boolean = false
    ): AddCourseValidationResult {
        return TimetableAddCourseValidator.validate(
            timetableId = "timetable-1",
            title = title,
            creditText = "3",
            timeSlots = timeSlots,
            existingTimeSlots = existingTimeSlots,
            isSubmitting = isSubmitting
        )
    }

    private fun slot(
        id: Long,
        day: TimetableDayOfWeek,
        start: Int,
        end: Int
    ): EditableTimeSlot {
        return EditableTimeSlot(
            localId = id,
            dayOfWeek = day,
            startAt = start,
            endAt = end
        )
    }
}
