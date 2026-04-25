package com.example.hangsha_android.data.repository.model

import java.time.LocalDate

data class EventDateRange(
    val from: LocalDate,
    val to: LocalDate
) {
    init {
        require(!to.isBefore(from)) {
            "EventDateRange.to must be the same as or after EventDateRange.from."
        }
    }
}
