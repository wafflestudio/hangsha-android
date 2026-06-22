package com.example.hangsha_android.data.network.api

import com.example.hangsha_android.data.network.model.BookmarkedEventsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BookmarkApi {
    @GET("api/v1/users/me/bookmarks")
    suspend fun getMyBookmarks(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<BookmarkedEventsResponse>
}
