package com.example.hangsha_android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Calendar("calendar", "캘린더", Icons.Filled.CalendarMonth),
    Timetable("timetable", "시간표", Icons.AutoMirrored.Filled.List),
    Bookmarks("bookmarks", "북마크", Icons.Filled.Bookmarks),
    MyPage("mypage", "마이", Icons.Filled.Person)
}