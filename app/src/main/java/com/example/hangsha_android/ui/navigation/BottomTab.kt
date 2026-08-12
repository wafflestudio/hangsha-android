package com.example.hangsha_android.ui.navigation

import androidx.annotation.DrawableRes
import com.example.hangsha_android.R

enum class BottomTab(
    val route: String,
    val label: String,
    @DrawableRes val iconResId: Int,
    @DrawableRes val activeIconResId: Int
) {
    Calendar(
        "calendar",
        "캘린더",
        R.drawable.ic_bottombar_calender,
        R.drawable.ic_bottombar_calender_active
    ),
    Timetable(
        "timetable",
        "시간표",
        R.drawable.ic_bottombar_timetable,
        R.drawable.ic_bottombar_timetable_active
    ),
    Memos(
        "memos",
        "\uD589\uC0AC \uD6C4\uAE30",
        R.drawable.ic_bottombar_reviews,
        R.drawable.ic_bottombar_reviews_active
    ),
    MyPage(
        "mypage",
        "마이",
        R.drawable.ic_bottombar_mypage,
        R.drawable.ic_bottombar_mypage_active
    )
}
