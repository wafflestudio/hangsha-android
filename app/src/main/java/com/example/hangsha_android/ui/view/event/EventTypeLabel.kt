package com.example.hangsha_android.ui.view.event

internal fun eventTypeLabel(eventTypeId: Long): String {
    return when (eventTypeId) {
        4L -> "\uAD50\uC721(\uD2B9\uAC15/\uC138\uBBF8\uB098)"
        5L -> "\uACF5\uBAA8\uC804/\uACBD\uC9C4\uB300\uD68C"
        6L -> "\uD604\uC7A5\uD559\uC2B5/\uC778\uD134"
        7L -> "\uC0AC\uD68C\uACF5\uD5CC(\uBD09\uC0AC)"
        8L -> "\uD559\uC2B5/\uC9C4\uB85C\uC0C1\uB2F4"
        9L -> "OpenLnL"
        10L -> "\uAE30\uD0C0"
        else -> "\uD589\uC0AC \uC720\uD615 $eventTypeId"
    }
}

