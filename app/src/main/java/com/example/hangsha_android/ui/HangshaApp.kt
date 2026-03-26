package com.example.hangsha_android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hangsha_android.ui.components.HangshaBottomBar
import com.example.hangsha_android.ui.navigation.HangshaDestinations
import com.example.hangsha_android.ui.navigation.HangshaNavHost

@Composable
fun HangshaApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar =
        currentDestination?.hierarchy?.any { it.route == HangshaDestinations.Main.route } == true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                HangshaBottomBar(
                    currentDestination = currentDestination,
                    onNavigateToDestination = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(HangshaDestinations.Main.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        HangshaNavHost(
            navController = navController,
            innerPadding = innerPadding
        )
    }
}