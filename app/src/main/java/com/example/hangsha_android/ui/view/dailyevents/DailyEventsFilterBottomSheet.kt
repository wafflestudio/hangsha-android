package com.example.hangsha_android.ui.view.dailyevents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.hangsha_android.ui.theme.PureWhite
import com.example.hangsha_android.ui.view.event.eventTypeColor
import com.example.hangsha_android.ui.view.org.organizationLabel

private val FilterSheetBackground = Color(0xFFF8F8F6)
private val FilterDivider = Color(0xFFE7E5E1)
private val FilterActionGray = Color(0xFFE1E1DF)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEventsFilterBottomSheet(
    uiState: DailyEventsUiState,
    onDismiss: () -> Unit,
    onSelectTab: (DailyEventsFilterTab) -> Unit,
    onBookmarkedOnlyChange: (Boolean) -> Unit,
    onInterestedOnlyChange: (Boolean) -> Unit,
    onToggleOrgId: (Long) -> Unit,
    onToggleStatus: (Long) -> Unit,
    onToggleEventType: (Long) -> Unit,
    onExcludeKeywordInputChange: (String) -> Unit,
    onAddExcludeKeyword: () -> Unit,
    onRemoveExcludeKeyword: (String) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    val draft = uiState.draftFilters

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FilterSheetBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            FilterTabRow(
                selectedTab = uiState.selectedFilterTab,
                onSelectTab = onSelectTab
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                when (uiState.selectedFilterTab) {
                    DailyEventsFilterTab.EVENT_TYPE -> {
                        EventTypeSection(
                            selected = draft.eventTypeIds,
                            options = uiState.availableFilterOptions.eventTypeIds,
                            onToggle = onToggleEventType
                        )
                    }

                    DailyEventsFilterTab.ORGANIZER -> {
                        FilterChecklistSection(
                            allLabel = "주최 기관 전체",
                            title = "주최 기관",
                            emptyText = "선택 가능한 주최 기관이 없습니다.",
                            options = uiState.availableFilterOptions.orgIds,
                            selected = draft.orgIds,
                            label = { organizationLabel(it, uiState.organizationNames) },
                            onToggle = onToggleOrgId
                        )
                    }

                    DailyEventsFilterTab.RECRUITMENT_STATUS -> {
                        val validStatusIds = uiState.availableFilterOptions.statusIds.filter { statusId ->
                            statusId in listOf(1L, 2L, 3L)
                        }

                        FilterChecklistSection(
                            allLabel = "모집 현황 전체",
                            title = "모집 현황",
                            emptyText = "선택 가능한 모집 현황이 없습니다.",
                            options = validStatusIds,
                            selected = draft.statusIds,
                            label = { statusLabel(it) },
                            onToggle = onToggleStatus
                        )
                    }

                    DailyEventsFilterTab.EXCLUDE -> {
                        ExcludeKeywordSection(
                            input = uiState.excludeKeywordInput,
                            keywords = draft.excludedKeywords,
                            onInputChange = onExcludeKeywordInputChange,
                            onAdd = onAddExcludeKeyword,
                            onRemove = onRemoveExcludeKeyword
                        )
                    }
                }
            }

            FilterFooter(
                resultCount = uiState.filteredItemCount,
                isLoggedIn = uiState.isLoggedIn,
                isLoading = uiState.isLoading,
                onClear = onClear,
                onApply = onApply
            )
        }
    }
}

@Composable
private fun FilterTabRow(
    selectedTab: DailyEventsFilterTab,
    onSelectTab: (DailyEventsFilterTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PureWhite)
            .padding(horizontal = 20.dp)
    ) {
        DailyEventsFilterTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectTab(tab) }
                    .padding(top = 8.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSelected) 2.dp else 1.dp)
                        .background(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                FilterDivider
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun EventTypeSection(
    selected: Set<Long>,
    options: List<Long>,
    onToggle: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (options.isEmpty()) {
            Text(
                text = "선택 가능한 행사 종류가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        val groupedOptions = options
            .groupBy(::eventTypeLabel)
            .entries
            .sortedBy { (label, _) -> if (label == "기타") 1 else 0 }

        groupedOptions.forEach { (label, eventTypeIds) ->
            val representativeId = eventTypeIds.first()
            val isSelected = eventTypeIds.all { it in selected }
            val baseColor = eventTypeColor(representativeId)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        eventTypeIds.forEach { eventTypeId ->
                            onToggle(eventTypeId)
                        }
                    }
                    .background(
                        color = baseColor.copy(
                            alpha = baseColor.alpha * if (isSelected) 1f else 0.75f
                        ),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionSquare(selected = isSelected)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun <T> FilterChecklistSection(
    allLabel: String,
    title: String,
    emptyText: String,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (options.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        val allSelected = options.all { it in selected }

        FilterChecklistRow(
            text = allLabel,
            selected = allSelected,
            onClick = {
                if (allSelected) {
                    options.forEach { option ->
                        if (option in selected) onToggle(option)
                    }
                } else {
                    options.forEach { option ->
                        if (option !in selected) onToggle(option)
                    }
                }
            }
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = FilterDivider.copy(alpha = 0.7f)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { option ->
                FilterChecklistRow(
                    text = label(option),
                    selected = option in selected,
                    onClick = { onToggle(option) }
                )
            }
        }
    }
}

@Composable
private fun FilterChecklistRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionSquare(selected = selected)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SelectionSquare(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
            .background(
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = RoundedCornerShape(2.dp)
            )
    )
}

@Composable
private fun ExcludeKeywordSection(
    input: String,
    keywords: List<String>,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "해당 단어를 포함하는 행사는 표시하지 않습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("제외 키워드 입력") },
            trailingIcon = {
                androidx.compose.material3.IconButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "제외 키워드 추가"
                    )
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
            maxLines = 1
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keywords.forEach { keyword ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onRemove(keyword) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = keyword,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterFooter(
    resultCount: Int,
    isLoggedIn: Boolean,
    isLoading: Boolean,
    onClear: () -> Unit,
    onApply: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PureWhite,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FilterActionGray,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("초기화")
            }

            Button(
                onClick = onApply,
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = PureWhite,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = if (isLoggedIn) "행사 보기" else "${resultCount}개의 행사 보기"
                )
            }
        }
    }
}

private fun statusLabel(statusId: Long): String {
    return when (statusId) {
        1L -> "모집 대기"
        2L -> "모집 중"
        3L -> "모집 마감"
        else -> "상태 $statusId"
    }
}

private fun eventTypeLabel(eventTypeId: Long): String {
    return when (eventTypeId) {
        4L -> "교육(특강/세미나)"
        5L -> "공모전/경진대회"
        6L -> "인턴십/현장실습"
        7L -> "사회공헌(봉사)"
        8L -> "취업/진로상담"
        39L -> "OpenLNL"
        else -> "기타"
    }
}
