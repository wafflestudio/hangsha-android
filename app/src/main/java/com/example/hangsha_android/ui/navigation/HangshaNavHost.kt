package com.example.hangsha_android.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun HangshaNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = BottomTab.Calendar.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(BottomTab.Calendar.route) {
            SimplePageText("calendar")
        }
        composable(BottomTab.Timetable.route) {
            SimplePageText("timetable")
        }
        composable(BottomTab.Bookmarks.route) {
            SimplePageText("bookmark events")
        }
        composable(BottomTab.MyPage.route) {
            SimplePageText("my page")
        }
    }
}

@Composable
fun SimplePageText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
