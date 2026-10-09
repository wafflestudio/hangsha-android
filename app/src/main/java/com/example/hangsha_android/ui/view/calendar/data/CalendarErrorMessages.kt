package com.example.hangsha_android.ui.view.calendar.data

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

internal fun calendarLoadErrorMessage(error: Throwable): String {
    return when (error) {
        is UnknownHostException -> "인터넷 연결을 확인해 주세요."
        is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
        is HttpException -> when (error.code()) {
            400 -> "행사 요청이 올바르지 않습니다."
            401 -> "로그인이 필요합니다."
            403 -> "행사 목록을 볼 권한이 없습니다."
            404 -> "행사 정보를 찾을 수 없습니다."
            in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
            else -> "행사 목록을 불러오지 못했습니다. (${error.code()})"
        }
        is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
        is IllegalStateException -> "행사 목록을 불러오지 못했습니다."
        else -> "행사 목록을 불러오지 못했습니다."
    }
}

internal fun calendarBookmarkErrorMessage(error: Throwable): String {
    return when (error) {
        is UnknownHostException -> "인터넷 연결을 확인해 주세요."
        is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
        is HttpException -> when (error.code()) {
            400 -> "북마크 요청이 올바르지 않습니다."
            401 -> "로그인이 필요합니다."
            403 -> "이 북마크를 변경할 권한이 없습니다."
            404 -> "행사 정보를 찾을 수 없습니다."
            in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
            else -> "북마크를 변경하지 못했습니다. (${error.code()})"
        }
        is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
        else -> "북마크를 변경하지 못했습니다."
    }
}

internal fun calendarExcludedKeywordErrorMessage(error: Throwable): String {
    return when (error) {
        is UnknownHostException -> "인터넷 연결을 확인해 주세요."
        is SocketTimeoutException -> "요청 시간이 초과되었습니다. 다시 시도해 주세요."
        is HttpException -> when (error.code()) {
            400 -> "제외 키워드 요청이 올바르지 않습니다."
            401 -> "로그인이 필요합니다."
            403 -> "제외 키워드를 변경할 권한이 없습니다."
            404 -> "제외 키워드 정보를 찾을 수 없습니다."
            in 500..599 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
            else -> "제외 키워드를 변경하지 못했습니다. (${error.code()})"
        }
        is IOException -> "네트워크 오류가 발생했습니다. 다시 시도해 주세요."
        is IllegalStateException -> "제외 키워드를 변경하지 못했습니다."
        else -> "제외 키워드를 변경하지 못했습니다."
    }
}
