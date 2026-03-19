package com.example.hangsha_android.ui.navigation

import androidx.annotation.DrawableRes
import com.example.hangsha_android.R

enum class BottomTab(
    val route: String,
    val label: String,
    @DrawableRes val iconResId: Int
) {
    Calendar("calendar", "캘린더", R.drawable.ic_bottombar_calender),
    Timetable("timetable", "시간표", R.drawable.ic_bottombar_timetable),
    Bookmarks("bookmarks", "북마크", R.drawable.ic_bottombar_bookmark),
    MyPage("mypage", "마이", R.drawable.ic_bottombar_mypage)
}