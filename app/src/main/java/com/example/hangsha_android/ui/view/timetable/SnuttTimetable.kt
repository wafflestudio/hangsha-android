package com.example.hangsha_android.ui.view.timetable

import com.example.hangsha_android.data.network.model.CreateCustomTimetableEnrollTimeSlotRequest
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal data class SnuttTimetable(
    val title: String,
    val year: Int,
    val semester: Int,
    val lectures: List<SnuttLecture>
)

internal data class SnuttLecture(
    val courseTitle: String,
    val classPlaceAndTimes: List<SnuttTimeSlot>,
    val courseNumber: String?,
    val lectureNumber: String?,
    val credit: Int?,
    val instructor: String?
)

internal data class SnuttTimeSlot(
    val day: Int,
    val startMinute: Int,
    val endMinute: Int
)

internal data class SnuttCourseImport(
    val year: Int,
    val semester: String,
    val courseTitle: String,
    val timeSlots: List<CreateCustomTimetableEnrollTimeSlotRequest>,
    val courseNumber: String?,
    val lectureNumber: String?,
    val credit: Int?,
    val instructor: String?
)

internal data class SnuttImportData(
    val timetableName: String,
    val year: Int,
    val semester: String,
    val courses: List<SnuttCourseImport>,
    val excludedCourseCount: Int,
    val weekendCourseCount: Int
)

internal object SnuttTimetableMessageParser {
    fun parse(data: String): SnuttTimetable? {
        val message = runCatching { JsonParser.parseString(data) }
            .getOrNull()
            ?.asObjectOrNull()
            ?: return null
        if (message.readString("type") != SNUTT_MESSAGE_TYPE) return null

        return parseTimetable(message["payload"])
    }

    private fun parseTimetable(value: JsonElement?): SnuttTimetable? {
        val timetable = value.asObjectOrNull() ?: return null
        val title = timetable.readString("title")?.takeIf { it.isNotEmpty() } ?: return null
        val year = timetable.readInt("year") ?: return null
        val semester = timetable.readInt("semester")
            ?.takeIf { it in SEMESTER_CODES }
            ?: return null
        val rawLectures = timetable.firstPresent("lectures", "lecture_list")
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?: return null

        val lectures = buildList {
            rawLectures.forEach { rawLecture ->
                add(parseLecture(rawLecture) ?: return null)
            }
        }
        return SnuttTimetable(
            title = title,
            year = year,
            semester = semester,
            lectures = lectures
        )
    }

    private fun parseLecture(value: JsonElement): SnuttLecture? {
        val lecture = value.asObjectOrNull() ?: return null
        val courseTitle = lecture.readString("courseTitle", "course_title")
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val rawTimeSlots = lecture.firstPresent("classPlaceAndTimes", "class_time_json")
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?: return null

        val timeSlots = rawTimeSlots.mapNotNull(::parseTimeSlot)
        return SnuttLecture(
            courseTitle = courseTitle,
            classPlaceAndTimes = timeSlots,
            courseNumber = lecture.readString("courseNumber", "course_number"),
            lectureNumber = lecture.readString("lectureNumber", "lecture_number"),
            credit = lecture.readInt("credit"),
            instructor = lecture.readString("instructor")
        )
    }

    private fun parseTimeSlot(value: JsonElement): SnuttTimeSlot? {
        val timeSlot = value.asObjectOrNull() ?: return null
        val day = timeSlot.readInt("day") ?: return null
        val startMinute = timeSlot.readInt("startMinute") ?: return null
        val endMinute = timeSlot.readInt("endMinute") ?: return null
        if (day !in 0..6 || startMinute !in 0 until endMinute || endMinute > MINUTES_PER_DAY) {
            return null
        }
        return SnuttTimeSlot(day, startMinute, endMinute)
    }
}

internal object SnuttTimetableMapper {
    fun toImportData(timetable: SnuttTimetable): SnuttImportData {
        val semester = SEMESTER_CODES.getValue(timetable.semester)
        val courses = timetable.lectures
            .filter { it.classPlaceAndTimes.isNotEmpty() }
            .map { lecture ->
                SnuttCourseImport(
                    year = timetable.year,
                    semester = semester,
                    courseTitle = lecture.courseTitle,
                    timeSlots = lecture.classPlaceAndTimes.map { slot ->
                        CreateCustomTimetableEnrollTimeSlotRequest(
                            dayOfWeek = DAY_OF_WEEK_CODES[slot.day],
                            startAt = slot.startMinute,
                            endAt = slot.endMinute
                        )
                    },
                    courseNumber = lecture.courseNumber,
                    lectureNumber = lecture.lectureNumber,
                    credit = lecture.credit,
                    instructor = lecture.instructor
                )
            }

        return SnuttImportData(
            timetableName = "${timetable.title.trim().ifEmpty { "SNUTT 시간표" }} (SNUTT)",
            year = timetable.year,
            semester = semester,
            courses = courses,
            excludedCourseCount = timetable.lectures.size - courses.size,
            weekendCourseCount = courses.count { course ->
                course.timeSlots.any { slot -> slot.dayOfWeek == "SAT" || slot.dayOfWeek == "SUN" }
            }
        )
    }
}

private fun JsonElement?.asObjectOrNull(): JsonObject? {
    return this?.takeIf { it.isJsonObject }?.asJsonObject
}

private fun JsonObject.firstPresent(vararg names: String): JsonElement? {
    return names.firstNotNullOfOrNull { name -> get(name)?.takeUnless { it.isJsonNull } }
}

private fun JsonObject.readString(vararg names: String): String? {
    val primitive = firstPresent(*names)
        ?.takeIf { it.isJsonPrimitive }
        ?.asJsonPrimitive
        ?: return null
    if (!primitive.isString) return null
    return primitive.asString
}

private fun JsonObject.readInt(vararg names: String): Int? {
    val primitive = firstPresent(*names)
        ?.takeIf { it.isJsonPrimitive }
        ?.asJsonPrimitive
        ?: return null
    if (!primitive.isNumber) return null
    return runCatching { primitive.asBigDecimal.intValueExact() }.getOrNull()
}

private const val SNUTT_MESSAGE_TYPE = "SNUTT_TIMETABLE_SELECTED"
private const val MINUTES_PER_DAY = 24 * 60
private val DAY_OF_WEEK_CODES = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
private val SEMESTER_CODES = mapOf(
    1 to "SPRING",
    2 to "SUMMER",
    3 to "FALL",
    4 to "WINTER"
)
