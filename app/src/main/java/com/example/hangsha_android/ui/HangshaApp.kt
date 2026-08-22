package com.example.hangsha_android.ui

import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hangsha_android.ui.components.HangshaBottomBar
import com.example.hangsha_android.ui.navigation.HangshaDestinations
import com.example.hangsha_android.ui.navigation.HangshaNavHost

@Composable
fun HangshaApp() {
    val bootstrapViewModel = hiltViewModel<AppBootstrapViewModel>()
    val catalogErrorMessage by bootstrapViewModel.catalogErrorMessage.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(catalogErrorMessage) {
        catalogErrorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            bootstrapViewModel.consumeCatalogError()
        }
    }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isOnboardingInterestPriority =
        currentDestination?.route == HangshaDestinations.InterestPriority.route &&
            navBackStackEntry?.arguments
                ?.getString(HangshaDestinations.InterestPriority.sourceArg) ==
            HangshaDestinations.InterestPriority.sourceOnboarding
    val isMainGraphDestination =
        currentDestination?.hierarchy?.any { it.route == HangshaDestinations.Main.route } == true
    val showBottomBar = isMainGraphDestination && !isOnboardingInterestPriority

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
