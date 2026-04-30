package com.example.hangsha_android.ui.view.calendar

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.theme.Coral60
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Peach20
import com.example.hangsha_android.ui.theme.PureWhite
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayCardShadow = Color(0x16000000)
private val DayDivider = Color(0xFFF0F1F3)
private val DayCellBorder = Color(0xFFE6E8EB)
private val OutOfMonthText = Color(0xFFC9CDD3)

private val KoreanMonthFormatter = DateTimeFormatter.ofPattern("yyyy'년' M'월'", Locale.KOREAN)
private val WeekdayLabels = listOf("일", "월", "화", "수", "목", "금", "토")
private const val MaxVisibleEventMarkers = 4
private val ScreenHorizontalPadding = 15.dp
private val ScreenVerticalPadding = 30.dp
private val DayCellCornerRadius = 3.dp

private data class CalendarHeaderState(
    val currentMonth: YearMonth,
    val hasActiveFilters: Boolean,
    val isLoading: Boolean
)

private data class CalendarDayUiModel(
    val isCurrentMonth: Boolean,
    val dayTextColor: Color,
    val markerColors: List<Color>,
    val footerColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onOpenFilterClick: () -> Unit,
    onDismissFilterSheet: () -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onRetryClick: () -> Unit
) {
    CalendarScreenContent(
        uiState = uiState,
        onPreviousMonthClick = onPreviousMonthClick,
        onNextMonthClick = onNextMonthClick,
        onOpenFilterClick = onOpenFilterClick,
        onDismissFilterSheet = onDismissFilterSheet,
        onApplyFilters = onApplyFilters,
        onClearFilters = onClearFilters,
        onRetryClick = onRetryClick
    )
}

@Composable
private fun CalendarScreenContent(
    uiState: CalendarUiState,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onOpenFilterClick: () -> Unit,
    onDismissFilterSheet: () -> Unit,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onRetryClick: () -> Unit
) {
    if (uiState.isFilterSheetVisible) {
        // 우측 상단 슬라이더 버튼 - 필터 시트
        FilterSheet(
            uiState = uiState,
            onDismiss = onDismissFilterSheet,
            onApply = onApplyFilters,
            onClear = onClearFilters
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = ScreenHorizontalPadding,
                vertical = ScreenVerticalPadding
            )
    ) {
        Spacer(modifier = Modifier.height(35.dp))
        Spacer(modifier = Modifier.weight(1f))
        // "2026년 3월", 좌우 화살표, 필터 버튼
        CalendarHeader(
            state = CalendarHeaderState(
                currentMonth = uiState.currentMonth,
                hasActiveFilters = uiState.hasActiveFilters,
                isLoading = uiState.isLoading
            ),
            onPreviousMonthClick = onPreviousMonthClick,
            onNextMonthClick = onNextMonthClick,
            onOpenFilterClick = onOpenFilterClick
        )
        Spacer(modifier = Modifier.height(15.dp))

        // "일 월 화 수 목 금 토"
        WeekdayHeader()
        Spacer(modifier = Modifier.height(30.dp))

        if (uiState.errorMessage != null) {
            ErrorState(
                message = uiState.errorMessage,
                onRetryClick = onRetryClick,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // 월간 달력 카드들
            CalendarBody(
                visibleDates = uiState.visibleDates,
                currentMonth = uiState.currentMonth,
                eventsByDate = uiState.eventsByDate,
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CalendarHeader(
    state: CalendarHeaderState,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onOpenFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 연, 월 텍스트
        Text(
            text = state.currentMonth.format(KoreanMonthFormatter),
            style = MaterialTheme.typography.labelLarge,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        // 이전, 이후 달로 변경 버튼
        HeaderNavigationButtons(
            isLoading = state.isLoading,
            onPreviousMonthClick = onPreviousMonthClick,
            onNextMonthClick = onNextMonthClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        // 필터 버튼
        FilterButton(
            isLoading = state.isLoading,
            hasActiveFilters = state.hasActiveFilters,
            onOpenFilterClick = onOpenFilterClick
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HeaderNavigationButtons(
    isLoading: Boolean,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit
) {
    // 이전 달로 이동
    HeaderCircleButton(
        enabled = !isLoading,
        onClick = onPreviousMonthClick
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "Previous month",
            tint = Ink60
        )
    }
    Spacer(modifier = Modifier.width(1.dp))

    // 다음 달로 이동
    HeaderCircleButton(
        enabled = !isLoading,
        onClick = onNextMonthClick
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Next month",
            tint = Ink60
        )
    }
}

@Composable
private fun FilterButton(
    isLoading: Boolean,
    hasActiveFilters: Boolean,
    onOpenFilterClick: () -> Unit
) {
    Box {
        HeaderCircleButton(
            enabled = !isLoading,
            onClick = onOpenFilterClick,
            isElevated = true
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = "Filter",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        if (hasActiveFilters) {
            // TODO: 필터 적용 여부를 알려 주는 작은 배지 점
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Coral60)
            )
        }
    }
}

@Composable
private fun CalendarBody(
    visibleDates: List<LocalDate>,
    currentMonth: YearMonth,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // 날짜 카드들의 월간 그리드
        CalendarMonthGrid(
            visibleDates = visibleDates,
            currentMonth = currentMonth,
            eventsByDate = eventsByDate,
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            LoadingOverlay()
        }
    }
}

@Composable
private fun HeaderCircleButton(
    enabled: Boolean,
    onClick: () -> Unit,
    isElevated: Boolean = false,
    content: @Composable () -> Unit
) {
    if (isElevated) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(9.dp),
            color = PureWhite,
            shadowElevation = 2.dp // Apply shadow
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = enabled, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}


@Composable
private fun WeekdayHeader() {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        WeekdayLabels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // 일요일만 붉게, 나머지는 옅은 회색으로
                // TODO: 오늘 날짜는 진하게 표현 (예: 2026년 4월 27일이 오늘이라면 "27" 숫자와 "월" 요일을 강조)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (index == 0) Coral60 else OutOfMonthText
                )
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    visibleDates: List<LocalDate>,
    currentMonth: YearMonth,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    modifier: Modifier = Modifier
) {
    val weeks = visibleDates.chunked(size = 7)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 각 Row가 스크린샷의 한 줄, 즉 한 주를 의미
                week.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        currentMonth = currentMonth,
                        events = eventsByDate[date].orEmpty(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    currentMonth: YearMonth,
    events: List<CalendarEvent>,
    modifier: Modifier = Modifier
) {
    val dayModel = buildCalendarDayUiModel(
        date = date,
        currentMonth = currentMonth,
        events = events
    )

    Column(
        modifier = modifier
            .aspectRatio(0.5f)
            .drawBehind {
                val radius = DayCellCornerRadius.toPx()
                val offset = 4.dp.toPx() // 그림자 길이
                val blurRadius = 2.dp.toPx() // 블러의 퍼짐 정도 (값을 키울수록 더 부드럽게 퍼짐)

                drawIntoCanvas { canvas ->
                    val paint = Paint()
                    val frameworkPaint = paint.asFrameworkPaint()

                    // 그림자 색상 설정
                    frameworkPaint.color = DayCardShadow.toArgb()

                    // 블러 필터 적용
                    frameworkPaint.maskFilter = BlurMaskFilter(
                        blurRadius,
                        BlurMaskFilter.Blur.NORMAL
                    )

                    // 캔버스에 모서리가 둥근 사각형(그림자) 그리기
                    canvas.drawRoundRect(
                        left = offset,
                        top = offset,
                        right = size.width + offset,
                        bottom = size.height + offset,
                        radiusX = radius,
                        radiusY = radius,
                        paint = paint
                    )
                }
            }
            .background(
                color = PureWhite,
                shape = RoundedCornerShape(DayCellCornerRadius)
            )
            .border( // 선
                width = 0.dp, // 일단 피그마에는 선이 없음...
                color = DayCellBorder,
                shape = RoundedCornerShape(DayCellCornerRadius)
            )
            .padding(horizontal = 3.dp, vertical = 3.dp) // 내부 여백
    ) {
        // 날짜 숫자
        Box(
            modifier = Modifier.size(width = 20.dp, height = 21.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 14.sp, // 폰트 크기 14.sp 고정
                color = dayModel.dayTextColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 날짜 카드 안의 노랑, 보라, 파랑, 초록 일정 막대
            dayModel.markerColors.forEach { color ->
                EventMarkerBar(
                    color = color,
                    alpha = if (dayModel.isCurrentMonth) 1f else 0.45f
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    // 아래의 얇은 컬러 라인
                    color = dayModel.footerColor,
                    shape = RoundedCornerShape(99.dp)
                )
        )
    }
}

@Composable
private fun buildCalendarDayUiModel(
    date: LocalDate,
    currentMonth: YearMonth,
    events: List<CalendarEvent>
): CalendarDayUiModel {
    val isCurrentMonth = YearMonth.from(date) == currentMonth
    val dayTextColor = when {
        !isCurrentMonth -> OutOfMonthText
        date.dayOfWeek == DayOfWeek.SUNDAY -> Coral60
        else -> MaterialTheme.colorScheme.onSurface
    }
    val markerColors = events
        .take(MaxVisibleEventMarkers)
        .map(CalendarEventColorMapper::colorFor)

    return CalendarDayUiModel(
        isCurrentMonth = isCurrentMonth,
        dayTextColor = dayTextColor,
        markerColors = markerColors,
        footerColor = markerColors.lastOrNull() ?: DayDivider
    )
}

@Composable
private fun EventMarkerBar(
    color: Color,
    alpha: Float
) {
    // 날짜 카드 안의 개별 일정 한 줄
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(0.dp))
            .background(color.copy(alpha = alpha))
    )
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 로딩 패널
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp
                )
                Text(
                    text = "일정을 불러오는 중",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Peach20.copy(alpha = 0.45f)
        ) {
            // 데이터를 못 불러올 때 달력 대신 중앙에 뜨는 에러 카드
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = onRetryClick) {
                    Text(text = "다시 시도")
                }
            }
        }
    }
}

// TODO: 필터 기능 만들기
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    uiState: CalendarUiState,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "필터",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = buildFilterSummary(uiState),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "초기화")
                }
                Button(
                    onClick = onApply,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "적용")
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

private fun buildFilterSummary(uiState: CalendarUiState): String {
    if (!uiState.hasActiveFilters) {
        return "적용된 필터가 없습니다."
    }

    return buildList {
        if (uiState.appliedFilters.bookmarkedOnly) add("북마크만")
        if (uiState.appliedFilters.interestedOnly) add("관심 일정만")
        if (uiState.appliedFilters.operationModes.isNotEmpty()) {
            add("운영 방식 ${uiState.appliedFilters.operationModes.size}개")
        }
        if (uiState.appliedFilters.statusIds.isNotEmpty()) {
            add("상태 ${uiState.appliedFilters.statusIds.size}개")
        }
        if (uiState.appliedFilters.eventTypeIds.isNotEmpty()) {
            add("유형 ${uiState.appliedFilters.eventTypeIds.size}개")
        }
    }.joinToString(", ")
}
