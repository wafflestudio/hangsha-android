package com.example.hangsha_android.ui.view.event

internal fun eventTypeLabel(eventTypeId: Long): String {
    return when (eventTypeId) {
        1L -> "교육(특강/세미나)"
        2L -> "공모전/경진대회"
        3L -> "현장학습/인턴"
        4L -> "사회공헌(봉사)"
        5L -> "학습/진로상담"
        6L -> "OpenLnL"
        7L -> "기타"
        else -> "행사 유형 " + eventTypeId
    }
}
