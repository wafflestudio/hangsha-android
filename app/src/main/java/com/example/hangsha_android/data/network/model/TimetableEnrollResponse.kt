package com.example.hangsha_android.data.network.model

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class TimetableEnrollListResponse(
    @SerializedName("items")
    val items: List<TimetableEnrollResponse>
)

data class TimetableEnrollResponse(
    @SerializedName("enrollId")
    val enrollId: Long,
    @SerializedName("course")
    val course: TimetableCourseResponse
)

data class TimetableCourseResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("year")
    val year: Int,
    @SerializedName("semester")
    val semester: String,
    @SerializedName("courseTitle")
    val courseTitle: String,
    @SerializedName("source")
    val source: String,
    @SerializedName("timeSlots")
    val timeSlots: List<TimetableCourseTimeSlotResponse>,
    @SerializedName("courseNumber")
    val courseNumber: String?,
    @SerializedName("lectureNumber")
    val lectureNumber: String?,
    @SerializedName("credit")
    val credit: Int?,
    @SerializedName("instructor")
    val instructor: String?
)

data class TimetableCourseTimeSlotResponse(
    @SerializedName("dayOfWeek")
    val dayOfWeek: String,
    @SerializedName("startAt")
    val startAt: Int,
    @SerializedName("endAt")
    val endAt: Int
)

data class CreateCustomTimetableEnrollRequest(
    @SerializedName("year")
    val year: Int,
    @SerializedName("semester")
    val semester: String,
    @SerializedName("courseTitle")
    val courseTitle: String,
    @SerializedName("timeSlots")
    val timeSlots: List<CreateCustomTimetableEnrollTimeSlotRequest>,
    @SerializedName("courseNumber")
    val courseNumber: String?,
    @SerializedName("lectureNumber")
    val lectureNumber: String?,
    @SerializedName("credit")
    val credit: Int?,
    @SerializedName("instructor")
    val instructor: String?
)

data class CreateCustomTimetableEnrollTimeSlotRequest(
    @SerializedName("dayOfWeek")
    val dayOfWeek: String,
    @SerializedName("startAt")
    val startAt: Int,
    @SerializedName("endAt")
    val endAt: Int
)

class UpdateCustomTimetableEnrollRequest private constructor(
    val courseTitle: TimetableEnrollPatchValue<String>,
    val timeSlots: TimetableEnrollPatchValue<List<UpdateCustomTimetableEnrollTimeSlotRequest>>,
    val courseNumber: TimetableEnrollPatchValue<String>,
    val lectureNumber: TimetableEnrollPatchValue<String>,
    val credit: TimetableEnrollPatchValue<Int>,
    val instructor: TimetableEnrollPatchValue<String>
) {
    fun toJsonObject(): JsonObject {
        require(
            courseTitle !is TimetableEnrollPatchValue.Unchanged ||
                timeSlots !is TimetableEnrollPatchValue.Unchanged ||
                courseNumber !is TimetableEnrollPatchValue.Unchanged ||
                lectureNumber !is TimetableEnrollPatchValue.Unchanged ||
                credit !is TimetableEnrollPatchValue.Unchanged ||
                instructor !is TimetableEnrollPatchValue.Unchanged
        ) {
            "At least one enroll field must be included."
        }

        return JsonObject().apply {
            when (courseTitle) {
                is TimetableEnrollPatchValue.Set -> {
                    val normalizedTitle = courseTitle.value.trim()
                    require(normalizedTitle.isNotBlank()) { "Course title cannot be blank." }
                    addProperty("courseTitle", normalizedTitle)
                }
                TimetableEnrollPatchValue.Clear -> error("Course title cannot be null.")
                TimetableEnrollPatchValue.Unchanged -> Unit
            }

            when (timeSlots) {
                is TimetableEnrollPatchValue.Set -> {
                    require(timeSlots.value.isNotEmpty()) { "Time slots cannot be empty." }
                    add("timeSlots", JsonArray().apply {
                        timeSlots.value.forEach { slot ->
                            add(JsonObject().apply {
                                addProperty("dayOfWeek", slot.dayOfWeek)
                                addProperty("startAt", slot.startAt)
                                addProperty("endAt", slot.endAt)
                            })
                        }
                    })
                }
                TimetableEnrollPatchValue.Clear -> error("Time slots cannot be null.")
                TimetableEnrollPatchValue.Unchanged -> Unit
            }

            addOptionalString("courseNumber", courseNumber)
            addOptionalString("lectureNumber", lectureNumber)
            addOptionalInt("credit", credit)
            addOptionalString("instructor", instructor)
        }
    }

    companion object {
        fun partial(
            courseTitle: TimetableEnrollPatchValue<String> = TimetableEnrollPatchValue.Unchanged,
            timeSlots: TimetableEnrollPatchValue<List<UpdateCustomTimetableEnrollTimeSlotRequest>> = TimetableEnrollPatchValue.Unchanged,
            courseNumber: TimetableEnrollPatchValue<String> = TimetableEnrollPatchValue.Unchanged,
            lectureNumber: TimetableEnrollPatchValue<String> = TimetableEnrollPatchValue.Unchanged,
            credit: TimetableEnrollPatchValue<Int> = TimetableEnrollPatchValue.Unchanged,
            instructor: TimetableEnrollPatchValue<String> = TimetableEnrollPatchValue.Unchanged
        ): UpdateCustomTimetableEnrollRequest {
            return UpdateCustomTimetableEnrollRequest(
                courseTitle = courseTitle,
                timeSlots = timeSlots,
                courseNumber = courseNumber,
                lectureNumber = lectureNumber,
                credit = credit,
                instructor = instructor
            )
        }
    }
}

data class UpdateCustomTimetableEnrollTimeSlotRequest(
    val dayOfWeek: String,
    val startAt: String,
    val endAt: String
)

sealed interface TimetableEnrollPatchValue<out T> {
    data class Set<T>(val value: T) : TimetableEnrollPatchValue<T>

    data object Clear : TimetableEnrollPatchValue<Nothing>

    data object Unchanged : TimetableEnrollPatchValue<Nothing>
}

private fun JsonObject.addOptionalString(
    name: String,
    value: TimetableEnrollPatchValue<String>
) {
    when (value) {
        is TimetableEnrollPatchValue.Set -> addProperty(name, value.value)
        TimetableEnrollPatchValue.Clear -> add(name, JsonNull.INSTANCE)
        TimetableEnrollPatchValue.Unchanged -> Unit
    }
}

private fun JsonObject.addOptionalInt(
    name: String,
    value: TimetableEnrollPatchValue<Int>
) {
    when (value) {
        is TimetableEnrollPatchValue.Set -> addProperty(name, value.value)
        TimetableEnrollPatchValue.Clear -> add(name, JsonNull.INSTANCE)
        TimetableEnrollPatchValue.Unchanged -> Unit
    }
}

