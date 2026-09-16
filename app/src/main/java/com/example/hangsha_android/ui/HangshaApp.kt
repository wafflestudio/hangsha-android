package com.example.hangsha_android.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hangsha_android.ui.components.HangshaAdaptiveLayout
import com.example.hangsha_android.ui.components.HangshaBottomBar
import com.example.hangsha_android.ui.components.HangshaNavigationRail
import com.example.hangsha_android.ui.components.LocalHangshaWindowInfo
import com.example.hangsha_android.ui.navigation.BottomTab
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
    var hasSettledInMainGraph by remember { mutableStateOf(false) }

    DisposableEffect(navBackStackEntry, isMainGraphDestination) {
        if (!isMainGraphDestination) {
            hasSettledInMainGraph = false
            onDispose { }
        } else {
            val lifecycle = navBackStackEntry?.lifecycle
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasSettledInMainGraph = true
                }
            }

            if (lifecycle?.currentState == Lifecycle.State.RESUMED) {
                hasSettledInMainGraph = true
            }
            lifecycle?.addObserver(observer)

            onDispose {
                lifecycle?.removeObserver(observer)
            }
        }
    }

    val showMainNavigation =
        isMainGraphDestination && hasSettledInMainGraph && !isOnboardingInterestPriority
    val onNavigateToDestination: (BottomTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(HangshaDestinations.Main.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    HangshaAdaptiveLayout(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = LocalHangshaWindowInfo.current.usesNavigationRail

        Row(modifier = Modifier.fillMaxSize()) {
            if (showMainNavigation && useNavigationRail) {
                HangshaNavigationRail(
                    currentDestination = currentDestination,
                    onNavigateToDestination = onNavigateToDestination
                )
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (showMainNavigation && !useNavigationRail) {
                        HangshaBottomBar(
                            currentDestination = currentDestination,
                            onNavigateToDestination = onNavigateToDestination
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
    }
}
