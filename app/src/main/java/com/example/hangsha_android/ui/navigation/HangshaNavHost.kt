package com.example.hangsha_android.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.hangsha_android.BuildConfig
import com.example.hangsha_android.ui.view.login.LoginScreen
import com.example.hangsha_android.ui.view.login.LoginViewModel
import com.example.hangsha_android.ui.view.serverhealth.ServerHealthViewModel
import com.example.hangsha_android.ui.view.signup.SignUpScreen
import com.example.hangsha_android.ui.view.signup.SignUpViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class HangshaDestinations(val route: String) {
    data object Login : HangshaDestinations("login")
    data object SignUp : HangshaDestinations("sign_up")
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
        signUpGraph(navController = navController)
        mainGraph(navController = navController)
    }
}

fun NavGraphBuilder.loginGraph(navController: NavHostController) {
    composable(HangshaDestinations.Login.route) {
        val loginViewModel: LoginViewModel = hiltViewModel()
        val loginUiState by loginViewModel.uiState.collectAsState()
        val serverHealthViewModel: ServerHealthViewModel = hiltViewModel()
        val serverHealthUiState by serverHealthViewModel.uiState.collectAsState()
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val googleSignInOptions = remember(BuildConfig.GOOGLE_SERVER_CLIENT_ID) {
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestServerAuthCode(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                .build()
        }
        val googleSignInClient = remember(context, googleSignInOptions) {
            GoogleSignIn.getClient(context, googleSignInOptions)
        }
        val googleLoginLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                loginViewModel.onGoogleLoginCancelled()
                return@rememberLauncherForActivityResult
            }

            val serverAuthCode = runCatching {
                GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                    .serverAuthCode
            }.getOrElse { error ->
                val message = if (error is ApiException) {
                    "Google sign-in failed with status ${error.statusCode}"
                } else {
                    error.message ?: "Google sign-in failed."
                }
                loginViewModel.onGoogleLoginError(message)
                return@rememberLauncherForActivityResult
            }

            loginViewModel.loginWithGoogle(serverAuthCode)
        }

        LaunchedEffect(loginUiState.isLoginSuccessful) {
            if (!loginUiState.isLoginSuccessful) {
                return@LaunchedEffect
            }

            navController.navigate(HangshaDestinations.Main.route) {
                popUpTo(HangshaDestinations.Login.route) { inclusive = true }
            }
            loginViewModel.onLoginSuccessConsumed()
        }

        LoginScreen(
            onLoginClick = { loginViewModel.loginWithCredentials() },
            onSignUpClick = {
                navController.navigate(HangshaDestinations.SignUp.route)
            },
            onUsernameChanged = { value -> loginViewModel.onUsernameChanged(value) },
            onPasswordChanged = { value -> loginViewModel.onPasswordChanged(value) },
            onGoogleLoginClick = {
                if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isBlank()) {
                    loginViewModel.onGoogleLoginConfigMissing()
                } else {
                    googleLoginLauncher.launch(googleSignInClient.signInIntent)
                }
            },
            onClearGoogleLoginHistoryClick = {
                loginViewModel.onGoogleHistoryClearStarted()
                coroutineScope.launch {
                    runCatching {
                        googleSignInClient.signOut().await()
                    }.fold(
                        onSuccess = {
                            loginViewModel.onGoogleHistoryCleared()
                        },
                        onFailure = { error ->
                            loginViewModel.onGoogleLoginError(
                                error.message ?: "Failed to clear Google login history."
                            )
                        }
                    )
                }
            },
            onCheckServerClick = { serverHealthViewModel.checkServer() },
            loginUiState = loginUiState,
            serverHealthUiState = serverHealthUiState
        )
    }
}

fun NavGraphBuilder.signUpGraph(navController: NavHostController) {
    composable(HangshaDestinations.SignUp.route) {
        val signUpViewModel: SignUpViewModel = hiltViewModel()
        val signUpUiState by signUpViewModel.uiState.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(signUpUiState.signUpMessage) {
            val message = signUpUiState.signUpMessage ?: return@LaunchedEffect
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            signUpViewModel.onSignUpMessageConsumed()
        }

        LaunchedEffect(signUpUiState.isSignUpSuccessful) {
            if (!signUpUiState.isSignUpSuccessful) {
                return@LaunchedEffect
            }

            navController.popBackStack()
            signUpViewModel.onSignUpSuccessConsumed()
        }

        SignUpScreen(
            uiState = signUpUiState,
            onEmailChanged = { value -> signUpViewModel.onEmailChanged(value) },
            onPasswordChanged = { value -> signUpViewModel.onPasswordChanged(value) },
            onPasswordConfirmationChanged = { value ->
                signUpViewModel.onPasswordConfirmationChanged(value)
            },
            onSignUpClick = { signUpViewModel.signUp() },
            onNavigateBack = {
                navController.popBackStack()
            }
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
