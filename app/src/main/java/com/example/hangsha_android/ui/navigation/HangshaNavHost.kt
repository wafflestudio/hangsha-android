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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.navArgument
import com.example.hangsha_android.BuildConfig
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
import com.example.hangsha_android.ui.view.mypage.MyPageScreen
import com.example.hangsha_android.ui.view.mypage.MyPageViewModel
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

            EventDetailScreen(
                uiState = eventDetailUiState,
                onNavigateBack = { navController.popBackStack() },
                onBookmarkClick = { eventDetailViewModel.toggleBookmark() },
                onRetryClick = { eventDetailViewModel.retry() }
            )
        }
        composable(BottomTab.Timetable.route) {
            SimplePageText("timetable")
        }
        composable(BottomTab.Bookmarks.route) {
            SimplePageText("bookmark events")
        }
        composable(BottomTab.MyPage.route) {
            val myPageViewModel: MyPageViewModel = hiltViewModel()
            val myPageUiState by myPageViewModel.uiState.collectAsState()
            val context = LocalContext.current

            LaunchedEffect(myPageUiState.profileSaveToastMessage) {
                val message = myPageUiState.profileSaveToastMessage ?: return@LaunchedEffect
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                myPageViewModel.onProfileSaveToastConsumed()
            }

            LaunchedEffect(myPageUiState.bugReportToastMessage) {
                val message = myPageUiState.bugReportToastMessage ?: return@LaunchedEffect
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                myPageViewModel.onBugReportToastConsumed()
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
                onLogoutClick = { myPageViewModel.logout() },
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

// 그냥 임시로 페이지 만들어주는 거.
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

private object CalendarFilterNavigationKeys {
    const val bookmarkedOnlyKey = "calendar_bookmarked_only"
    const val interestedOnlyKey = "calendar_interested_only"
    const val orgIdsKey = "calendar_org_ids"
    const val statusIdsKey = "calendar_status_ids"
    const val eventTypeIdsKey = "calendar_event_type_ids"
    const val excludedKeywordsKey = "calendar_excluded_keywords"
    const val hasAppliedServerFiltersKey = "calendar_has_applied_server_filters"
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
