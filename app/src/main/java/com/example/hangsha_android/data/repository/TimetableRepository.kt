package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.network.api.TimetableApi
import com.example.hangsha_android.data.network.model.CreateCustomTimetableEnrollRequest
import com.example.hangsha_android.data.network.model.CreateCustomTimetableEnrollTimeSlotRequest
import com.example.hangsha_android.data.network.model.CreateTimetableRequest
import com.example.hangsha_android.data.network.model.TimetableEnrollListResponse
import com.example.hangsha_android.data.network.model.TimetableEnrollResponse
import com.example.hangsha_android.data.network.model.TimetableListResponse
import com.example.hangsha_android.data.network.model.TimetableResponse
import com.example.hangsha_android.data.network.model.UpdateTimetableRequest
import com.example.hangsha_android.data.network.model.UpdateCustomTimetableEnrollRequest
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

@Singleton
class TimetableRepository @Inject constructor(
    private val timetableApi: TimetableApi,
    private val authTokenStorage: AuthTokenStorage
) {
    suspend fun getTimetables(
        year: Int,
        semester: String
    ): Response<TimetableListResponse> {
        if (!isLoggedIn()) {
            return unauthorizedResponse()
        }

        return timetableApi.getTimetables(
            year = year,
            semester = semester
        )
    }

    suspend fun createTimetable(
        name: String,
        year: Int,
        semester: String
    ): Response<TimetableResponse> {
        if (!isLoggedIn()) {
            return unauthorizedResponse()
        }

        return timetableApi.createTimetable(
            request = CreateTimetableRequest(
                name = name,
                year = year,
                semester = semester
            )
        )
    }

    suspend fun updateTimetableName(
        timetableId: Long,
        name: String
    ): Response<TimetableResponse> {
        val normalizedName = name.trim()
        if (!isLoggedIn()) {
            return unauthorizedResponse()
        }

        return timetableApi.updateTimetable(
            timetableId = timetableId,
            request = UpdateTimetableRequest(name = normalizedName)
        )
    }

    suspend fun deleteTimetable(timetableId: Long): Response<Unit> {
        if (!isLoggedIn()) {
            return unauthorizedResponse()
        }

        return timetableApi.deleteTimetable(timetableId = timetableId)
    }

    suspend fun getEnrolls(timetableId: Long): Response<TimetableEnrollListResponse> {
        if (!isLoggedIn()) {
            return unauthorizedResponse()
        }

        return timetableApi.getEnrolls(timetableId = timetableId)
    }

    suspend fun getEnroll(
        timetableId: Long,
        enrollId: Long
    ): Response<TimetableEnrollResponse> {
        if (!isLoggedIn()) {
            return unauthorizedResponse()
        }

        return timetableApi.getEnroll(
            timetableId = timetableId,
            enrollId = enrollId
        )
    }

    suspend fun createCustomEnroll(
        timetableId: Long,
        year: Int,
        semester: String,
        courseTitle: String,
        timeSlots: List<CreateCustomTimetableEnrollTimeSlotRequest>,
        courseNumber: String?,
        lectureNumber: String?,
        credit: Int?,
        instructor: String?
    ): Response<TimetableEnrollResponse> {
        val request = CreateCustomTimetableEnrollRequest(
            year = year,
            semester = semester,
            courseTitle = courseTitle.trim(),
            timeSlots = timeSlots,
            courseNumber = courseNumber?.trim()?.takeIf { it.isNotEmpty() },
            lectureNumber = lectureNumber?.trim()?.takeIf { it.isNotEmpty() },
            credit = credit,
            instructor = instructor?.trim()?.takeIf { it.isNotEmpty() }
        )

        if (!isLoggedIn()) {
            return unauthorizedResponse()
        }

        return timetableApi.createCustomEnroll(
            timetableId = timetableId,
            request = request
        )
    }


    suspend fun updateCustomEnroll(
        timetableId: Long,
        enrollId: Long,
        request: UpdateCustomTimetableEnrollRequest
    ): Response<TimetableEnrollResponse> {
        if (!isLoggedIn()) {
            return unauthorizedResponse()
        }

        val requestBody = request.toJsonObject()
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE.toMediaTypeOrNull())
        return timetableApi.updateCustomEnroll(
            timetableId = timetableId,
            enrollId = enrollId,
            request = requestBody
        )
    }

    suspend fun deleteEnroll(
        timetableId: Long,
        enrollId: Long
    ): Response<Unit> {
        if (!isLoggedIn()) {
            return unauthorizedResponse()
        }

        return timetableApi.deleteEnroll(
            timetableId = timetableId,
            enrollId = enrollId
        )
    }

    private fun isLoggedIn(): Boolean {
        return authTokenStorage.hasAuthenticatedUser()
    }

    private fun <T> unauthorizedResponse(): Response<T> {
        return Response.error(401, "".toResponseBody(null))
    }
}

private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
