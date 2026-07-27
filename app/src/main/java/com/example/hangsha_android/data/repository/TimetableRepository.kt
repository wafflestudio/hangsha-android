package com.example.hangsha_android.data.repository

import com.example.hangsha_android.data.local.AuthTokenStorage
import com.example.hangsha_android.data.local.GuestTimetableLocalDataSource
import com.example.hangsha_android.data.network.api.TimetableApi
import com.example.hangsha_android.data.network.model.CreateTimetableRequest
import com.example.hangsha_android.data.network.model.TimetableListResponse
import com.example.hangsha_android.data.network.model.TimetableResponse
import com.example.hangsha_android.data.network.model.UpdateTimetableRequest
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

@Singleton
class TimetableRepository @Inject constructor(
    private val timetableApi: TimetableApi,
    private val localDataSource: GuestTimetableLocalDataSource,
    private val authTokenStorage: AuthTokenStorage
) {
    suspend fun getTimetables(
        year: Int,
        semester: String
    ): Response<TimetableListResponse> {
        if (!isLoggedIn()) {
            return Response.success(
                localDataSource.getTimetables(
                    year = year,
                    semester = semester
                )
            )
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
            return Response.success(
                localDataSource.createTimetable(
                    name = name,
                    year = year,
                    semester = semester
                )
            )
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
            return Response.success(
                localDataSource.updateTimetableName(
                    timetableId = timetableId,
                    name = normalizedName
                )
            )
        }

        return timetableApi.updateTimetable(
            timetableId = timetableId,
            request = UpdateTimetableRequest(name = normalizedName)
        )
    }
    suspend fun deleteTimetable(timetableId: Long): Response<Unit> {
        if (!isLoggedIn()) {
            localDataSource.deleteTimetable(timetableId)
            return Response.success(Unit)
        }

        return timetableApi.deleteTimetable(timetableId = timetableId)
    }
    private fun isLoggedIn(): Boolean {
        return authTokenStorage.hasAccessToken()
    }
}