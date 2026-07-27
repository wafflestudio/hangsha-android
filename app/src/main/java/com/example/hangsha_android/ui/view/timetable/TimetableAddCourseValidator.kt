package com.example.hangsha_android.ui.view.timetable

internal enum class TimetableDayOfWeek(val weekday: Int, val label: String, val apiValue: String) {
    MON(0, "월", "MON"),
    TUE(1, "화", "TUE"),
    WED(2, "수", "WED"),
    THU(3, "목", "THU"),
    FRI(4, "금", "FRI"),
    SAT(5, "토", "SAT"),
    SUN(6, "일", "SUN")
}

internal data class EditableTimeSlot(
    val localId: Long,
    val dayOfWeek: TimetableDayOfWeek,
    val startAt: Int,
    val endAt: Int
)

internal data class TimeSlotError(
    val localId: Long,
    val message: String
)

internal data class AddCourseValidationResult(
    val canSubmit: Boolean,
    val titleError: String?,
    val creditError: String?,
    val timeSlotErrors: List<TimeSlotError>
)

internal object TimetableAddCourseValidator {
    fun validate(
        timetableId: String?,
        title: String,
        creditText: String,
        timeSlots: List<EditableTimeSlot>,
        existingTimeSlots: List<EditableTimeSlot>,
        isSubmitting: Boolean
    ): AddCourseValidationResult {
        val timeSlotErrors = mutableListOf<TimeSlotError>()
        val titleError = if (title.trim().isEmpty()) "과목명을 입력하세요" else null
        val creditError = validateCredit(creditText)

        if (timeSlots.isEmpty()) {
            timeSlotErrors += TimeSlotError(-1L, "수업 시간을 1개 이상 추가하세요")
        }

        timeSlots.forEach { slot ->
            when {
                slot.endAt <= slot.startAt -> {
                    timeSlotErrors += TimeSlotError(slot.localId, "종료 시각은 시작 시각보다 늦어야 합니다")
                }
                !slot.startAt.isFiveMinuteUnit() || !slot.endAt.isFiveMinuteUnit() -> {
                    timeSlotErrors += TimeSlotError(slot.localId, "시간은 5분 단위여야 합니다")
                }
            }
        }

        timeSlots.forEach { slot ->
            if (existingTimeSlots.any { existing -> slot.overlaps(existing) }) {
                timeSlotErrors += TimeSlotError(slot.localId, "기존 수업과 시간이 겹칩니다")
            }
        }

        timeSlots.forEachIndexed { index, slot ->
            val hasInternalOverlap = timeSlots.drop(index + 1).any { other -> slot.overlaps(other) }
            if (hasInternalOverlap) {
                timeSlotErrors += TimeSlotError(slot.localId, "추가한 시간끼리 겹칩니다")
            }
        }

        return AddCourseValidationResult(
            canSubmit = timetableId != null &&
                titleError == null &&
                creditError == null &&
                timeSlots.isNotEmpty() &&
                timeSlotErrors.isEmpty() &&
                !isSubmitting,
            titleError = titleError,
            creditError = creditError,
            timeSlotErrors = timeSlotErrors.distinctBy { it.localId to it.message }
        )
    }

    private fun validateCredit(creditText: String): String? {
        val normalized = creditText.trim()
        if (normalized.isEmpty()) {
            return null
        }
        val credit = normalized.toIntOrNull() ?: return "학점은 0 이상의 정수여야 합니다"
        return if (credit < 0) "학점은 0 이상의 정수여야 합니다" else null
    }

    private fun Int.isFiveMinuteUnit(): Boolean = this % 5 == 0

    private fun EditableTimeSlot.overlaps(other: EditableTimeSlot): Boolean {
        return dayOfWeek == other.dayOfWeek && startAt < other.endAt && other.startAt < endAt
    }
}