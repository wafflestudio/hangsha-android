package com.example.hangsha_android.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hangsha_android.data.network.model.CreateCustomTimetableEnrollRequest
import com.example.hangsha_android.data.network.model.TimetableCourseResponse
import com.example.hangsha_android.data.network.model.TimetableCourseTimeSlotResponse
import com.example.hangsha_android.data.network.model.TimetableEnrollListResponse
import com.example.hangsha_android.data.network.model.TimetableEnrollResponse
import com.example.hangsha_android.data.network.model.UpdateCustomTimetableEnrollRequest
import com.example.hangsha_android.data.network.model.TimetableEnrollPatchValue
import com.example.hangsha_android.data.network.model.TimetableListResponse
import com.example.hangsha_android.data.network.model.TimetableResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.guestTimetablesDataStore by preferencesDataStore(name = "guest_timetables")

@Singleton
class GuestTimetableLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val gson = Gson()

    val timetables: Flow<List<StoredGuestTimetable>> =
        context.guestTimetablesDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences -> parseTimetables(preferences[GUEST_TIMETABLES_JSON]) }

    suspend fun getTimetables(
        year: Int,
        semester: String
    ): TimetableListResponse {
        return TimetableListResponse(
            items = timetables.first()
                .filter { timetable -> timetable.year == year && timetable.semester == semester }
                .map { timetable -> timetable.toTimetableResponse() }
        )
    }

    suspend fun createTimetable(
        name: String,
        year: Int,
        semester: String
    ): TimetableResponse {
        val currentTimetables = timetables.first()
        val nextId = (currentTimetables.minOfOrNull { timetable -> timetable.id } ?: 0L) - 1L
        val timetable = StoredGuestTimetable(
            id = nextId,
            name = name.trim(),
            year = year,
            semester = semester,
            courses = emptyList()
        )
        replaceAll(currentTimetables + timetable)
        return timetable.toTimetableResponse()
    }

    suspend fun updateTimetableName(
        timetableId: Long,
        name: String
    ): TimetableResponse {
        val currentTimetables = timetables.first()
        val target = currentTimetables.firstOrNull { timetable -> timetable.id == timetableId }
            ?: throw NoSuchElementException("Timetable was not found.")
        val updatedTimetable = target.copy(name = name.trim())
        replaceAll(currentTimetables.map { timetable ->
            if (timetable.id == timetableId) updatedTimetable else timetable
        })
        return updatedTimetable.toTimetableResponse()
    }

    suspend fun deleteTimetable(timetableId: Long) {
        replaceAll(timetables.first().filterNot { timetable -> timetable.id == timetableId })
    }

    suspend fun getEnrolls(timetableId: Long): TimetableEnrollListResponse {
        val target = timetables.first().firstOrNull { timetable -> timetable.id == timetableId }
            ?: throw NoSuchElementException("Timetable was not found.")
        return TimetableEnrollListResponse(
            items = target.courses.orEmpty().map { enroll -> enroll.toTimetableEnrollResponse() }
        )
    }

    suspend fun getEnroll(
        timetableId: Long,
        enrollId: Long
    ): TimetableEnrollResponse {
        val target = timetables.first().firstOrNull { timetable -> timetable.id == timetableId }
            ?: throw NoSuchElementException("Timetable was not found.")
        return target.courses.orEmpty()
            .firstOrNull { enroll -> enroll.enrollId == enrollId }
            ?.toTimetableEnrollResponse()
            ?: throw NoSuchElementException("Enroll was not found.")
    }

    suspend fun createCustomEnroll(
        timetableId: Long,
        request: CreateCustomTimetableEnrollRequest
    ): TimetableEnrollResponse {
        val currentTimetables = timetables.first()
        val target = currentTimetables.firstOrNull { timetable -> timetable.id == timetableId }
            ?: throw NoSuchElementException("Timetable was not found.")
        val nextEnrollId = (currentTimetables.flatMap { timetable -> timetable.courses.orEmpty() }
            .minOfOrNull { enroll -> enroll.enrollId } ?: 0L) - 1L
        val storedEnroll = StoredGuestEnroll(
            enrollId = nextEnrollId,
            course = StoredGuestCourse(
                id = nextEnrollId,
                year = request.year,
                semester = request.semester,
                courseTitle = request.courseTitle,
                source = "CUSTOM",
                timeSlots = request.timeSlots.map { slot ->
                    StoredGuestCourseTimeSlot(
                        dayOfWeek = slot.dayOfWeek,
                        startAt = slot.startAt,
                        endAt = slot.endAt
                    )
                },
                courseNumber = request.courseNumber,
                lectureNumber = request.lectureNumber,
                credit = request.credit,
                instructor = request.instructor
            )
        )
        ensureNoTimeConflict(
            candidate = storedEnroll,
            existingEnrolls = target.courses.orEmpty()
        )
        replaceAll(currentTimetables.map { timetable ->
            if (timetable.id == timetableId) {
                target.copy(courses = target.courses.orEmpty() + storedEnroll)
            } else {
                timetable
            }
        })
        return storedEnroll.toTimetableEnrollResponse()
    }


    suspend fun updateCustomEnroll(
        timetableId: Long,
        enrollId: Long,
        request: UpdateCustomTimetableEnrollRequest
    ): TimetableEnrollResponse {
        request.toJsonObject()
        val currentTimetables = timetables.first()
        val targetTimetable = currentTimetables.firstOrNull { timetable -> timetable.id == timetableId }
            ?: throw NoSuchElementException("Timetable was not found.")
        val targetEnroll = targetTimetable.courses.orEmpty().firstOrNull { enroll -> enroll.enrollId == enrollId }
            ?: throw NoSuchElementException("Enroll was not found.")
        require(targetEnroll.course.source == "CUSTOM") {
            "Only custom enrolls can be updated."
        }

        val updatedEnroll = targetEnroll.copy(
            course = targetEnroll.course.patchWith(request)
        )
        ensureNoTimeConflict(
            candidate = updatedEnroll,
            existingEnrolls = targetTimetable.courses.orEmpty().filterNot { enroll -> enroll.enrollId == enrollId }
        )
        replaceAll(currentTimetables.map { timetable ->
            if (timetable.id == timetableId) {
                timetable.copy(courses = targetTimetable.courses.orEmpty().map { enroll ->
                    if (enroll.enrollId == enrollId) updatedEnroll else enroll
                })
            } else {
                timetable
            }
        })
        return updatedEnroll.toTimetableEnrollResponse()
    }

    suspend fun deleteEnroll(
        timetableId: Long,
        enrollId: Long
    ) {
        replaceAll(timetables.first().map { timetable ->
            if (timetable.id == timetableId) {
                timetable.copy(courses = timetable.courses.orEmpty().filterNot { enroll -> enroll.enrollId == enrollId })
            } else {
                timetable
            }
        })
    }

    suspend fun clearAllData() {
        context.guestTimetablesDataStore.edit { preferences -> preferences.clear() }
    }

    private suspend fun replaceAll(items: List<StoredGuestTimetable>) {
        context.guestTimetablesDataStore.edit { preferences ->
            preferences[GUEST_TIMETABLES_JSON] = gson.toJson(
                items
                    .filter { timetable -> timetable.name.isNotBlank() }
                    .distinctBy { timetable -> timetable.id }
                    .sortedWith(compareBy({ timetable -> timetable.year }, { timetable -> timetable.semester }, { timetable -> timetable.id }))
            )
        }
    }

    private fun parseTimetables(json: String?): List<StoredGuestTimetable> {
        if (json.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching {
            gson.fromJson<List<StoredGuestTimetable>>(json, storedTimetableListType)
                .map { timetable -> timetable.copy(courses = timetable.courses.orEmpty()) }
        }.getOrDefault(emptyList())
    }

    companion object {
        private val GUEST_TIMETABLES_JSON = stringPreferencesKey("guest_timetables_json")
        private val storedTimetableListType = object : TypeToken<List<StoredGuestTimetable>>() {}.type
    }
}

data class StoredGuestTimetable(
    val id: Long,
    val name: String,
    val year: Int,
    val semester: String,
    val courses: List<StoredGuestEnroll>? = emptyList()
)

data class StoredGuestEnroll(
    val enrollId: Long,
    val course: StoredGuestCourse
)

data class StoredGuestCourse(
    val id: Long,
    val year: Int,
    val semester: String,
    val courseTitle: String,
    val source: String,
    val timeSlots: List<StoredGuestCourseTimeSlot>,
    val courseNumber: String?,
    val lectureNumber: String?,
    val credit: Int?,
    val instructor: String?
)

data class StoredGuestCourseTimeSlot(
    val dayOfWeek: String,
    val startAt: Int,
    val endAt: Int
)

private fun StoredGuestTimetable.toTimetableResponse(): TimetableResponse {
    return TimetableResponse(
        id = id,
        name = name,
        year = year,
        semester = semester
    )
}

private fun StoredGuestEnroll.toTimetableEnrollResponse(): TimetableEnrollResponse {
    return TimetableEnrollResponse(
        enrollId = enrollId,
        course = TimetableCourseResponse(
            id = course.id,
            year = course.year,
            semester = course.semester,
            courseTitle = course.courseTitle,
            source = course.source,
            timeSlots = course.timeSlots.map { slot ->
                TimetableCourseTimeSlotResponse(
                    dayOfWeek = slot.dayOfWeek,
                    startAt = slot.startAt,
                    endAt = slot.endAt
                )
            },
            courseNumber = course.courseNumber,
            lectureNumber = course.lectureNumber,
            credit = course.credit,
            instructor = course.instructor
        )
    )
}


private fun ensureNoTimeConflict(
    candidate: StoredGuestEnroll,
    existingEnrolls: List<StoredGuestEnroll>
) {
    val hasConflict = candidate.course.timeSlots.any { candidateSlot ->
        existingEnrolls.any { existingEnroll ->
            existingEnroll.course.timeSlots.any { existingSlot ->
                candidateSlot.overlaps(existingSlot)
            }
        }
    }
    require(!hasConflict) {
        "Course time conflicts with another course."
    }
}

private fun StoredGuestCourseTimeSlot.overlaps(other: StoredGuestCourseTimeSlot): Boolean {
    return dayOfWeek == other.dayOfWeek && startAt < other.endAt && other.startAt < endAt
}

private fun StoredGuestCourse.patchWith(
    request: UpdateCustomTimetableEnrollRequest
): StoredGuestCourse {
    return copy(
        courseTitle = when (val value = request.courseTitle) {
            is TimetableEnrollPatchValue.Set -> value.value.trim()
            TimetableEnrollPatchValue.Clear -> error("Course title cannot be null.")
            TimetableEnrollPatchValue.Unchanged -> courseTitle
        },
        timeSlots = when (val value = request.timeSlots) {
            is TimetableEnrollPatchValue.Set -> value.value.map { slot ->
                StoredGuestCourseTimeSlot(
                    dayOfWeek = slot.dayOfWeek,
                    startAt = slot.startAt.toStoredMinute(),
                    endAt = slot.endAt.toStoredMinute()
                )
            }
            TimetableEnrollPatchValue.Clear -> error("Time slots cannot be null.")
            TimetableEnrollPatchValue.Unchanged -> timeSlots
        },
        courseNumber = patchOptionalString(courseNumber, request.courseNumber),
        lectureNumber = patchOptionalString(lectureNumber, request.lectureNumber),
        credit = patchOptionalInt(credit, request.credit),
        instructor = patchOptionalString(instructor, request.instructor)
    )
}

private fun patchOptionalString(
    current: String?,
    patchValue: TimetableEnrollPatchValue<String>
): String? {
    return when (patchValue) {
        is TimetableEnrollPatchValue.Set -> patchValue.value
        TimetableEnrollPatchValue.Clear -> null
        TimetableEnrollPatchValue.Unchanged -> current
    }
}

private fun patchOptionalInt(
    current: Int?,
    patchValue: TimetableEnrollPatchValue<Int>
): Int? {
    return when (patchValue) {
        is TimetableEnrollPatchValue.Set -> patchValue.value
        TimetableEnrollPatchValue.Clear -> null
        TimetableEnrollPatchValue.Unchanged -> current
    }
}

private fun String.toStoredMinute(): Int {
    val parts = split(":")
    if (parts.size == 2) {
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
    return toInt()
}
