package com.example.hangsha_android.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.navigation
import com.example.hangsha_android.ui.view.login.LoginScreen
import com.example.hangsha_android.ui.view.login.LoginViewModel
import com.example.hangsha_android.ui.view.serverhealth.ServerHealthViewModel

sealed class HangshaDestinations(val route: String) {
    data object Login : HangshaDestinations("login")
    data object Main : HangshaDestinations("main")
}

@Composable
fun HangshaNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = HangshaDestinations.Login.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        loginGraph(navController = navController)
        mainGraph(navController = navController)
    }
}

fun NavGraphBuilder.loginGraph(navController: NavHostController) {
    composable(HangshaDestinations.Login.route) {
        val loginViewModel: LoginViewModel = hiltViewModel()
        val loginUiState by loginViewModel.uiState.collectAsState()
        val serverHealthViewModel: ServerHealthViewModel = hiltViewModel()
        val serverHealthUiState by serverHealthViewModel.uiState.collectAsState()
        LoginScreen(
            onLoginClick = {
                navController.navigate(HangshaDestinations.Main.route) {
                    popUpTo(HangshaDestinations.Login.route) { inclusive = true }
                }
            },
            onGoogleLoginClick = loginViewModel::onGoogleLoginClick,
            onCheckServerClick = serverHealthViewModel::checkServer,
            loginUiState = loginUiState,
            serverHealthUiState = serverHealthUiState
        )
    }
}

fun NavGraphBuilder.mainGraph(navController: NavHostController) {
    navigation(
        startDestination = BottomTab.Calendar.route,
        route = HangshaDestinations.Main.route
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
