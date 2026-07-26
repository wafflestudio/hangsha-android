package com.example.hangsha_android.ui.navigation

import android.util.Log
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.example.hangsha_android.ui.view.bookmarks.BookmarksScreen
import com.example.hangsha_android.ui.view.bookmarks.BookmarksViewModel
import com.example.hangsha_android.ui.view.calendar.CalendarFilterState
import com.example.hangsha_android.ui.view.login.LoginScreen
import com.example.hangsha_android.ui.view.login.LoginViewModel
import com.example.hangsha_android.ui.view.calendar.CalendarScreen
import com.example.hangsha_android.ui.view.calendar.CalendarViewModel
import com.example.hangsha_android.ui.view.dailyevents.DailyEventsFilterState
import com.example.hangsha_android.ui.view.dailyevents.DailyEventsScreen
import com.example.hangsha_android.ui.view.dailyevents.DailyEventsViewModel
import com.example.hangsha_android.ui.view.eventdetail.EventDetailScreen
import com.example.hangsha_android.ui.view.eventdetail.EventDetailViewModel
import com.example.hangsha_android.ui.view.mypage.GuestMyPageScreen
import com.example.hangsha_android.ui.view.guest.GuestMyPageViewModel
import com.example.hangsha_android.ui.view.guest.LoginRequiredScreen
import com.example.hangsha_android.ui.view.interestpriority.InterestPriorityScreen
import com.example.hangsha_android.ui.view.interestpriority.InterestPriorityViewModel
import com.example.hangsha_android.ui.view.login.OpeningScreen
import com.example.hangsha_android.ui.view.mypage.MyPageScreen
import com.example.hangsha_android.ui.view.mypage.MyPageViewModel
import com.example.hangsha_android.ui.view.mymemos.MyMemosScreen
import com.example.hangsha_android.ui.view.mymemos.MyMemosViewModel
import com.example.hangsha_android.ui.view.onboarding.OnboardingScreen
import com.example.hangsha_android.ui.view.onboarding.OnboardingViewModel
import com.example.hangsha_android.ui.view.onboarding.OnboardingWelcomeScreen
import com.example.hangsha_android.ui.view.signup.SignUpScreen
import com.example.hangsha_android.ui.view.signup.SignUpViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

sealed class HangshaDestinations(val route: String) {
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
    data object DailyEvents : HangshaDestinations("daily_events/{date}") {
        const val baseRoute = "daily_events"
        const val dateArg = "date"
        const val bookmarkedOnlyKey = "daily_events_bookmarked_only"
        const val interestedOnlyKey = "daily_events_interested_only"
        const val orgIdsKey = "daily_events_org_ids"
        const val statusIdsKey = "daily_events_status_ids"
        const val eventTypeIdsKey = "daily_events_event_type_ids"
        const val excludedKeywordsKey = "daily_events_excluded_keywords"
        const val hasAppliedServerFiltersKey = "daily_events_has_applied_server_filters"

        fun createRoute(date: String): String = "$baseRoute/$date"
    }
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
        startDestination = HangshaDestinations.Login.route,
        modifier = Modifier.padding(innerPadding)
    ) {
        loginGraph(navController = navController)
        signUpGraph(navController = navController)
        onboardingGraph(navController = navController)
        mainGraph(navController = navController)
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
                    "Google sign-in failed with status ${error.statusCode}"
                )
                return@rememberLauncherForActivityResult
            } catch (error: Exception) {
                Log.e(
                    "AuthLog",
                    "Google sign-in failed: message=${error.message}",
                    error
                )
                loginViewModel.onGoogleLoginError(error.message ?: "Google sign-in failed.")
                return@rememberLauncherForActivityResult
            }

            loginViewModel.loginWithGoogle(serverAuthCode)
        }
        val kakaoLoginCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("AuthLog", "Kakao login failed: message=${error.message}", error)
                loginViewModel.onKakaoLoginError(error.message ?: "Kakao login failed.")
            } else {
                loginViewModel.loginWithKakao(token?.accessToken)
            }
        }

        LaunchedEffect(Unit) {
            loginViewModel.tryAutoLogin()
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
                                loginViewModel.onKakaoLoginError("Kakao login was cancelled.")
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
                // TODO(NAVER_LOGIN): Decide how to handle Naver client secret before enabling the native SDK login flow.
                loginViewModel.onNaverLoginConfigMissing()
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

        LaunchedEffect(signUpUiState.signUpMessage) {
            val message = signUpUiState.signUpMessage ?: return@LaunchedEffect
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            signUpViewModel.onSignUpMessageConsumed()
        }

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
            onSignUpClick = { signUpViewModel.signUp() }
        )
    }
}

fun NavGraphBuilder.onboardingGraph(navController: NavHostController) {
    composable(HangshaDestinations.Onboarding.route) {
        val onboardingViewModel: OnboardingViewModel = hiltViewModel()
        val onboardingUiState by onboardingViewModel.uiState.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(onboardingUiState.onboardingMessage) {
            val message = onboardingUiState.onboardingMessage ?: return@LaunchedEffect
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
            val calendarSavedStateHandle = navController.currentBackStackEntry?.savedStateHandle
            val returnedCalendarFilters = calendarSavedStateHandle?.toCalendarFilterState()
            val returnedCalendarHasAppliedServerFilters: Boolean? = calendarSavedStateHandle
                ?.get<Boolean>(CalendarFilterNavigationKeys.hasAppliedServerFiltersKey)
                ?: returnedCalendarFilters?.hasActiveFilters

            LaunchedEffect(returnedCalendarFilters, returnedCalendarHasAppliedServerFilters) {
                val filters = returnedCalendarFilters ?: return@LaunchedEffect
                calendarViewModel.restoreAppliedFilters(
                    filters = filters,
                    hasAppliedServerFilters = returnedCalendarHasAppliedServerFilters
                        ?: filters.hasActiveFilters
                )
                calendarSavedStateHandle?.clearCalendarFilters()
            }

            CalendarScreen(
                uiState = calendarUiState,
                onPreviousMonthClick = { calendarViewModel.showPreviousMonth() },
                onNextMonthClick = { calendarViewModel.showNextMonth() },
                onDateClick = { date ->
                    navController.currentBackStackEntry?.savedStateHandle?.apply {
                        setDailyEventsFilters(
                            filters = calendarUiState.appliedFilters,
                            hasAppliedServerFilters = calendarUiState.hasAppliedServerFilters
                        )
                    }
                    navController.navigate(HangshaDestinations.DailyEvents.createRoute(date.toString()))
                },
                onOpenFilterClick = { calendarViewModel.openFilterSheet() },
                onDismissFilterSheet = { calendarViewModel.dismissFilterSheet() },
                onSelectFilterTab = { calendarViewModel.selectFilterTab(it) },
                onBookmarkedOnlyChange = { calendarViewModel.setDraftBookmarkedOnly(it) },
                onInterestedOnlyChange = { calendarViewModel.setDraftInterestedOnly(it) },
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
        composable(
            route = HangshaDestinations.DailyEvents.route,
            arguments = listOf(
                navArgument(HangshaDestinations.DailyEvents.dateArg) {
                    type = NavType.StringType
                }
            )
        ) {
            val dailyEventsViewModel: DailyEventsViewModel = hiltViewModel()
            val dailyEventsUiState by dailyEventsViewModel.uiState.collectAsState()
            val previousSavedStateHandle = navController.previousBackStackEntry?.savedStateHandle
            val initialDailyFilters = previousSavedStateHandle?.toDailyEventsFilterState()
            val initialHasAppliedServerFilters = previousSavedStateHandle
                ?.get<Boolean>(HangshaDestinations.DailyEvents.hasAppliedServerFiltersKey)
                ?: initialDailyFilters?.hasActiveFilters

            LaunchedEffect(initialDailyFilters, initialHasAppliedServerFilters) {
                dailyEventsViewModel.initialize(
                    filters = initialDailyFilters,
                    hasAppliedServerFilters = initialHasAppliedServerFilters
                )
            }

            LaunchedEffect(
                dailyEventsUiState.appliedFilters,
                dailyEventsUiState.hasAppliedServerFilters
            ) {
                previousSavedStateHandle?.setCalendarFilters(
                    filters = dailyEventsUiState.appliedFilters.toCalendarFilterState(),
                    hasAppliedServerFilters = dailyEventsUiState.hasAppliedServerFilters
                )
            }

            DailyEventsScreen(
                uiState = dailyEventsUiState,
                onPreviousDayClick = { dailyEventsViewModel.showPreviousDay() },
                onNextDayClick = { dailyEventsViewModel.showNextDay() },
                onOpenFilterClick = { dailyEventsViewModel.openFilterSheet() },
                onDismissFilterSheet = { dailyEventsViewModel.dismissFilterSheet() },
                onSelectFilterTab = { dailyEventsViewModel.selectFilterTab(it) },
                onBookmarkedOnlyChange = { dailyEventsViewModel.setDraftBookmarkedOnly(it) },
                onInterestedOnlyChange = { dailyEventsViewModel.setDraftInterestedOnly(it) },
                onToggleOrgId = { dailyEventsViewModel.toggleDraftOrgId(it) },
                onToggleStatus = { dailyEventsViewModel.toggleDraftStatus(it) },
                onToggleEventType = { dailyEventsViewModel.toggleDraftEventType(it) },
                onExcludeKeywordInputChange = {
                    dailyEventsViewModel.updateExcludeKeywordInput(it)
                },
                onAddExcludeKeyword = { dailyEventsViewModel.addDraftExcludeKeyword() },
                onRemoveExcludeKeyword = {
                    dailyEventsViewModel.removeDraftExcludeKeyword(it)
                },
                onApplyFilters = { dailyEventsViewModel.applyDraftFilters() },
                onClearFilters = { dailyEventsViewModel.clearDraftFilters() },
                onRetryClick = { dailyEventsViewModel.retry() },
                onEventClick = { eventId ->
                    navController.navigate(HangshaDestinations.EventDetail.createRoute(eventId))
                },
                onBookmarkClick = { eventId -> dailyEventsViewModel.toggleBookmark(eventId) }
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
            val context = LocalContext.current

            LaunchedEffect(eventDetailUiState.memoSaveMessage) {
                val message = eventDetailUiState.memoSaveMessage ?: return@LaunchedEffect
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                eventDetailViewModel.onMemoSaveMessageConsumed()
            }

            EventDetailScreen(
                uiState = eventDetailUiState,
                onNavigateBack = { navController.popBackStack() },
                onBookmarkClick = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        MyBookmarksNavigationKeys.bookmarkChangedKey,
                        true
                    )
                    eventDetailViewModel.toggleBookmark()
                },
                onMemoClick = { eventDetailViewModel.openMemoEditor() },
                onMemoContentChanged = { value ->
                    eventDetailViewModel.onMemoContentChanged(value)
                },
                onMemoTagInputChanged = { value ->
                    eventDetailViewModel.onMemoTagInputChanged(value)
                },
                onAddMemoTag = { eventDetailViewModel.addMemoTag() },
                onRemoveMemoTag = { tagName -> eventDetailViewModel.removeMemoTag(tagName) },
                onSaveMemoClick = { eventDetailViewModel.saveMemo() },
                onRetryClick = { eventDetailViewModel.retry() }
            )
        }
        composable(BottomTab.Timetable.route) {
            SimplePageText("timetable")
        }
        composable(BottomTab.Bookmarks.route) {
            val authStateViewModel: AuthStateViewModel = hiltViewModel()
            val isLoggedIn by authStateViewModel.isLoggedIn.collectAsState()

            if (isLoggedIn) {
                SimplePageText("bookmark events")
            } else {
                LoginRequiredScreen(
                    title = "북마크 목록은 로그인 후 이용할 수 있습니다.",
                    message = "게스트로 추가한 북마크는 캘린더 안에서 이 기기에만 저장됩니다.",
                    onLoginClick = { navController.navigateToLoginFromMain() }
                )
            }
        }
        composable(HangshaDestinations.MyBookmarks.route) {
            val authStateViewModel: AuthStateViewModel = hiltViewModel()
            val isLoggedIn by authStateViewModel.isLoggedIn.collectAsState()

            if (!isLoggedIn) {
                LoginRequiredScreen(
                    title = "내 북마크는 로그인 후 이용할 수 있습니다.",
                    message = "계정에 저장된 북마크 목록을 보려면 로그인해 주세요.",
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

            if (!isLoggedIn) {
                LoginRequiredScreen(
                    title = "내 메모는 로그인 후 이용할 수 있습니다.",
                    message = "게스트 메모는 행사 상세 화면에서 이 기기에만 보관됩니다.",
                    onLoginClick = { navController.navigateToLoginFromMain() },
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                val myMemosViewModel: MyMemosViewModel = hiltViewModel()
                val myMemosUiState by myMemosViewModel.uiState.collectAsState()
                val context = LocalContext.current
                val myMemosLifecycleOwner = LocalLifecycleOwner.current

                LaunchedEffect(myMemosUiState.toastMessage) {
                    val message = myMemosUiState.toastMessage ?: return@LaunchedEffect
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    myMemosViewModel.onToastMessageConsumed()
                }

                DisposableEffect(myMemosLifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            myMemosViewModel.loadMemos()
                        }
                    }
                    myMemosLifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        myMemosLifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                MyMemosScreen(
                    uiState = myMemosUiState,
                    onNavigateBack = { navController.popBackStack() },
                    onMemoClick = { eventId ->
                        navController.navigate(HangshaDestinations.EventDetail.createRoute(eventId))
                    },
                    onDeleteMemoClick = { memoId -> myMemosViewModel.deleteMemo(memoId) },
                    onStartEditMemo = { memo -> myMemosViewModel.startEditMemo(memo) },
                    onEditContentChanged = { value -> myMemosViewModel.onEditContentChanged(value) },
                    onStartAddingTag = { myMemosViewModel.startAddingTag() },
                    onEditTagInputChanged = { value -> myMemosViewModel.onEditTagInputChanged(value) },
                    onAddEditTag = { myMemosViewModel.addEditTag() },
                    onRemoveEditTag = { tagName -> myMemosViewModel.removeEditTag(tagName) },
                    onSaveEditedMemo = { myMemosViewModel.saveEditedMemo() },
                    onRetryClick = { myMemosViewModel.loadMemos() }
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
                val guestMyPageViewModel: GuestMyPageViewModel = hiltViewModel()
                val guestMyPageUiState by guestMyPageViewModel.uiState.collectAsState()
                val context = LocalContext.current

                LaunchedEffect(guestMyPageUiState.bugReportToastMessage) {
                    val message = guestMyPageUiState.bugReportToastMessage ?: return@LaunchedEffect
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    guestMyPageViewModel.onBugReportToastConsumed()
                }

                GuestMyPageScreen(
                    uiState = guestMyPageUiState,
                    onLoginClick = { navController.navigateToLoginFromMain() },
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
                    onBugReportTitleChanged = { value ->
                        guestMyPageViewModel.onBugReportTitleChanged(value)
                    },
                    onBugReportContentChanged = { value ->
                        guestMyPageViewModel.onBugReportContentChanged(value)
                    },
                    onSubmitBugReportClick = {
                        guestMyPageViewModel.submitBugReport()
                    }
                )
            } else {
                val myPageViewModel: MyPageViewModel = hiltViewModel()
                val myPageUiState by myPageViewModel.uiState.collectAsState()
                val context = LocalContext.current
                val myPageSavedStateHandle = navController.currentBackStackEntry?.savedStateHandle
                val interestPriorityUpdated = myPageSavedStateHandle
                    ?.get<Boolean>(InterestPriorityNavigationKeys.updatedKey)
                val myPageLifecycleOwner = LocalLifecycleOwner.current

                LaunchedEffect(myPageUiState.profileSaveToastMessage) {
                    val message = myPageUiState.profileSaveToastMessage ?: return@LaunchedEffect
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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

// Temporary placeholder page.
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
private object CalendarFilterNavigationKeys {
    const val bookmarkedOnlyKey = "calendar_bookmarked_only"
    const val interestedOnlyKey = "calendar_interested_only"
    const val orgIdsKey = "calendar_org_ids"
    const val statusIdsKey = "calendar_status_ids"
    const val eventTypeIdsKey = "calendar_event_type_ids"
    const val excludedKeywordsKey = "calendar_excluded_keywords"
    const val hasAppliedServerFiltersKey = "calendar_has_applied_server_filters"
}

private object InterestPriorityNavigationKeys {
    const val updatedKey = "interest_priority_updated"
}

private object MyBookmarksNavigationKeys {
    const val bookmarkChangedKey = "my_bookmarks_bookmark_changed"
}

private fun androidx.lifecycle.SavedStateHandle.setDailyEventsFilters(
    filters: CalendarFilterState,
    hasAppliedServerFilters: Boolean
) {
    set(HangshaDestinations.DailyEvents.bookmarkedOnlyKey, filters.bookmarkedOnly)
    set(HangshaDestinations.DailyEvents.interestedOnlyKey, filters.interestedOnly)
    set(HangshaDestinations.DailyEvents.orgIdsKey, ArrayList(filters.orgIds))
    set(HangshaDestinations.DailyEvents.statusIdsKey, ArrayList(filters.statusIds))
    set(HangshaDestinations.DailyEvents.eventTypeIdsKey, ArrayList(filters.eventTypeIds))
    set(HangshaDestinations.DailyEvents.excludedKeywordsKey, ArrayList(filters.excludedKeywords))
    set(
        HangshaDestinations.DailyEvents.hasAppliedServerFiltersKey,
        hasAppliedServerFilters
    )
}

private fun androidx.lifecycle.SavedStateHandle.setCalendarFilters(
    filters: CalendarFilterState,
    hasAppliedServerFilters: Boolean
) {
    set(CalendarFilterNavigationKeys.bookmarkedOnlyKey, filters.bookmarkedOnly)
    set(CalendarFilterNavigationKeys.interestedOnlyKey, filters.interestedOnly)
    set(CalendarFilterNavigationKeys.orgIdsKey, ArrayList(filters.orgIds))
    set(CalendarFilterNavigationKeys.statusIdsKey, ArrayList(filters.statusIds))
    set(CalendarFilterNavigationKeys.eventTypeIdsKey, ArrayList(filters.eventTypeIds))
    set(CalendarFilterNavigationKeys.excludedKeywordsKey, ArrayList(filters.excludedKeywords))
    set(CalendarFilterNavigationKeys.hasAppliedServerFiltersKey, hasAppliedServerFilters)
}

private fun androidx.lifecycle.SavedStateHandle.toDailyEventsFilterState(): DailyEventsFilterState? {
    if (!contains(HangshaDestinations.DailyEvents.statusIdsKey)) {
        return null
    }

    return DailyEventsFilterState(
        bookmarkedOnly = get<Boolean>(HangshaDestinations.DailyEvents.bookmarkedOnlyKey) ?: false,
        interestedOnly = get<Boolean>(HangshaDestinations.DailyEvents.interestedOnlyKey) ?: false,
        orgIds = get<ArrayList<Long>>(HangshaDestinations.DailyEvents.orgIdsKey)?.toSet()
            ?: emptySet(),
        statusIds = get<ArrayList<Long>>(HangshaDestinations.DailyEvents.statusIdsKey)?.toSet()
            ?: emptySet(),
        eventTypeIds = get<ArrayList<Long>>(HangshaDestinations.DailyEvents.eventTypeIdsKey)?.toSet()
            ?: emptySet(),
        excludedKeywords = get<ArrayList<String>>(HangshaDestinations.DailyEvents.excludedKeywordsKey)
            ?.toList()
            ?: emptyList()
    )
}

private fun androidx.lifecycle.SavedStateHandle.toCalendarFilterState(): CalendarFilterState? {
    if (!contains(CalendarFilterNavigationKeys.statusIdsKey)) {
        return null
    }

    return CalendarFilterState(
        bookmarkedOnly = get<Boolean>(CalendarFilterNavigationKeys.bookmarkedOnlyKey) ?: false,
        interestedOnly = get<Boolean>(CalendarFilterNavigationKeys.interestedOnlyKey) ?: false,
        orgIds = get<ArrayList<Long>>(CalendarFilterNavigationKeys.orgIdsKey)?.toSet()
            ?: emptySet(),
        statusIds = get<ArrayList<Long>>(CalendarFilterNavigationKeys.statusIdsKey)?.toSet()
            ?: emptySet(),
        eventTypeIds = get<ArrayList<Long>>(CalendarFilterNavigationKeys.eventTypeIdsKey)?.toSet()
            ?: emptySet(),
        excludedKeywords = get<ArrayList<String>>(CalendarFilterNavigationKeys.excludedKeywordsKey)
            ?.toList()
            ?: emptyList()
    )
}

private fun androidx.lifecycle.SavedStateHandle.clearCalendarFilters() {
    remove<Boolean>(CalendarFilterNavigationKeys.bookmarkedOnlyKey)
    remove<Boolean>(CalendarFilterNavigationKeys.interestedOnlyKey)
    remove<ArrayList<Long>>(CalendarFilterNavigationKeys.orgIdsKey)
    remove<ArrayList<Long>>(CalendarFilterNavigationKeys.statusIdsKey)
    remove<ArrayList<Long>>(CalendarFilterNavigationKeys.eventTypeIdsKey)
    remove<ArrayList<String>>(CalendarFilterNavigationKeys.excludedKeywordsKey)
    remove<Boolean>(CalendarFilterNavigationKeys.hasAppliedServerFiltersKey)
}

private fun DailyEventsFilterState.toCalendarFilterState(): CalendarFilterState {
    return CalendarFilterState(
        bookmarkedOnly = bookmarkedOnly,
        interestedOnly = interestedOnly,
        orgIds = orgIds,
        statusIds = statusIds,
        eventTypeIds = eventTypeIds,
        excludedKeywords = excludedKeywords
    )
}
