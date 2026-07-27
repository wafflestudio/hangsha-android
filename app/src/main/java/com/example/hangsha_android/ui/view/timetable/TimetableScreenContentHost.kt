package com.example.hangsha_android.ui.view.timetable

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite

private const val GridStartMinute = 9 * 60
private const val GridEndMinute = 18 * 60
private const val DayCount = 5
private const val DefaultYear = 2026
private const val DefaultSemester = "FALL"
private val TimeLabelWidth = 26.dp
private val HeaderHeight = 26.dp
private val GridLineColor = Color(0xFFE8E8E8)
private val HalfHourLineColor = Color(0xFFF1F1F1)
private val CourseMaskColor = Color(0xFFCFCFCF)
private val SnuttDisabledButtonColor = Color(0xFFCFCFCF)
private val ChangeButtonColor = Color(0xFF72D3EC)
private val AddButtonColor = Color(0xFFF08AA0)
private val EditButtonColor = Color(0xFF72D3EC)
private val DeleteButtonColor = Color(0xFFF08AA0)
private val PanelHintColor = Color(0xFFB4B4B4)
private val WeekdayLabels = listOf("월", "화", "수", "목", "금")
private val CourseColors = listOf(
    Color(0xFF55B9DC),
    Color(0xFFEAC94D),
    Color(0xFF2D82EA),
    Color(0xFFE94061),
    Color(0xFF54C987),
    Color(0xFF8F56EC)
)
private val EmptyTimetable = TimetableUiModel(
    id = "empty",
    name = "시간표 이름",
    year = DefaultYear,
    semester = DefaultSemester,
    courses = emptyList()
)

// 시간표 화면 상태 호스트: 로컬 시간표 생성, 선택, 수업 추가 상태를 관리한다.
@Composable
internal fun TimetableScreenContentHost() {
    val timetableViewModel: TimetableViewModel = hiltViewModel()
    val apiUiState by timetableViewModel.uiState.collectAsState()
    val events = remember { emptyList<TimetableEventItem>() }
    var timetables by remember { mutableStateOf(emptyList<TimetableUiModel>()) }
    var selectedTimetableId by rememberSaveable { mutableStateOf<String?>(null) }
    var isEventOverlayEnabled by rememberSaveable { mutableStateOf(false) }
    var isTimetablePanelOpen by rememberSaveable { mutableStateOf(false) }
    var isCreateTimetablePanelOpen by rememberSaveable { mutableStateOf(false) }
    var isEditTimetablePanelOpen by rememberSaveable { mutableStateOf(false) }
    var isAddCoursePanelOpen by rememberSaveable { mutableStateOf(false) }
    var nextTimetableId by rememberSaveable { mutableStateOf(1L) }
    var nextCourseId by rememberSaveable { mutableStateOf(1L) }
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
    var isSubmitting by rememberSaveable { mutableStateOf(false) }
    var submitError by rememberSaveable { mutableStateOf<String?>(null) }
    var submitMessage by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        timetableViewModel.loadTimetables(
            year = DefaultYear,
            semester = DefaultSemester
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
    val validation = TimetableAddCourseValidator.validate(
        timetableId = selectedTimetableId.takeIf { hasSelectedTimetable },
        title = courseTitle,
        creditText = creditText,
        timeSlots = timeSlots,
        existingTimeSlots = selectedTimetable.courses.toExistingEditableSlots(),
        isSubmitting = isSubmitting
    )

    fun closePanels() {
        isTimetablePanelOpen = false
        isCreateTimetablePanelOpen = false
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

    TimetableScreenContent(
        selectedTimetable = selectedTimetable,
        timetables = timetables,
        events = events,
        isLoadingTimetables = apiUiState.isLoadingTimetables,
        loadTimetablesError = apiUiState.loadTimetablesError,
                deletingTimetableId = apiUiState.deletingTimetableId?.toString(),
        deleteTimetableError = apiUiState.deleteTimetableError,
        hasSelectedTimetable = hasSelectedTimetable,
        isEventOverlayEnabled = isEventOverlayEnabled,
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
        submitError = submitError,
        submitMessage = submitMessage,
        isSubmitting = isSubmitting,
        onEventOverlayChanged = { isEventOverlayEnabled = it },
        onOpenTimetablePanel = {
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
                    year = DefaultYear,
                    semester = DefaultSemester
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
        },
        onInstructorChange = { instructor = it },
        onCreditChange = {
            creditText = it.filter { char -> char.isDigit() }
            submitError = null
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
        onSubmitAddCourse = {
            if (!validation.canSubmit || isSubmitting || selectedTimetableId == null) {
                return@TimetableScreenContent
            }
            isSubmitting = true
            val newCourse = CourseUiModel(
                id = "local-course-${nextCourseId++}",
                title = courseTitle.trim(),
                instructor = instructor.trim().takeIf { it.isNotEmpty() },
                credit = creditText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull(),
                color = CourseColors[(nextCourseId.toInt() - 2).floorMod(CourseColors.size)],
                timeSlots = timeSlots.map { slot ->
                    CourseTimeSlot(
                        weekday = slot.dayOfWeek.weekday,
                        startMinute = slot.startAt,
                        endMinute = slot.endAt
                    )
                }
            )
            timetables = timetables.map { timetable ->
                if (timetable.id == selectedTimetableId) {
                    timetable.copy(courses = timetable.courses + newCourse)
                } else {
                    timetable
                }
            }
            isSubmitting = false
            resetAddCourseForm(message = "수업을 추가했습니다")
        }
    )
}

// 화면 전체 레이아웃: 그리드, 플로팅 버튼, 하단 패널들을 한 화면 안에서 겹쳐 배치한다.
@Composable
private fun TimetableScreenContent(
    selectedTimetable: TimetableUiModel,
    timetables: List<TimetableUiModel>,
    events: List<TimetableEventItem>,
    isLoadingTimetables: Boolean,
    loadTimetablesError: String?,
        deletingTimetableId: String?,
    deleteTimetableError: String?,
    hasSelectedTimetable: Boolean,
    isEventOverlayEnabled: Boolean,
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
    onEventOverlayChanged: (Boolean) -> Unit,
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
    onSubmitAddCourse: () -> Unit
) {
    val hasPanelOpen = isTimetablePanelOpen || isCreateTimetablePanelOpen || isEditTimetablePanelOpen || isAddCoursePanelOpen
    BackHandler(enabled = hasPanelOpen, onBack = onClosePanels)

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
            TimetableHeader(
                name = selectedTimetable.name,
                credits = selectedTimetable.totalCredits,
                isEventOverlayEnabled = isEventOverlayEnabled,
                onEventOverlayChanged = onEventOverlayChanged
            )
            Spacer(modifier = Modifier.height(4.dp))
            WeekdayHeader()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                WeeklyTimetableGrid(
                    courses = selectedTimetable.courses,
                    events = events,
                    showEvents = isEventOverlayEnabled,
                    modifier = Modifier.fillMaxSize()
                )
                TimetableFloatingActions(
                    hasSelectedTimetable = hasSelectedTimetable,
                    onChangeTimetableClick = onOpenTimetablePanel,
                    onAddCourseClick = onOpenAddCourse,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
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
private fun TimetableHeader(
    name: String,
    credits: Int,
    isEventOverlayEnabled: Boolean,
    onEventOverlayChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink100,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "(${credits}학점)",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
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
    }
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
            events.forEach { event ->
                val position = eventPositions[event.id] ?: return@forEach
                TimetableBlockBackground(position, dayWidth, gridHeight, event.categoryColor, 1f)
            }
            courseBlocks.forEach { block ->
                val position = coursePositions[block.id] ?: return@forEach
                TimetableBlockBackground(position, dayWidth, gridHeight, CourseMaskColor, 0.72f)
            }
            events.forEach { event ->
                val position = eventPositions[event.id] ?: return@forEach
                TimetableBlockLabel(position, dayWidth, gridHeight, event.title, PureWhite, onClick = {})
            }
        } else {
            courseBlocks.forEach { block ->
                val position = coursePositions[block.id] ?: return@forEach
                CourseBlock(position, dayWidth, gridHeight, block.color, block.title, block.subtitle, onClick = {})
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
private fun CourseBlock(position: PositionedTimetableBlock, dayWidth: Dp, gridHeight: Dp, color: Color, title: String, subtitle: String?, onClick: () -> Unit) {
    Box(
        modifier = blockModifier(position, dayWidth, gridHeight)
            .clip(RoundedCornerShape(0.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = listOfNotNull(title, subtitle).joinToString("\n"),
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimetableBlockBackground(position: PositionedTimetableBlock, dayWidth: Dp, gridHeight: Dp, color: Color, alpha: Float) {
    Box(modifier = blockModifier(position, dayWidth, gridHeight).background(color.copy(alpha = alpha)))
}

@Composable
private fun TimetableBlockLabel(position: PositionedTimetableBlock, dayWidth: Dp, gridHeight: Dp, text: String, textColor: Color, onClick: () -> Unit) {
    Box(
        modifier = blockModifier(position, dayWidth, gridHeight).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 5.dp),
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
                TimeStepper(label = "시작", minute = slot.startAt, onChange = onChangeStart, modifier = Modifier.weight(1f))
                TimeStepper(label = "종료", minute = slot.endAt, onChange = onChangeEnd, modifier = Modifier.weight(1f))
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
private fun TimeStepper(label: String, minute: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = Ink60, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(minute - 5) }, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Rounded.Remove, contentDescription = "$label 5분 감소")
            }
            Text(formatMinute(minute), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onChange(minute + 5) }, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "$label 5분 증가")
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
            CourseBlockUiModel("${course.id}-$index", course.title, course.subtitle, course.color, slot.weekday, slot.startMinute, slot.endMinute)
        }
    }
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
