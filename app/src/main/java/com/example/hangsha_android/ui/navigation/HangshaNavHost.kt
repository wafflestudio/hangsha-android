package com.example.hangsha_android.ui.navigation

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.navArgument
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hangsha_android.BuildConfig
import com.example.hangsha_android.ui.components.HangshaToastType
import com.example.hangsha_android.ui.components.LocalHangshaToastState
import com.example.hangsha_android.ui.view.bookmarks.BookmarksScreen
import com.example.hangsha_android.ui.view.bookmarks.BookmarksViewModel
import com.example.hangsha_android.ui.view.login.LoginScreen
import com.example.hangsha_android.ui.view.login.LoginViewModel
import com.example.hangsha_android.ui.view.calendar.CalendarScreen
import com.example.hangsha_android.ui.view.calendar.CalendarViewModel
import com.example.hangsha_android.ui.view.eventdetail.EventDetailScreen
import com.example.hangsha_android.ui.view.eventdetail.EventDetailViewModel
import com.example.hangsha_android.ui.view.guest.LoginRequiredScreen
import com.example.hangsha_android.ui.view.interestpriority.InterestPriorityScreen
import com.example.hangsha_android.ui.view.interestpriority.InterestPriorityViewModel
import com.example.hangsha_android.ui.view.login.OpeningScreen
import com.example.hangsha_android.ui.view.mypage.MyPageScreen
import com.example.hangsha_android.ui.view.mypage.MyPageViewModel
import com.example.hangsha_android.ui.view.onboarding.OnboardingScreen
import com.example.hangsha_android.ui.view.onboarding.OnboardingViewModel
import com.example.hangsha_android.ui.view.onboarding.OnboardingWelcomeScreen
import com.example.hangsha_android.ui.view.search.SearchScreen
import com.example.hangsha_android.ui.view.search.SearchViewModel
import com.example.hangsha_android.ui.view.signup.SignUpScreen
import com.example.hangsha_android.ui.view.signup.SignUpViewModel
import com.example.hangsha_android.ui.view.splash.SplashNavigationTarget
import com.example.hangsha_android.ui.view.splash.SplashScreen
import com.example.hangsha_android.ui.view.splash.SplashViewModel
import com.example.hangsha_android.ui.view.timetable.TimetableScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.oauth.util.NidOAuthCallback

sealed class HangshaDestinations(val route: String) {
    data object Splash : HangshaDestinations("splash")
    data object Login : HangshaDestinations("login")
    data object CredentialLogin : HangshaDestinations("credential_login")
    data object SignUp : HangshaDestinations("sign_up")
    data object Onboarding : HangshaDestinations("onboarding")
    data object OnboardingWelcome : HangshaDestinations("onboarding_welcome")
    data object Main : HangshaDestinations("main")
    data object InterestPriority : HangshaDestinations("interest_priority?source={source}") {
        const val baseRoute = "interest_priority"
        const val sourceArg = "source"
        const val sourceMyPage = "mypage"
        const val sourceOnboarding = "onboarding"

        fun createRoute(source: String = sourceMyPage): String {
            return "$baseRoute?$sourceArg=$source"
        }
    }
    data object MyBookmarks : HangshaDestinations("my_bookmarks")
    data object MyMemos : HangshaDestinations("my_memos")
    data object Search : HangshaDestinations("search")
    data object EventDetail : HangshaDestinations("event_detail/{eventId}") {
        const val baseRoute = "event_detail"
        const val eventIdArg = "eventId"

        fun createRoute(eventId: Long): String = "$baseRoute/$eventId"
    }
}

@Composable
fun HangshaNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = HangshaDestinations.Splash.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        splashGraph(navController = navController)
        loginGraph(navController = navController)
        signUpGraph(navController = navController)
        onboardingGraph(navController = navController)
        mainGraph(navController = navController)
    }
}

fun NavGraphBuilder.splashGraph(navController: NavHostController) {
    composable(HangshaDestinations.Splash.route) {
        val splashViewModel: SplashViewModel = hiltViewModel()
        val splashUiState by splashViewModel.uiState.collectAsState()

        LaunchedEffect(splashUiState.navigationTarget) {
            when (splashUiState.navigationTarget) {
                SplashNavigationTarget.Calendar -> {
                    navController.navigate(HangshaDestinations.Main.route) {
                        popUpTo(HangshaDestinations.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                SplashNavigationTarget.Login -> {
                    navController.navigate(HangshaDestinations.Login.route) {
                        popUpTo(HangshaDestinations.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                null -> Unit
            }
        }

        SplashScreen()
    }
}

fun NavGraphBuilder.loginGraph(navController: NavHostController) {
    composable(HangshaDestinations.Login.route) {
        val loginViewModel: LoginViewModel = hiltViewModel()
        val loginUiState by loginViewModel.uiState.collectAsState()
        val context = LocalContext.current
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
            Log.d("AuthLog", "Google sign-in resultCode=${result.resultCode}")

            val serverAuthCode = try {
                GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                    .serverAuthCode
            } catch (error: ApiException) {
                Log.e(
                    "AuthLog",
                    "Google sign-in failed: statusCode=${error.statusCode}, message=${error.message}",
                    error
                )
                loginViewModel.onGoogleLoginError(
                    "Google 로그인에 실패했습니다. (${error.statusCode})"
                )
                return@rememberLauncherForActivityResult
            } catch (error: Exception) {
                Log.e(
                    "AuthLog",
                    "Google sign-in failed: message=${error.message}",
                    error
                )
                loginViewModel.onGoogleLoginError("Google 로그인에 실패했습니다.")
                return@rememberLauncherForActivityResult
            }

            loginViewModel.loginWithGoogle(serverAuthCode)
        }
        val kakaoLoginCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("AuthLog", "Kakao login failed: message=${error.message}", error)
                loginViewModel.onKakaoLoginError("카카오 로그인에 실패했습니다.")
            } else {
                loginViewModel.loginWithKakao(token?.accessToken)
            }
        }
        val naverLoginCallback = remember(loginViewModel) {
            object : NidOAuthCallback {
                override fun onSuccess() {
                    loginViewModel.loginWithNaver(NidOAuth.getAccessToken())
                }

                override fun onFailure(errorCode: String, errorDesc: String) {
                    Log.e(
                        "AuthLog",
                        "Naver login failed: errorCode=$errorCode, errorDesc=$errorDesc"
                    )
                    loginViewModel.onNaverLoginError("네이버 로그인에 실패했습니다.")
                }
            }
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

        OpeningScreen(
            loginUiState = loginUiState,
            onEmailLoginClick = {
                navController.navigate(HangshaDestinations.CredentialLogin.route)
            },
            onSignUpClick = {
                navController.navigate(HangshaDestinations.SignUp.route)
            },
            onGoogleLoginClick = {
                if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isBlank()) {
                    loginViewModel.onGoogleLoginConfigMissing()
                } else {
                    googleLoginLauncher.launch(googleSignInClient.signInIntent)
                }
            },
            onKakaoLoginClick = {
                // TODO(KAKAO_SETUP): Add KAKAO_NATIVE_APP_KEY and register package/key hash in Kakao Developers before release testing.
                if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
                    loginViewModel.onKakaoLoginConfigMissing()
                } else if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                    UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                        if (error != null) {
                            Log.e(
                                "AuthLog",
                                "Kakao Talk login failed: message=${error.message}",
                                error
                            )
                            if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                loginViewModel.onKakaoLoginError("카카오 로그인이 취소되었습니다.")
                                return@loginWithKakaoTalk
                            }
                            UserApiClient.instance.loginWithKakaoAccount(
                                context,
                                callback = kakaoLoginCallback
                            )
                        } else {
                            loginViewModel.loginWithKakao(token?.accessToken)
                        }
                    }
                } else {
                    UserApiClient.instance.loginWithKakaoAccount(
                        context,
                        callback = kakaoLoginCallback
                    )
                }
            },
            onNaverLoginClick = {
                if (
                    BuildConfig.NAVER_CLIENT_ID.isBlank() ||
                    BuildConfig.NAVER_CLIENT_SECRET.isBlank()
                ) {
                    loginViewModel.onNaverLoginConfigMissing()
                } else {
                    NidOAuth.requestLogin(context, naverLoginCallback)
                }
            },
            onGuestContinueClick = {
                loginViewModel.continueAsGuest()
                navController.navigate(HangshaDestinations.Main.route) {
                    popUpTo(HangshaDestinations.Login.route) { inclusive = true }
                }
            }
        )
    }

    composable(HangshaDestinations.CredentialLogin.route) {
        val loginViewModel: LoginViewModel = hiltViewModel()
        val loginUiState by loginViewModel.uiState.collectAsState()

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
            onUsernameChanged = { value -> loginViewModel.onUsernameChanged(value) },
            onPasswordChanged = { value -> loginViewModel.onPasswordChanged(value) },
            loginUiState = loginUiState
        )
    }
}

fun NavGraphBuilder.signUpGraph(navController: NavHostController) {
    composable(HangshaDestinations.SignUp.route) {
        val signUpViewModel: SignUpViewModel = hiltViewModel()
        val signUpUiState by signUpViewModel.uiState.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(signUpUiState.isSignUpSuccessful) {
            if (!signUpUiState.isSignUpSuccessful) {
                return@LaunchedEffect
            }

            navController.navigate(HangshaDestinations.Onboarding.route) {
                popUpTo(HangshaDestinations.Login.route) { inclusive = true }
            }
            signUpViewModel.onSignUpSuccessConsumed()
        }

        SignUpScreen(
            uiState = signUpUiState,
            onEmailChanged = { value -> signUpViewModel.onEmailChanged(value) },
            onPasswordChanged = { value -> signUpViewModel.onPasswordChanged(value) },
            onPasswordConfirmationChanged = { value ->
                signUpViewModel.onPasswordConfirmationChanged(value)
            },
            onPrivacyPolicyAgreementChanged = { isAgreed ->
                signUpViewModel.onPrivacyPolicyAgreementChanged(isAgreed)
            },
            onVerificationCodeChanged = { value -> signUpViewModel.onVerificationCodeChanged(value) },
            onSendVerificationCodeClick = { signUpViewModel.sendVerificationCode() },
            onVerifyVerificationCodeClick = { signUpViewModel.verifyVerificationCode() },
            onSignUpClick = { signUpViewModel.signUp() }
        )
    }
}

fun NavGraphBuilder.onboardingGraph(navController: NavHostController) {
    composable(HangshaDestinations.Onboarding.route) {
        val onboardingViewModel: OnboardingViewModel = hiltViewModel()
        val onboardingUiState by onboardingViewModel.uiState.collectAsState()
        val toastState = LocalHangshaToastState.current
        LaunchedEffect(onboardingUiState.onboardingMessage) {
            val message = onboardingUiState.onboardingMessage ?: return@LaunchedEffect
            toastState.show(message, HangshaToastType.Error)
            onboardingViewModel.onOnboardingMessageConsumed()
        }

        LaunchedEffect(onboardingUiState.isProfileSaved) {
            if (!onboardingUiState.isProfileSaved) {
                return@LaunchedEffect
            }

            navController.navigate(
                HangshaDestinations.InterestPriority.createRoute(
                    source = HangshaDestinations.InterestPriority.sourceOnboarding
                )
            )
            onboardingViewModel.onProfileSavedConsumed()
        }

        OnboardingScreen(
            uiState = onboardingUiState,
            onUsernameChanged = { value -> onboardingViewModel.onUsernameChanged(value) },
            onProfileImageSelected = { uri ->
                onboardingViewModel.onProfileImageSelected(uri)
            },
            onProfileImageDeleted = {
                onboardingViewModel.markProfileImageDeleted()
            },
            onContinueClick = { onboardingViewModel.saveProfile() }
        )
    }

    composable(HangshaDestinations.OnboardingWelcome.route) {
        OnboardingWelcomeScreen(
            onMyPageClick = {
                navController.navigate(BottomTab.MyPage.route) {
                    popUpTo(HangshaDestinations.OnboardingWelcome.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            onCalendarClick = {
                navController.navigate(HangshaDestinations.Main.route) {
                    popUpTo(HangshaDestinations.OnboardingWelcome.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
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
            val calendarViewModel: CalendarViewModel = hiltViewModel()
            val calendarUiState by calendarViewModel.uiState.collectAsState()
            val authStateViewModel: AuthStateViewModel = hiltViewModel()
            val isLoggedIn by authStateViewModel.isLoggedIn.collectAsState()
            CalendarScreen(
                uiState = calendarUiState,
                onCalendarPeriodChange = { calendarViewModel.setPeriod(it) },
                onPeriodSelected = { calendarViewModel.showPeriod(it) },
                onOpenDayCalendar = { calendarViewModel.showDayCalendar(it) },
                onViewModeChange = { calendarViewModel.setViewMode(it) },
                onSearchClick = { navController.navigate(HangshaDestinations.Search.route) },
                onDateClick = { calendarViewModel.showDayList(it) },
                onEventClick = { eventId ->
                    navController.navigate(HangshaDestinations.EventDetail.createRoute(eventId))
                },
                onBookmarkClick = { eventId ->
                    if (isLoggedIn) {
                        calendarViewModel.toggleBookmark(eventId)
                    } else {
                        navController.navigateToLoginFromMain()
                    }
                },
                showBookmarkAction = isLoggedIn,
                onOpenFilterClick = { calendarViewModel.openFilterSheet() },
                onDismissFilterSheet = { calendarViewModel.dismissFilterSheet() },
                onSelectFilterTab = { calendarViewModel.selectFilterTab(it) },
                onToggleOrgId = { calendarViewModel.toggleDraftOrgId(it) },
                onToggleStatus = { calendarViewModel.toggleDraftStatus(it) },
                onToggleEventType = { calendarViewModel.toggleDraftEventType(it) },
                onExcludeKeywordInputChange = { calendarViewModel.updateExcludeKeywordInput(it) },
                onAddExcludeKeyword = { calendarViewModel.addDraftExcludeKeyword() },
                onRemoveExcludeKeyword = { calendarViewModel.removeDraftExcludeKeyword(it) },
                onApplyFilters = { calendarViewModel.applyDraftFilters() },
                onClearFilters = { calendarViewModel.clearDraftFilters() },
                onRetryClick = { calendarViewModel.retry() }
            )
        }
        composable(HangshaDestinations.Search.route) {
            val searchViewModel: SearchViewModel = hiltViewModel()
            val searchUiState by searchViewModel.uiState.collectAsState()

            SearchScreen(
                uiState = searchUiState,
                onNavigateBack = { navController.popBackStack() },
                onInputChanged = searchViewModel::onInputChanged,
                onSearch = searchViewModel::search,
                onClear = searchViewModel::clearSearch,
                onEventClick = { eventId ->
                    navController.navigate(HangshaDestinations.EventDetail.createRoute(eventId))
                },
                onRetry = searchViewModel::retry,
                onLoadMore = searchViewModel::loadNextPage
            )
        }
        composable(
            route = HangshaDestinations.EventDetail.route,
            arguments = listOf(
                navArgument(HangshaDestinations.EventDetail.eventIdArg) {
                    type = NavType.LongType
                }
            )
        ) {
            val eventDetailViewModel: EventDetailViewModel = hiltViewModel()
            val eventDetailUiState by eventDetailViewModel.uiState.collectAsState()
            val authStateViewModel: AuthStateViewModel = hiltViewModel()
            val isLoggedIn by authStateViewModel.isLoggedIn.collectAsState()
            val toastState = LocalHangshaToastState.current

            LaunchedEffect(eventDetailUiState.memoSaveMessage) {
                val message = eventDetailUiState.memoSaveMessage ?: return@LaunchedEffect
                toastState.show(message, eventDetailUiState.memoSaveToastType)
                eventDetailViewModel.onMemoSaveMessageConsumed()
            }
            LaunchedEffect(eventDetailUiState.bugReportMessage) {
                val message = eventDetailUiState.bugReportMessage ?: return@LaunchedEffect
                toastState.show(message, eventDetailUiState.bugReportToastType)
                eventDetailViewModel.onBugReportMessageConsumed()
            }

            EventDetailScreen(
                uiState = eventDetailUiState,
                showMemberFeatures = isLoggedIn,
                onNavigateBack = { navController.popBackStack() },
                onBookmarkClick = {
                    if (isLoggedIn) {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            MyBookmarksNavigationKeys.bookmarkChangedKey,
                            true
                        )
                        eventDetailViewModel.toggleBookmark()
                    } else {
                        navController.navigateToLoginFromMain()
                    }
                },
                onMemoClick = {
                    if (isLoggedIn) {
                        eventDetailViewModel.openMemoEditor()
                    } else {
                        navController.navigateToLoginFromMain()
                    }
                },
                onMemoContentChanged = { value ->
                    eventDetailViewModel.onMemoContentChanged(value)
                },
                onMemoTagInputChanged = { value ->
                    eventDetailViewModel.onMemoTagInputChanged(value)
                },
                onAddMemoTag = { eventDetailViewModel.addMemoTag() },
                onRemoveMemoTag = { tagName -> eventDetailViewModel.removeMemoTag(tagName) },
                onSaveMemoClick = { eventDetailViewModel.saveMemo() },
                onOpenBugReport = { eventDetailViewModel.openBugReportDialog() },
                onDismissBugReport = { eventDetailViewModel.dismissBugReportDialog() },
                onBugReportTitleChanged = { value -> eventDetailViewModel.onBugReportTitleChanged(value) },
                onBugReportContentChanged = { value -> eventDetailViewModel.onBugReportContentChanged(value) },
                onSubmitBugReport = { eventDetailViewModel.submitBugReport() },
                onRetryClick = { eventDetailViewModel.retry() }
            )
        }
        composable(BottomTab.Timetable.route) {
            val authStateViewModel: AuthStateViewModel = hiltViewModel()
            val isLoggedIn by authStateViewModel.isLoggedIn.collectAsState()
            if (isLoggedIn) {
                TimetableScreen(
                    onEventClick = { eventId ->
                        navController.navigate(HangshaDestinations.EventDetail.createRoute(eventId))
                    }
                )
            } else {
                LoginRequiredScreen(
                    title = "시간표는 로그인 후 사용할 수 있습니다.",
                    message = "시간표와 수업 정보는 계정에 저장됩니다.",
                    onLoginClick = { navController.navigateToLoginFromMain() },
                    onNavigateBack = { navController.navigateToCalendarTab() }
                )
            }
        }
        composable(BottomTab.Memos.route) {
            val authStateViewModel: AuthStateViewModel = hiltViewModel()
            val isLoggedIn by authStateViewModel.isLoggedIn.collectAsState()

            if (isLoggedIn) {
                MyMemosRoute(
                    navController = navController,
                    onNavigateBack = null
                )
            } else {
                LoginRequiredScreen(
                    title = "행사 후기는 로그인 후 확인할 수 있습니다.",
                    message = "행사에 작성한 메모가 계정에 저장됩니다.",
                    onLoginClick = { navController.navigateToLoginFromMain() },
                    onNavigateBack = {
                        navController.navigate(BottomTab.Calendar.route) {
                            popUpTo(HangshaDestinations.Main.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
        composable(HangshaDestinations.MyBookmarks.route) {
            val authStateViewModel: AuthStateViewModel = hiltViewModel()
            val isLoggedIn by authStateViewModel.isLoggedIn.collectAsState()

            if (!isLoggedIn) {
                LoginRequiredScreen(
                    title = "찜한 행사는 로그인 후 확인할 수 있습니다.",
                    message = "찜한 행사는 계정에 저장됩니다.",
                    onLoginClick = { navController.navigateToLoginFromMain() },
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                val bookmarksViewModel: BookmarksViewModel = hiltViewModel()
                val bookmarksUiState by bookmarksViewModel.uiState.collectAsState()
                val myBookmarksSavedStateHandle = navController.currentBackStackEntry?.savedStateHandle
                val bookmarkChanged = myBookmarksSavedStateHandle
                    ?.get<Boolean>(MyBookmarksNavigationKeys.bookmarkChangedKey)
                val lifecycleOwner = LocalLifecycleOwner.current

                LaunchedEffect(Unit) {
                    bookmarksViewModel.refreshFromServerKeepingScroll()
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            bookmarksViewModel.refreshFromServerKeepingScroll()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(bookmarkChanged) {
                    if (bookmarkChanged != true) {
                        return@LaunchedEffect
                    }

                    bookmarksViewModel.refreshFromServerKeepingScroll()
                    myBookmarksSavedStateHandle.remove<Boolean>(
                        MyBookmarksNavigationKeys.bookmarkChangedKey
                    )
                }

                BookmarksScreen(
                    uiState = bookmarksUiState,
                    onNavigateBack = { navController.popBackStack() },
                    onEventClick = { eventId ->
                        navController.navigate(HangshaDestinations.EventDetail.createRoute(eventId))
                    },
                    onBookmarkClick = { eventId ->
                        bookmarksViewModel.removeBookmark(eventId)
                    },
                    onRetryClick = { bookmarksViewModel.loadFirstPage() },
                    onLoadNextPage = { bookmarksViewModel.loadNextPage() },
                    onScrollPositionChanged = { index, offset, itemId ->
                        bookmarksViewModel.saveScrollPosition(
                            firstVisibleItemIndex = index,
                            firstVisibleItemOffset = offset,
                            firstVisibleItemId = itemId
                        )
                    }
                )
            }
        }
        composable(HangshaDestinations.MyMemos.route) {
            val authStateViewModel: AuthStateViewModel = hiltViewModel()
            val isLoggedIn by authStateViewModel.isLoggedIn.collectAsState()

            if (isLoggedIn) {
                MyMemosRoute(
                    navController = navController,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LoginRequiredScreen(
                    title = "메모는 로그인 후 확인할 수 있습니다.",
                    message = "행사에 작성한 메모가 계정에 저장됩니다.",
                    onLoginClick = { navController.navigateToLoginFromMain() },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable(
            route = HangshaDestinations.InterestPriority.route,
            arguments = listOf(
                navArgument(HangshaDestinations.InterestPriority.sourceArg) {
                    type = NavType.StringType
                    defaultValue = HangshaDestinations.InterestPriority.sourceMyPage
                }
            )
        ) { backStackEntry ->
            val authStateViewModel: AuthStateViewModel = hiltViewModel()
            val isLoggedIn by authStateViewModel.isLoggedIn.collectAsState()

            if (!isLoggedIn) {
                LoginRequiredScreen(
                    title = "관심 우선순위는 로그인 후 설정할 수 있습니다.",
                    message = "계정에 저장되는 개인화 설정입니다.",
                    onLoginClick = { navController.navigateToLoginFromMain() },
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                val interestPriorityViewModel: InterestPriorityViewModel = hiltViewModel()
                val interestPriorityUiState by interestPriorityViewModel.uiState.collectAsState()
                val source = backStackEntry.arguments
                    ?.getString(HangshaDestinations.InterestPriority.sourceArg)
                    ?: HangshaDestinations.InterestPriority.sourceMyPage
                val isOnboardingFlow =
                    source == HangshaDestinations.InterestPriority.sourceOnboarding

                LaunchedEffect(interestPriorityUiState.isSaveSuccessful, isOnboardingFlow) {
                    if (!interestPriorityUiState.isSaveSuccessful) {
                        return@LaunchedEffect
                    }

                    if (isOnboardingFlow) {
                        navController.navigate(HangshaDestinations.OnboardingWelcome.route) {
                            popUpTo(HangshaDestinations.Onboarding.route) {
                                inclusive = true
                            }
                        }
                    } else {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            InterestPriorityNavigationKeys.updatedKey,
                            true
                        )
                        navController.popBackStack()
                    }
                    interestPriorityViewModel.onSaveSuccessConsumed()
                }

                InterestPriorityScreen(
                    uiState = interestPriorityUiState,
                    onNavigateBack = { navController.popBackStack() },
                    onCategoryClick = { categoryId ->
                        interestPriorityViewModel.toggleCategory(categoryId)
                    },
                    onRetryClick = { interestPriorityViewModel.load() },
                    onDoneClick = { interestPriorityViewModel.save() }
                )
            }
        }
        composable(BottomTab.MyPage.route) {
            val authStateViewModel: AuthStateViewModel = hiltViewModel()
            val isLoggedIn by authStateViewModel.isLoggedIn.collectAsState()

            if (!isLoggedIn) {
                LoginRequiredScreen(
                    title = "마이페이지는 로그인 후 사용할 수 있습니다.",
                    message = "계정 정보와 개인 저장 내용을 확인하려면 로그인해 주세요.",
                    onLoginClick = { navController.navigateToLoginFromMain() },
                    onNavigateBack = { navController.navigateToCalendarTab() }
                )
            } else {
                val myPageViewModel: MyPageViewModel = hiltViewModel()
                val myPageUiState by myPageViewModel.uiState.collectAsState()
                val toastState = LocalHangshaToastState.current
                val myPageSavedStateHandle = navController.currentBackStackEntry?.savedStateHandle
                val interestPriorityUpdated = myPageSavedStateHandle
                    ?.get<Boolean>(InterestPriorityNavigationKeys.updatedKey)
                val myPageLifecycleOwner = LocalLifecycleOwner.current

                LaunchedEffect(myPageUiState.profileSaveToastMessage) {
                    val message = myPageUiState.profileSaveToastMessage ?: return@LaunchedEffect
                    toastState.show(message, HangshaToastType.Error)
                    myPageViewModel.onProfileSaveToastConsumed()
                }

                LaunchedEffect(interestPriorityUpdated) {
                    if (interestPriorityUpdated != true) {
                        return@LaunchedEffect
                    }

                    myPageViewModel.loadMyProfile()
                    myPageSavedStateHandle.remove<Boolean>(InterestPriorityNavigationKeys.updatedKey)
                }

                LaunchedEffect(myPageUiState.bugReportToastMessage) {
                    val message = myPageUiState.bugReportToastMessage ?: return@LaunchedEffect
                    toastState.show(message, myPageUiState.bugReportToastType)
                    myPageViewModel.onBugReportToastConsumed()
                }

                DisposableEffect(myPageLifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            myPageViewModel.loadBookmarkedEventPreview()
                            myPageViewModel.loadMemoPreview()
                        }
                    }
                    myPageLifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        myPageLifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(myPageUiState.isLoggedOut) {
                    if (!myPageUiState.isLoggedOut) {
                        return@LaunchedEffect
                    }

                    navController.navigate(HangshaDestinations.Login.route) {
                        popUpTo(HangshaDestinations.Main.route) { inclusive = true }
                    }
                    myPageViewModel.onLogoutNavigationConsumed()
                }

                MyPageScreen(
                    uiState = myPageUiState,
                    onRetryClick = { myPageViewModel.loadMyProfile() },
                    onStartProfileEdit = { myPageViewModel.startProfileEdit() },
                    onDraftUsernameChanged = { value ->
                        myPageViewModel.onDraftUsernameChanged(value)
                    },
                    onDraftProfileImageSelected = { uri ->
                        myPageViewModel.onDraftProfileImageSelected(uri)
                    },
                    onDraftProfileImageDeleted = {
                        myPageViewModel.markDraftProfileImageDeleted()
                    },
                    onSaveProfileEdit = { myPageViewModel.saveProfileEdit() },
                    onInterestPriorityClick = {
                        navController.navigate(HangshaDestinations.InterestPriority.createRoute())
                    },
                    onTimetableClick = {
                        navController.navigate(BottomTab.Timetable.route) {
                            popUpTo(HangshaDestinations.Main.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onBookmarksClick = {
                        navController.navigate(HangshaDestinations.MyBookmarks.route)
                    },
                    onBookmarkedEventClick = { eventId ->
                        navController.navigate(HangshaDestinations.EventDetail.createRoute(eventId))
                    },
                    onMemoListClick = {
                        navController.navigate(HangshaDestinations.MyMemos.route)
                    },
                    onMemoEventClick = { eventId ->
                        navController.navigate(HangshaDestinations.EventDetail.createRoute(eventId))
                    },
                    onLogoutClick = {
                        myPageViewModel.logout()
                        navController.navigateToLoginFromMain()
                    },
                    onBugReportTitleChanged = { value ->
                        myPageViewModel.onBugReportTitleChanged(value)
                    },
                    onBugReportContentChanged = { value ->
                        myPageViewModel.onBugReportContentChanged(value)
                    },
                    onSubmitBugReportClick = { myPageViewModel.submitBugReport() },
                    onDeleteAccountClick = { myPageViewModel.deleteMyAccount() }
                )
            }
        }
    }
}


private fun NavHostController.navigateToLoginFromMain() {
    navigate(HangshaDestinations.Login.route) {
        popUpTo(HangshaDestinations.Main.route) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToSignUpFromMain() {
    navigate(HangshaDestinations.SignUp.route) {
        popUpTo(HangshaDestinations.Main.route) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToCalendarTab() {
    navigate(BottomTab.Calendar.route) {
        popUpTo(HangshaDestinations.Main.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private object InterestPriorityNavigationKeys {
    const val updatedKey = "interest_priority_updated"
}

private object MyBookmarksNavigationKeys {
    const val bookmarkChangedKey = "my_bookmarks_bookmark_changed"
}
