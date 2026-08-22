package com.example.hangsha_android.util

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId

val HANGSHA_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

fun currentHangshaDate(): LocalDate {
    return LocalDate.now(HANGSHA_ZONE_ID)
}

fun currentHangshaMonth(): YearMonth {
    return YearMonth.now(HANGSHA_ZONE_ID)
}

fun OffsetDateTime.toHangshaDate(): LocalDate {
    return atZoneSameInstant(HANGSHA_ZONE_ID).toLocalDate()
}
