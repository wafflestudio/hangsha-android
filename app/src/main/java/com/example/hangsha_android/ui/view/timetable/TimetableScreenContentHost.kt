package com.example.hangsha_android.ui.view.timetable

import com.example.hangsha_android.util.currentHangshaDate
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hangsha_android.data.network.model.CreateCustomTimetableEnrollTimeSlotRequest
import com.example.hangsha_android.data.network.model.TimetableEnrollResponse
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite
import java.time.LocalDate

private const val GridStartMinute = 7 * 60
private const val GridEndMinute = 24 * 60
private const val DayCount = 5
private val TimeLabelWidth = 26.dp
private val HeaderHeight = 26.dp
private val GridHourHeight = 56.dp
private val GridContentHeight = GridHourHeight * ((GridEndMinute - GridStartMinute) / 60f)
private val GridLineColor = Color(0xFFE8E8E8)
private val HalfHourLineColor = Color(0xFFF1F1F1)
private val CourseMaskColor = Color(0xFFCFCFCF)
private val SnuttDisabledButtonColor = Color(0xFFCFCFCF)
private val ChangeButtonColor = Color(0xFF72D3EC)
private val AddButtonColor = Color(0xFFF08AA0)
private val EditButtonColor = Color(0xFF72D3EC)
private val DeleteButtonColor = Color(0xFFF08AA0)
private val PanelHintColor = Color(0xFFB4B4B4)
private val YearOptions = buildYearOptions()
private val SemesterOptions = listOf(
    TimetableSemesterOption("SPRING", "1\uD559\uAE30"),
    TimetableSemesterOption("SUMMER", "\uC5EC\uB984\uD559\uAE30"),
    TimetableSemesterOption("FALL", "2\uD559\uAE30"),
    TimetableSemesterOption("WINTER", "\uACA8\uC6B8\uD559\uAE30")
)
private val DefaultYear = currentHangshaDate().year
private val DefaultSemester = semesterForMonth(currentHangshaDate().monthValue).apiValue
private val WeekdayLabels = listOf("월", "화", "수", "목", "금")
private val CourseColors = listOf(
    Color(0xFF55B9DC),
    Color(0xFFEAC94D),
    Color(0xFF2D82EA),
    Color(0xFFE94061),
    Color(0xFF54C987),
    Color(0xFF8F56EC)
)
private data class TimetableSemesterOption(
    val apiValue: String,
    val label: String
)

private fun buildYearOptions(): List<Int> {
    val currentYear = currentHangshaDate().year
    return (currentYear - 2..currentYear + 2).toList()
}

private fun semesterForMonth(month: Int): TimetableSemesterOption {
    return when (month) {
        in 3..6 -> SemesterOptions[0]
        in 7..8 -> SemesterOptions[1]
        in 9..12 -> SemesterOptions[2]
        else -> SemesterOptions[3]
    }
}

private val EmptyTimetable = TimetableUiModel(
    id = "empty",
    name = "시간표 이름",
    year = DefaultYear,
    semester = DefaultSemester,
    courses = emptyList()
)

// 시간표 화면 상태 호스트: 로컬 시간표 생성, 선택, 수업 추가 상태를 관리한다.
@Composable
internal fun TimetableScreenContentHost(onEventClick: (Long) -> Unit) {
    val timetableViewModel: TimetableViewModel = hiltViewModel()
    val apiUiState by timetableViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val currentWeekStart = remember {
        val today = currentHangshaDate()
        today.minusDays((today.dayOfWeek.value - 1).toLong())
    }
    var selectedWeekOffset by rememberSaveable { mutableStateOf(0) }
    val weekStart = remember(currentWeekStart, selectedWeekOffset) { currentWeekStart.plusWeeks(selectedWeekOffset.toLong()) }
    val weekEvents = remember(apiUiState.weeklyEventSummaries, weekStart) {
        TimetableEventMapper.map(apiUiState.weeklyEventSummaries, weekStart)
    }
    var timetables by remember { mutableStateOf(emptyList<TimetableUiModel>()) }
    var selectedYear by rememberSaveable { mutableStateOf(DefaultYear) }
    var selectedSemester by rememberSaveable { mutableStateOf(DefaultSemester) }
    var selectedTimetableId by rememberSaveable { mutableStateOf<String?>(null) }
    var isEventOverlayEnabled by rememberSaveable { mutableStateOf(false) }
    var isEventTimelineExpanded by rememberSaveable { mutableStateOf(false) }
    var isTimetablePanelOpen by rememberSaveable { mutableStateOf(false) }
    var isCreateTimetablePanelOpen by rememberSaveable { mutableStateOf(false) }
    var isEditTimetablePanelOpen by rememberSaveable { mutableStateOf(false) }
    var isAddCoursePanelOpen by rememberSaveable { mutableStateOf(false) }
    var nextSlotId by rememberSaveable { mutableStateOf(2L) }
    var createTimetableName by rememberSaveable { mutableStateOf("") }
    var editTimetableId by rememberSaveable { mutableStateOf<String?>(null) }
    var editTimetableName by rememberSaveable { mutableStateOf("") }
    var courseTitle by rememberSaveable { mutableStateOf("") }
    var instructor by rememberSaveable { mutableStateOf("") }
    var creditText by rememberSaveable { mutableStateOf("") }
    var timeSlots by remember {
        mutableStateOf(listOf(defaultEditableTimeSlot(1L)))
    }
    var submitError by rememberSaveable { mutableStateOf<String?>(null) }
    var submitMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(isEventOverlayEnabled, weekStart) {
        if (isEventOverlayEnabled) {
            timetableViewModel.loadWeeklyEvents(weekStart)
        }
    }

    LaunchedEffect(selectedYear, selectedSemester) {
        selectedTimetableId = null
        timetables = emptyList()
        isTimetablePanelOpen = false
        isCreateTimetablePanelOpen = false
        isEditTimetablePanelOpen = false
        isAddCoursePanelOpen = false
        submitError = null
        submitMessage = null
        timetableViewModel.clearLoadedEnrolls()
        timetableViewModel.loadTimetables(
            year = selectedYear,
            semester = selectedSemester
        )
    }

    LaunchedEffect(apiUiState.timetables) {
        val localCoursesByTimetableId = timetables.associate { timetable ->
            timetable.id to timetable.courses
        }
        val loadedTimetables = apiUiState.timetables.map { response ->
            TimetableUiModel(
                id = response.id.toString(),
                name = response.name,
                year = response.year,
                semester = response.semester,
                courses = localCoursesByTimetableId[response.id.toString()].orEmpty()
            )
        }
        if (loadedTimetables.isNotEmpty() || timetables.isNotEmpty()) {
            timetables = loadedTimetables
            if (selectedTimetableId == null || loadedTimetables.none { timetable -> timetable.id == selectedTimetableId }) {
                selectedTimetableId = loadedTimetables.firstOrNull()?.id
            }
        }
    }

    LaunchedEffect(apiUiState.updatedTimetable) {
        val updatedTimetable = apiUiState.updatedTimetable ?: return@LaunchedEffect
        timetables = timetables.map { timetable ->
            if (timetable.id == updatedTimetable.id.toString()) {
                timetable.copy(name = updatedTimetable.name, year = updatedTimetable.year, semester = updatedTimetable.semester)
            } else {
                timetable
            }
        }
        editTimetableId = null
        editTimetableName = ""
        isTimetablePanelOpen = false
        isEditTimetablePanelOpen = false
        timetableViewModel.onUpdatedTimetableConsumed()
    }
    LaunchedEffect(apiUiState.deletedTimetableId) {
        val deletedTimetableId = apiUiState.deletedTimetableId?.toString() ?: return@LaunchedEffect
        val updatedTimetables = timetables.filterNot { timetable -> timetable.id == deletedTimetableId }
        timetables = updatedTimetables
        if (selectedTimetableId == deletedTimetableId) {
            selectedTimetableId = updatedTimetables.firstOrNull()?.id
        }
        timetableViewModel.onDeletedTimetableConsumed()
    }
    LaunchedEffect(apiUiState.createdTimetable) {
        val createdTimetable = apiUiState.createdTimetable ?: return@LaunchedEffect
        val newTimetable = TimetableUiModel(
            id = createdTimetable.id.toString(),
            name = createdTimetable.name,
            year = createdTimetable.year,
            semester = createdTimetable.semester,
            courses = emptyList()
        )
        timetables = timetables.filterNot { timetable -> timetable.id == newTimetable.id } + newTimetable
        selectedTimetableId = newTimetable.id
        createTimetableName = ""
        isTimetablePanelOpen = false
        isCreateTimetablePanelOpen = false
        isAddCoursePanelOpen = false
        timetableViewModel.onCreatedTimetableConsumed()
    }

    val selectedTimetable = timetables.firstOrNull { it.id == selectedTimetableId }
        ?: timetables.firstOrNull()
        ?: EmptyTimetable
    val hasSelectedTimetable = timetables.any { it.id == selectedTimetableId }

    LaunchedEffect(selectedTimetableId, hasSelectedTimetable) {
        val timetableId = selectedTimetableId?.toLongOrNull() ?: return@LaunchedEffect
        if (hasSelectedTimetable &&
            apiUiState.loadedEnrollsTimetableId != timetableId &&
            apiUiState.loadingEnrollsTimetableId != timetableId
        ) {
            timetableViewModel.loadEnrolls(timetableId = timetableId)
        }
    }

    LaunchedEffect(apiUiState.loadedEnrollsTimetableId, apiUiState.enrolls) {
        val timetableId = apiUiState.loadedEnrollsTimetableId?.toString() ?: return@LaunchedEffect
        timetables = timetables.map { timetable ->
            if (timetable.id == timetableId) {
                timetable.copy(courses = apiUiState.enrolls.toCourseUiModels())
            } else {
                timetable
            }
        }
    }

    LaunchedEffect(apiUiState.loadEnrollsError) {
        val message = apiUiState.loadEnrollsError ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        timetableViewModel.clearLoadEnrollsError()
    }

    LaunchedEffect(apiUiState.deletedEnrollId) {
        apiUiState.deletedEnrollId ?: return@LaunchedEffect
        Toast.makeText(
            context,
            "\uC218\uC5C5\uC744 \uC0AD\uC81C\uD588\uC2B5\uB2C8\uB2E4.",
            Toast.LENGTH_SHORT
        ).show()
        timetableViewModel.onDeletedEnrollConsumed()
    }

    LaunchedEffect(apiUiState.deleteEnrollError) {
        val message = apiUiState.deleteEnrollError ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        timetableViewModel.clearDeleteEnrollError()
    }
    val validation = TimetableAddCourseValidator.validate(
        timetableId = selectedTimetableId.takeIf { hasSelectedTimetable },
        title = courseTitle,
        creditText = creditText,
        timeSlots = timeSlots,
        existingTimeSlots = selectedTimetable.courses.toExistingEditableSlots(),
        isSubmitting = apiUiState.isCreatingCustomEnroll
    )

    fun closePanels() {
        isTimetablePanelOpen = false
        isCreateTimetablePanelOpen = false
        isEditTimetablePanelOpen = false
        isAddCoursePanelOpen = false
    }

    fun resetAddCourseForm(message: String? = null) {
        courseTitle = ""
        instructor = ""
        creditText = ""
        timeSlots = listOf(defaultEditableTimeSlot(nextSlotId++))
        submitError = null
        submitMessage = message
    }

    LaunchedEffect(apiUiState.createdCustomEnroll) {
        apiUiState.createdCustomEnroll ?: return@LaunchedEffect
        resetAddCourseForm(message = "\uC218\uC5C5\uC744 \uCD94\uAC00\uD588\uC2B5\uB2C8\uB2E4.")
        timetableViewModel.onCreatedCustomEnrollConsumed()
    }

    TimetableScreenContent(
        selectedYear = selectedYear,
        selectedSemester = selectedSemester,
        yearOptions = YearOptions,
        semesterOptions = SemesterOptions,
        selectedTimetable = selectedTimetable,
        timetables = timetables,
        weekStart = weekStart,
        events = weekEvents.timed,
        periodEvents = weekEvents.period,
        allDayEvents = weekEvents.allDay,
        isLoadingWeeklyEvents = apiUiState.isLoadingWeeklyEvents,
        loadWeeklyEventsError = apiUiState.loadWeeklyEventsError,
        isLoadingTimetables = apiUiState.isLoadingTimetables,
        loadTimetablesError = apiUiState.loadTimetablesError,
        deletingTimetableId = apiUiState.deletingTimetableId?.toString(),
        deleteTimetableError = apiUiState.deleteTimetableError,
        hasSelectedTimetable = hasSelectedTimetable,
        isEventOverlayEnabled = isEventOverlayEnabled,
        isEventTimelineExpanded = isEventTimelineExpanded,
        isTimetablePanelOpen = isTimetablePanelOpen,
        isCreateTimetablePanelOpen = isCreateTimetablePanelOpen,
        isEditTimetablePanelOpen = isEditTimetablePanelOpen,
        isAddCoursePanelOpen = isAddCoursePanelOpen,
        createTimetableName = createTimetableName,
        createTimetableError = apiUiState.createTimetableError,
        isCreatingTimetable = apiUiState.isCreatingTimetable,
        editTimetableName = editTimetableName,
        editTimetableError = apiUiState.updateTimetableError,
        isUpdatingTimetable = apiUiState.updatingTimetableId != null,
        courseTitle = courseTitle,
        instructor = instructor,
        creditText = creditText,
        timeSlots = timeSlots,
        validation = validation,
        submitError = apiUiState.createCustomEnrollError ?: submitError,
        submitMessage = submitMessage,
        isSubmitting = apiUiState.isCreatingCustomEnroll,
        deletingCourseId = apiUiState.deletingEnrollId?.toString(),
        onYearSelected = { year -> selectedYear = year },
        onSemesterSelected = { semester -> selectedSemester = semester.apiValue },
        onPreviousWeek = { selectedWeekOffset -= 1 },
        onNextWeek = { selectedWeekOffset += 1 },
        onEventOverlayChanged = { enabled ->
            isEventOverlayEnabled = enabled
            if (!enabled) isEventTimelineExpanded = false
        },
        onEventTimelineExpandedChanged = { expanded ->
            isEventTimelineExpanded = expanded
        },
        onRetryWeeklyEvents = {
            timetableViewModel.loadWeeklyEvents(weekStart)
        },
        onEventClick = onEventClick,
        onOpenTimetablePanel = {
            isEventTimelineExpanded = false
            isAddCoursePanelOpen = false
            isCreateTimetablePanelOpen = false
            isTimetablePanelOpen = true
        },
        onClosePanels = { closePanels() },
        onSelectTimetable = { timetableId ->
            selectedTimetableId = timetableId
            closePanels()
        },
                onOpenEditTimetable = { timetableId ->
            val target = timetables.firstOrNull { timetable -> timetable.id == timetableId }
            if (target != null) {
                isEventTimelineExpanded = false
                timetableViewModel.clearUpdateError()
                editTimetableId = target.id
                editTimetableName = target.name
                isTimetablePanelOpen = false
                isCreateTimetablePanelOpen = false
                isAddCoursePanelOpen = false
                isEditTimetablePanelOpen = true
            }
        },
        onDeleteTimetable = { timetableId ->
            timetableViewModel.clearDeleteError()
            timetableId.toLongOrNull()?.let { id -> timetableViewModel.deleteTimetable(id) }
        },
        onOpenCreateTimetable = {
            isEventTimelineExpanded = false
            isTimetablePanelOpen = false
            isAddCoursePanelOpen = false
            isCreateTimetablePanelOpen = true
        },
        onCreateTimetableNameChange = { createTimetableName = it },
        onEditTimetableNameChange = { editTimetableName = it },
        onSubmitCreateTimetable = {
            val normalizedName = createTimetableName.trim()
            if (normalizedName.isNotEmpty()) {
                timetableViewModel.createTimetable(
                    name = normalizedName,
                    year = selectedYear,
                    semester = selectedSemester
                )
            }
        },
        onSubmitEditTimetable = {
            val timetableId = editTimetableId?.toLongOrNull()
            val normalizedName = editTimetableName.trim()
            if (timetableId != null && normalizedName.isNotEmpty()) {
                timetableViewModel.updateTimetableName(timetableId = timetableId, name = normalizedName)
            }
        },
        onOpenAddCourse = {
            if (hasSelectedTimetable) {
                isEventTimelineExpanded = false
                isTimetablePanelOpen = false
                isCreateTimetablePanelOpen = false
                isAddCoursePanelOpen = true
                submitError = null
                submitMessage = null
            }
        },
        onTitleChange = {
            courseTitle = it
            submitError = null
            submitMessage = null
            timetableViewModel.clearCreateCustomEnrollError()
        },
        onInstructorChange = { instructor = it },
        onCreditChange = {
            creditText = it.filter { char -> char.isDigit() }
            submitError = null
            timetableViewModel.clearCreateCustomEnrollError()
        },
        onAddTimeSlot = {
            timeSlots = timeSlots + defaultEditableTimeSlot(nextSlotId++)
        },
        onRemoveTimeSlot = { localId ->
            if (timeSlots.size > 1) {
                timeSlots = timeSlots.filterNot { it.localId == localId }
            }
        },
        onChangeDay = { localId, day ->
            timeSlots = timeSlots.map { slot ->
                if (slot.localId == localId) slot.copy(dayOfWeek = day) else slot
            }
        },
        onChangeStart = { localId, minute ->
            timeSlots = timeSlots.map { slot ->
                if (slot.localId == localId) slot.copy(startAt = minute.coerceIn(0, 24 * 60 - 5)) else slot
            }
        },
        onChangeEnd = { localId, minute ->
            timeSlots = timeSlots.map { slot ->
                if (slot.localId == localId) slot.copy(endAt = minute.coerceIn(5, 24 * 60)) else slot
            }
        },
        onDeleteCourse = { courseId ->
            val timetableId = selectedTimetableId?.toLongOrNull()
            val enrollId = courseId.toLongOrNull()
            if (timetableId != null && enrollId != null) {
                timetableViewModel.clearDeleteEnrollError()
                timetableViewModel.deleteEnroll(
                    timetableId = timetableId,
                    enrollId = enrollId
                )
            }
        },
        onSubmitAddCourse = {
            val timetableId = selectedTimetableId?.toLongOrNull()
            if (!validation.canSubmit || apiUiState.isCreatingCustomEnroll || timetableId == null) {
                return@TimetableScreenContent
            }
            timetableViewModel.createCustomEnroll(
                timetableId = timetableId,
                year = selectedTimetable.year,
                semester = selectedTimetable.semester,
                courseTitle = courseTitle,
                timeSlots = timeSlots.map { slot ->
                    CreateCustomTimetableEnrollTimeSlotRequest(
                        dayOfWeek = slot.dayOfWeek.apiValue,
                        startAt = slot.startAt,
                        endAt = slot.endAt
                    )
                },
                credit = creditText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull(),
                instructor = instructor
            )
        }
    )
}

// 화면 전체 레이아웃: 그리드, 플로팅 버튼, 하단 패널들을 한 화면 안에서 겹쳐 배치한다.
@Composable
private fun TimetableScreenContent(
    selectedYear: Int,
    selectedSemester: String,
    yearOptions: List<Int>,
    semesterOptions: List<TimetableSemesterOption>,
    selectedTimetable: TimetableUiModel,
    timetables: List<TimetableUiModel>,
    weekStart: LocalDate,
    events: List<TimetableEventItem>,
    periodEvents: List<TimetableTimelineEventItem>,
    allDayEvents: List<TimetableTimelineEventItem>,
    isLoadingWeeklyEvents: Boolean,
    loadWeeklyEventsError: String?,
    isLoadingTimetables: Boolean,
    loadTimetablesError: String?,
    deletingTimetableId: String?,
    deleteTimetableError: String?,
    hasSelectedTimetable: Boolean,
    isEventOverlayEnabled: Boolean,
    isEventTimelineExpanded: Boolean,
    isTimetablePanelOpen: Boolean,
    isCreateTimetablePanelOpen: Boolean,
    isEditTimetablePanelOpen: Boolean,
    isAddCoursePanelOpen: Boolean,
    createTimetableName: String,
    createTimetableError: String?,
    isCreatingTimetable: Boolean,
    editTimetableName: String,
    editTimetableError: String?,
    isUpdatingTimetable: Boolean,
    courseTitle: String,
    instructor: String,
    creditText: String,
    timeSlots: List<EditableTimeSlot>,
    validation: AddCourseValidationResult,
    submitError: String?,
    submitMessage: String?,
    isSubmitting: Boolean,
    deletingCourseId: String?,
    onYearSelected: (Int) -> Unit,
    onSemesterSelected: (TimetableSemesterOption) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onEventOverlayChanged: (Boolean) -> Unit,
    onEventTimelineExpandedChanged: (Boolean) -> Unit,
    onRetryWeeklyEvents: () -> Unit,
    onEventClick: (Long) -> Unit,
    onOpenTimetablePanel: () -> Unit,
    onClosePanels: () -> Unit,
    onSelectTimetable: (String) -> Unit,
    onOpenEditTimetable: (String) -> Unit,
        onDeleteTimetable: (String) -> Unit,
    onOpenCreateTimetable: () -> Unit,
    onCreateTimetableNameChange: (String) -> Unit,
    onEditTimetableNameChange: (String) -> Unit,
    onSubmitCreateTimetable: () -> Unit,
    onSubmitEditTimetable: () -> Unit,
    onOpenAddCourse: () -> Unit,
    onTitleChange: (String) -> Unit,
    onInstructorChange: (String) -> Unit,
    onCreditChange: (String) -> Unit,
    onAddTimeSlot: () -> Unit,
    onRemoveTimeSlot: (Long) -> Unit,
    onChangeDay: (Long, TimetableDayOfWeek) -> Unit,
    onChangeStart: (Long, Int) -> Unit,
    onChangeEnd: (Long, Int) -> Unit,
    onSubmitAddCourse: () -> Unit,
    onDeleteCourse: (String) -> Unit
) {
    val gridScrollState = rememberScrollState()
    val hasPanelOpen = isTimetablePanelOpen || isCreateTimetablePanelOpen || isEditTimetablePanelOpen || isAddCoursePanelOpen
    BackHandler(enabled = hasPanelOpen || isEventTimelineExpanded) {
        if (isEventTimelineExpanded) {
            onEventTimelineExpandedChanged(false)
        } else {
            onClosePanels()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            TimetableTermSelector(
                selectedYear = selectedYear,
                selectedSemester = selectedSemester,
                yearOptions = yearOptions,
                semesterOptions = semesterOptions,
                onYearSelected = onYearSelected,
                onSemesterSelected = onSemesterSelected
            )
            Spacer(modifier = Modifier.height(12.dp))
            TimetableHeader(
                name = selectedTimetable.name,
                credits = selectedTimetable.totalCredits,
                weekStart = weekStart,
                isEventOverlayEnabled = isEventOverlayEnabled,
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
                onEventOverlayChanged = onEventOverlayChanged
            )
            Spacer(modifier = Modifier.height(4.dp))
            WeekdayHeader()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(gridScrollState, enabled = !isEventTimelineExpanded)
                ) {
                    WeeklyTimetableGrid(
                        courses = selectedTimetable.courses,
                        events = events,
                        showEvents = isEventOverlayEnabled,
                        deletingCourseId = deletingCourseId,
                        onDeleteCourse = onDeleteCourse,
                        onEventClick = onEventClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(GridContentHeight)
                    )
                }
                if (!isEventTimelineExpanded) {
                    TimetableFloatingActions(
                        hasSelectedTimetable = hasSelectedTimetable,
                        onChangeTimetableClick = onOpenTimetablePanel,
                        onAddCourseClick = onOpenAddCourse,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = if (isEventOverlayEnabled) 48.dp else 16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isEventOverlayEnabled && !hasPanelOpen) {
            TimetableEventTimelineSheet(
                weekStart = weekStart,
                periodEvents = periodEvents,
                allDayEvents = allDayEvents,
                expanded = isEventTimelineExpanded,
                isLoading = isLoadingWeeklyEvents,
                errorMessage = loadWeeklyEventsError,
                onExpandedChange = onEventTimelineExpandedChanged,
                onRetry = onRetryWeeklyEvents,
                onEventClick = onEventClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (isTimetablePanelOpen) {
            TimetableSelectionPanel(
                timetables = timetables,
                selectedTimetableId = selectedTimetable.id,
                isLoading = isLoadingTimetables,
                errorMessage = loadTimetablesError,
                                deletingTimetableId = deletingTimetableId,
                deleteErrorMessage = deleteTimetableError,
                onClose = onClosePanels,
                onAddTimetableClick = onOpenCreateTimetable,
                onSelectTimetable = onSelectTimetable,
                onEditTimetableClick = onOpenEditTimetable,
                onDeleteTimetableClick = onDeleteTimetable,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (isCreateTimetablePanelOpen) {
            TransparentDismissLayer(onDismiss = onClosePanels)
            CreateTimetablePanel(
                name = createTimetableName,
                errorMessage = createTimetableError,
                isCreating = isCreatingTimetable,
                onNameChange = onCreateTimetableNameChange,
                onClose = onClosePanels,
                onSubmit = onSubmitCreateTimetable,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (isEditTimetablePanelOpen) {
            TransparentDismissLayer(onDismiss = onClosePanels)
            CreateTimetablePanel(
                title = "시간표 이름 수정",
                submitText = "저장",
                name = editTimetableName,
                errorMessage = editTimetableError,
                isCreating = isUpdatingTimetable,
                onNameChange = onEditTimetableNameChange,
                onClose = onClosePanels,
                onSubmit = onSubmitEditTimetable,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        if (isAddCoursePanelOpen) {
            TransparentDismissLayer(onDismiss = onClosePanels)
            AddCoursePanel(
                courseTitle = courseTitle,
                instructor = instructor,
                creditText = creditText,
                timeSlots = timeSlots,
                validation = validation,
                submitError = submitError,
                submitMessage = submitMessage,
                isSubmitting = isSubmitting,
                onTitleChange = onTitleChange,
                onInstructorChange = onInstructorChange,
                onCreditChange = onCreditChange,
                onAddTimeSlot = onAddTimeSlot,
                onRemoveTimeSlot = onRemoveTimeSlot,
                onChangeDay = onChangeDay,
                onChangeStart = onChangeStart,
                onChangeEnd = onChangeEnd,
                onClose = onClosePanels,
                onSubmit = onSubmitAddCourse,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// 상단 헤더: 시간표 이름, 학점, 행사 보기 스위치를 표시한다.
@Composable
private fun TimetableTermSelector(
    selectedYear: Int,
    selectedSemester: String,
    yearOptions: List<Int>,
    semesterOptions: List<TimetableSemesterOption>,
    onYearSelected: (Int) -> Unit,
    onSemesterSelected: (TimetableSemesterOption) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "\uB098\uC758 \uC2DC\uAC04\uD45C",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink100,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimetableDropdown(
                text = "${selectedYear}\uD559\uB144\uB3C4",
                contentDescription = "\uD559\uB144\uB3C4 \uC120\uD0DD",
                options = yearOptions,
                optionText = { year -> "${year}\uD559\uB144\uB3C4" },
                onOptionSelected = onYearSelected
            )
            TimetableDropdown(
                text = semesterOptions.firstOrNull { option -> option.apiValue == selectedSemester }?.label.orEmpty(),
                contentDescription = "\uD559\uAE30 \uC120\uD0DD",
                options = semesterOptions,
                optionText = { semester -> semester.label },
                onOptionSelected = onSemesterSelected
            )
        }
    }
}

@Composable
private fun <T> TimetableDropdown(
    text: String,
    contentDescription: String,
    options: List<T>,
    optionText: (T) -> String,
    onOptionSelected: (T) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .semantics { this.contentDescription = contentDescription },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = Ink60,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionText(option),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        expanded = false
                        onOptionSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun TimetableHeader(
    name: String,
    credits: Int,
    weekStart: LocalDate,
    isEventOverlayEnabled: Boolean,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onEventOverlayChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink100,
            fontSize = 17.sp,
            modifier = Modifier.widthIn(max = 78.dp),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "(${credits}학점)",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(14.dp))
        Switch(
            checked = isEventOverlayEnabled,
            onCheckedChange = onEventOverlayChanged,
            modifier = Modifier
                .size(width = 42.dp, height = 24.dp)
                .semantics {
                    contentDescription = "행사 보기"
                    stateDescription = if (isEventOverlayEnabled) "켜짐" else "꺼짐"
                },
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureWhite,
                checkedTrackColor = ChangeButtonColor,
                uncheckedThumbColor = PureWhite,
                uncheckedTrackColor = Color(0xFFD0D0D0),
                uncheckedBorderColor = Color.Transparent,
                checkedBorderColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        TimetableWeekNavigator(
            weekStart = weekStart,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek
        )
    }
}

@Composable
private fun TimetableWeekNavigator(
    weekStart: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val weekEnd = weekStart.plusDays(7)
    Row(
        modifier = Modifier.height(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousWeek,
            modifier = Modifier
                .size(24.dp)
                .semantics { contentDescription = "이전 주" }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = null,
                tint = Ink60,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = formatWeekRange(weekStart, weekEnd),
            modifier = Modifier
                .width(68.dp)
                .semantics {
                    contentDescription = "${weekStart.year}년 ${weekStart.monthValue}월 ${weekStart.dayOfMonth}일부터 ${weekEnd.monthValue}월 ${weekEnd.dayOfMonth}일까지"
                },
            color = Ink100,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = onNextWeek,
            modifier = Modifier
                .size(24.dp)
                .semantics { contentDescription = "다음 주" }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Ink60,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatWeekRange(start: LocalDate, end: LocalDate): String {
    return "${start.monthValue}/${start.dayOfMonth}~${end.monthValue}/${end.dayOfMonth}"
}

// 요일 헤더: 시간 라벨 영역을 제외한 월~금 열 제목을 그린다.
@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(TimeLabelWidth))
        WeekdayLabels.forEach { label ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink100,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 주간 그리드: 시간 좌표 계산 결과에 따라 수업/행사 레이어를 순서대로 올린다.
@Composable
private fun WeeklyTimetableGrid(
    courses: List<CourseUiModel>,
    events: List<TimetableEventItem>,
    showEvents: Boolean,
    deletingCourseId: String?,
    onDeleteCourse: (String) -> Unit,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val courseBlocks = remember(courses) { courses.toCourseBlocks() }
    val coursePositions = remember(courseBlocks) {
        TimetableLayoutCalculator.positionBlocks(
            blocks = courseBlocks.map { block ->
                TimetableBlock(
                    id = block.id,
                    weekday = block.weekday,
                    startMinute = block.startMinute,
                    endMinute = block.endMinute
                )
            },
            gridStartMinute = GridStartMinute,
            gridEndMinute = GridEndMinute
        ).associateBy { it.id }
    }
    val eventPositions = remember(events) {
        TimetableLayoutCalculator.positionBlocks(
            blocks = events.map { event ->
                TimetableBlock(
                    id = event.id,
                    weekday = event.weekday,
                    startMinute = event.startMinute,
                    endMinute = event.endMinute
                )
            },
            gridStartMinute = GridStartMinute,
            gridEndMinute = GridEndMinute,
            splitOverlaps = true
        ).associateBy { it.id }
    }

    BoxWithConstraints(modifier = modifier) {
        val gridWidth = maxWidth - TimeLabelWidth
        val dayWidth = gridWidth / DayCount
        val gridHeight = maxHeight

        GridLines()
        HourLabels(gridHeight = gridHeight)

        if (showEvents) {
            courseBlocks.forEach { block ->
                val position = coursePositions[block.id] ?: return@forEach
                TimetableBlockBackground(position, dayWidth, gridHeight, CourseMaskColor, 0.72f)
            }
            courseBlocks.forEach { block ->
                val position = coursePositions[block.id] ?: return@forEach
                TimetableBlockLabel(
                    position = position,
                    dayWidth = dayWidth,
                    gridHeight = gridHeight,
                    text = listOfNotNull(block.title, block.subtitle).joinToString("\n"),
                    textColor = PureWhite.copy(alpha = 0.58f),
                    onClick = null
                )
            }
            events.forEach { event ->
                val position = eventPositions[event.id] ?: return@forEach
                TimetableBlockBackground(position, dayWidth, gridHeight, event.categoryColor, 0.7f)
            }
            events.forEach { event ->
                val position = eventPositions[event.id] ?: return@forEach
                TimetableBlockLabel(
                    position = position,
                    dayWidth = dayWidth,
                    gridHeight = gridHeight,
                    text = event.title,
                    textColor = PureWhite,
                    onClick = { onEventClick(event.eventId) }
                )
            }
        } else {
            courseBlocks.forEach { block ->
                val position = coursePositions[block.id] ?: return@forEach
                CourseBlock(
                    position = position,
                    dayWidth = dayWidth,
                    gridHeight = gridHeight,
                    color = block.color,
                    title = block.title,
                    subtitle = block.subtitle,
                    isDeleting = deletingCourseId == block.courseId,
                    onDelete = { onDeleteCourse(block.courseId) }
                )
            }
        }
    }
}

// 그리드 배경: 30분 단위 가로선을 그리고 시간 라벨 영역은 비워둔다.
@Composable
private fun GridLines() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val labelWidthPx = TimeLabelWidth.toPx()
        val minuteHeight = size.height / (GridEndMinute - GridStartMinute)
        for (minute in GridStartMinute..GridEndMinute step 30) {
            val y = (minute - GridStartMinute) * minuteHeight
            val color = if (minute % 60 == 0) GridLineColor else HalfHourLineColor
            drawLine(color = color, start = Offset(labelWidthPx, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
    }
}

// 시간 축: 그리드 높이에 맞춰 9시부터 6시까지의 라벨 위치를 계산한다.
@Composable
private fun HourLabels(gridHeight: Dp) {
    val gridDuration = GridEndMinute - GridStartMinute
    for (minute in GridStartMinute until GridEndMinute step 60) {
        val top = gridHeight * ((minute - GridStartMinute).toFloat() / gridDuration)
        Text(
            text = hourLabel(minute),
            modifier = Modifier.offset(x = 0.dp, y = top - 8.dp).width(TimeLabelWidth),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8D8D8D),
            fontSize = 11.sp,
            textAlign = TextAlign.Start
        )
    }
}

// 수업 블록: 토글 OFF 상태에서 과목 색상과 텍스트를 중앙 정렬로 표시한다.
@Composable
private fun CourseBlock(
    position: PositionedTimetableBlock,
    dayWidth: Dp,
    gridHeight: Dp,
    color: Color,
    title: String,
    subtitle: String?,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    Box(
        modifier = blockModifier(position, dayWidth, gridHeight)
            .clip(RoundedCornerShape(0.dp))
            .background(color)
    ) {
        Text(
            text = listOfNotNull(title, subtitle).joinToString("\n"),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 3.dp, vertical = 4.dp)
                .wrapContentSize(Alignment.Center),
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = onDelete,
            enabled = !isDeleting,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
        ) {
            if (isDeleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = DeleteButtonColor,
                    strokeWidth = 1.5.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "$title \uC218\uC5C5 \uC0AD\uC81C",
                    tint = DeleteButtonColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
@Composable
private fun TimetableBlockBackground(position: PositionedTimetableBlock, dayWidth: Dp, gridHeight: Dp, color: Color, alpha: Float) {
    Box(modifier = blockModifier(position, dayWidth, gridHeight).background(color.copy(alpha = alpha)))
}

@Composable
private fun TimetableBlockLabel(position: PositionedTimetableBlock, dayWidth: Dp, gridHeight: Dp, text: String, textColor: Color, onClick: (() -> Unit)?) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = blockModifier(position, dayWidth, gridHeight)
            .then(clickModifier)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = textColor, fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold, maxLines = 5, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TimetableFloatingActions(hasSelectedTimetable: Boolean, onChangeTimetableClick: () -> Unit, onAddCourseClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        TimetablePillButton("SNUTT 연동", SnuttDisabledButtonColor, false, {}, "SNUTT 연동, 비활성")
        TimetablePillButton("시간표 바꾸기", ChangeButtonColor, true, onChangeTimetableClick, "시간표 바꾸기")
        TimetablePillButton("수업 추가", AddButtonColor, hasSelectedTimetable, onAddCourseClick, "수업 추가") {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TimetableSelectionPanel(
    timetables: List<TimetableUiModel>,
    selectedTimetableId: String,
    isLoading: Boolean,
    errorMessage: String?,
        deletingTimetableId: String?,
    deleteErrorMessage: String?,
    onClose: () -> Unit,
    onAddTimetableClick: () -> Unit,
    onSelectTimetable: (String) -> Unit,
    onEditTimetableClick: (String) -> Unit,
    onDeleteTimetableClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth().heightIn(min = 250.dp, max = 292.dp), shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp), color = PureWhite, shadowElevation = 8.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp)) {
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp).semantics { contentDescription = "시간표 변경 패널 닫기" }) {
                Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = PanelHintColor)
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("나의 시간표", style = MaterialTheme.typography.bodyMedium, color = Ink100, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onAddTimetableClick, modifier = Modifier.size(32.dp).semantics { contentDescription = "시간표 추가" }) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = Ink60, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = "스누티티 연동하기",
                style = MaterialTheme.typography.bodyMedium,
                color = PanelHintColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "스누티티 연동하기, 비활성"
                    stateDescription = "비활성"
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (deleteErrorMessage != null) {
                FieldErrorText(deleteErrorMessage)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isLoading) {
                LoadingTimetablePanelState(modifier = Modifier.fillMaxWidth())
            } else if (errorMessage != null) {
                TimetablePanelErrorState(message = errorMessage, modifier = Modifier.fillMaxWidth())
            } else if (timetables.isEmpty()) {
                EmptyTimetablePanelState(modifier = Modifier.fillMaxWidth())
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(items = timetables, key = { it.id }) { timetable ->
                        TimetableSelectionRow(
                            timetable = timetable,
                            isSelected = timetable.id == selectedTimetableId,
                            isDeleting = timetable.id == deletingTimetableId,
                            onSelect = { onSelectTimetable(timetable.id) },
                            onEdit = { onEditTimetableClick(timetable.id) },
                            onDelete = { onDeleteTimetableClick(timetable.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateTimetablePanel(title: String = "시간표 만들기", submitText: String = "만들기", name: String, errorMessage: String?, isCreating: Boolean, onNameChange: (String) -> Unit, onClose: () -> Unit, onSubmit: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp), color = PureWhite, shadowElevation = 8.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp)) {
            PanelHeader(title = title, onClose = onClose)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("시간표 이름") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(14.dp))
            Button(onClick = onSubmit, enabled = name.trim().isNotEmpty() && !isCreating, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ChangeButtonColor, contentColor = PureWhite)) {
                Text(if (isCreating) "저장 중" else submitText)
            }
        }
    }
}

@Composable
private fun AddCoursePanel(
    courseTitle: String,
    instructor: String,
    creditText: String,
    timeSlots: List<EditableTimeSlot>,
    validation: AddCourseValidationResult,
    submitError: String?,
    submitMessage: String?,
    isSubmitting: Boolean,
    onTitleChange: (String) -> Unit,
    onInstructorChange: (String) -> Unit,
    onCreditChange: (String) -> Unit,
    onAddTimeSlot: () -> Unit,
    onRemoveTimeSlot: (Long) -> Unit,
    onChangeDay: (Long, TimetableDayOfWeek) -> Unit,
    onChangeStart: (Long, Int) -> Unit,
    onChangeEnd: (Long, Int) -> Unit,
    onClose: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth().heightIn(max = 560.dp), shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp), color = PureWhite, shadowElevation = 8.dp) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { PanelHeader(title = "수업 추가", onClose = onClose) }
            item {
                OutlinedTextField(value = courseTitle, onValueChange = onTitleChange, label = { Text("과목명") }, isError = validation.titleError != null, supportingText = validation.titleError?.let { message -> { Text(message) } }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = instructor, onValueChange = onInstructorChange, label = { Text("교수명") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = creditText, onValueChange = onCreditChange, label = { Text("학점") }, isError = validation.creditError != null, supportingText = validation.creditError?.let { message -> { Text(message) } }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(104.dp))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("수업 시간", style = MaterialTheme.typography.bodyMedium, color = Ink100, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onAddTimeSlot) { Text("+ 시간 추가") }
                }
            }
            items(items = timeSlots, key = { it.localId }) { slot ->
                TimeSlotEditorRow(
                    slot = slot,
                    canRemove = timeSlots.size > 1,
                    errors = validation.timeSlotErrors.filter { it.localId == slot.localId }.map { it.message },
                    onRemove = { onRemoveTimeSlot(slot.localId) },
                    onChangeDay = { day -> onChangeDay(slot.localId, day) },
                    onChangeStart = { minute -> onChangeStart(slot.localId, minute) },
                    onChangeEnd = { minute -> onChangeEnd(slot.localId, minute) }
                )
            }
            val globalErrors = validation.timeSlotErrors.filter { it.localId == -1L }
            if (globalErrors.isNotEmpty()) {
                item { FieldErrorText(globalErrors.joinToString("\n") { it.message }) }
            }
            if (submitError != null) {
                item { FieldErrorText(submitError) }
            }
            if (submitMessage != null) {
                item { Text(submitMessage, color = ChangeButtonColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
            item {
                Button(onClick = onSubmit, enabled = validation.canSubmit, modifier = Modifier.fillMaxWidth().height(42.dp), colors = ButtonDefaults.buttonColors(containerColor = AddButtonColor, contentColor = PureWhite)) {
                    Text(if (isSubmitting) "저장 중" else "저장")
                }
            }
        }
    }
}

@Composable
private fun TimeSlotEditorRow(
    slot: EditableTimeSlot,
    canRemove: Boolean,
    errors: List<String>,
    onRemove: () -> Unit,
    onChangeDay: (TimetableDayOfWeek) -> Unit,
    onChangeStart: (Int) -> Unit,
    onChangeEnd: (Int) -> Unit
) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF8F8F8)) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TimetableDayOfWeek.values().take(5).forEach { day ->
                    DayChip(day = day, selected = slot.dayOfWeek == day, onClick = { onChangeDay(day) })
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove, enabled = canRemove, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "시간 삭제", tint = if (canRemove) Ink60 else PanelHintColor)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DirectTimePicker(label = "시작", minute = slot.startAt, onChange = onChangeStart, modifier = Modifier.weight(1f))
                DirectTimePicker(label = "종료", minute = slot.endAt, onChange = onChangeEnd, modifier = Modifier.weight(1f))
            }
            errors.forEach { FieldErrorText(it) }
        }
    }
}

@Composable
private fun DayChip(day: TimetableDayOfWeek, selected: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) ChangeButtonColor else Color(0xFFE9E9E9), contentColor = if (selected) PureWhite else Ink60), contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(28.dp)) {
        Text(day.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DirectTimePicker(
    label: String,
    minute: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val safeMinute = minute.coerceIn(0, 24 * 60 - 5)

    OutlinedButton(
        onClick = {
            TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->
                    onChange(snapToFiveMinutes(selectedHour * 60 + selectedMinute))
                },
                safeMinute / 60,
                safeMinute % 60,
                true
            ).apply {
                setTitle("$label \uC2DC\uAC04 \uC120\uD0DD")
            }.show()
        },
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = Ink60,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatMinute(safeMinute),
                    color = Ink100,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = "$label \uC2DC\uAC04 \uC120\uD0DD",
                    tint = Ink60,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PanelHeader(title: String, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = Ink100, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(imageVector = Icons.Rounded.KeyboardArrowDown, contentDescription = "닫기", tint = PanelHintColor)
        }
    }
}

@Composable
private fun TransparentDismissLayer(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss))
}

@Composable
private fun TimetableSelectionRow(timetable: TimetableUiModel, isSelected: Boolean, isDeleting: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).semantics { contentDescription = "${timetable.name} 선택" }, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(timetable.name, style = MaterialTheme.typography.bodyMedium, color = Ink100, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${timetable.totalCredits}학점", style = MaterialTheme.typography.bodyMedium, color = Ink60, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        SmallPanelButton(text = "수정", color = EditButtonColor, onClick = onEdit)
        Spacer(modifier = Modifier.width(8.dp))
        SmallPanelButton(text = if (isDeleting) "삭제 중" else "삭제", color = DeleteButtonColor, enabled = !isDeleting, onClick = onDelete)
    }
}

@Composable
private fun EmptyTimetablePanelState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(88.dp), contentAlignment = Alignment.CenterStart) {
        Text("등록된 시간표가 없습니다.", style = MaterialTheme.typography.bodyMedium, color = Ink60, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SmallPanelButton(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = PureWhite), contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.height(28.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimetablePillButton(text: String, color: Color, enabled: Boolean, onClick: () -> Unit, contentDescription: String, icon: (@Composable () -> Unit)? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = PureWhite, disabledContainerColor = color, disabledContentColor = Color(0xFFFAFAFA).copy(alpha = 0.72f)),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.height(32.dp).semantics { this.contentDescription = contentDescription }
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(2.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FieldErrorText(message: String) {
    Text(message, color = DeleteButtonColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
}

private fun blockModifier(position: PositionedTimetableBlock, dayWidth: Dp, gridHeight: Dp): Modifier {
    val laneWidth = dayWidth / position.laneCount
    val x = TimeLabelWidth + (dayWidth * position.weekday) + (laneWidth * position.laneIndex)
    val y = gridHeight * position.topFraction
    val height = gridHeight * position.heightFraction
    return Modifier.offset(x = x + 1.dp, y = y).width(laneWidth - 2.dp).height(height)
}

private data class CourseBlockUiModel(
    val id: String,
    val courseId: String,
    val title: String,
    val subtitle: String?,
    val color: Color,
    val weekday: Int,
    val startMinute: Int,
    val endMinute: Int
)

private fun List<CourseUiModel>.toCourseBlocks(): List<CourseBlockUiModel> {
    return flatMap { course ->
        course.timeSlots.mapIndexed { index, slot ->
            CourseBlockUiModel(
                id = "${course.id}-$index",
                courseId = course.id,
                title = course.title,
                subtitle = course.subtitle,
                color = course.color,
                weekday = slot.weekday,
                startMinute = slot.startMinute,
                endMinute = slot.endMinute
            )
        }
    }
}

private fun List<TimetableEnrollResponse>.toCourseUiModels(): List<CourseUiModel> {
    return mapIndexed { index, enroll ->
        CourseUiModel(
            id = enroll.enrollId.toString(),
            title = enroll.course.courseTitle,
            instructor = enroll.course.instructor,
            credit = enroll.course.credit,
            color = CourseColors[index.floorMod(CourseColors.size)],
            timeSlots = enroll.course.timeSlots.mapNotNull { slot ->
                val weekday = slot.dayOfWeek.toTimetableWeekday() ?: return@mapNotNull null
                CourseTimeSlot(
                    weekday = weekday,
                    startMinute = slot.startAt,
                    endMinute = slot.endAt
                )
            }
        )
    }
}

private fun String.toTimetableWeekday(): Int? {
    return TimetableDayOfWeek.values().firstOrNull { day -> day.apiValue == this }?.weekday
}

private fun List<CourseUiModel>.toExistingEditableSlots(): List<EditableTimeSlot> {
    var localId = 1L
    return flatMap { course ->
        course.timeSlots.mapNotNull { slot ->
            val day = TimetableDayOfWeek.values().firstOrNull { it.weekday == slot.weekday } ?: return@mapNotNull null
            EditableTimeSlot(localId++, day, slot.startMinute, slot.endMinute)
        }
    }
}

private fun defaultEditableTimeSlot(localId: Long): EditableTimeSlot {
    return EditableTimeSlot(localId, TimetableDayOfWeek.MON, 8 * 60, 11 * 60)
}

private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor

private fun hourLabel(minute: Int): String {
    val hour = minute / 60
    return when {
        hour == 12 -> "12"
        hour > 12 -> (hour - 12).toString()
        else -> hour.toString()
    }
}

private fun snapToFiveMinutes(minute: Int): Int {
    val boundedMinute = minute.coerceIn(0, 24 * 60 - 1)
    return (((boundedMinute + 2) / 5) * 5).coerceAtMost(24 * 60 - 5)
}

private fun formatMinute(minute: Int): String {
    val safeMinute = minute.coerceIn(0, 24 * 60)
    val hour = safeMinute / 60
    val minuteOfHour = safeMinute % 60
    return "%02d:%02d".format(hour, minuteOfHour)
}
@Composable
private fun LoadingTimetablePanelState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(88.dp), contentAlignment = Alignment.CenterStart) {
        Text("시간표 목록을 불러오는 중입니다.", style = MaterialTheme.typography.bodyMedium, color = Ink60, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimetablePanelErrorState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(88.dp), contentAlignment = Alignment.CenterStart) {
        FieldErrorText(message)
    }
}
