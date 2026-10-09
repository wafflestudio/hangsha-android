package com.example.hangsha_android.ui.view.calendar.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hangsha_android.ui.components.EventFilterFooter
import com.example.hangsha_android.ui.view.calendar.CalendarUiState
import com.example.hangsha_android.ui.view.org.organizationLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// 캘린더 필터 바텀시트 전체와 탭별 본문 구성을 담당한다.
fun CalendarFilterBottomSheet(
    uiState: CalendarUiState,
    onDismiss: () -> Unit,
    onSelectTab: (CalendarFilterTab) -> Unit,
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
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {

            // top bar
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
                    CalendarFilterTab.EVENT_TYPE -> {
                        // 행사 종류 탭
                        EventTypeSection(
                            selected = draft.eventTypeIds,
                            options = uiState.availableFilterOptions.eventTypeIds,
                            names = uiState.eventTypeNames,
                            onToggle = onToggleEventType
                        )
                    }

                    CalendarFilterTab.ORGANIZER -> {
                        // 주최 기관 탭
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

                    CalendarFilterTab.RECRUITMENT_STATUS -> {
                        // 모집 현황 탭
                        FilterChecklistSection(
                            allLabel = "모집 현황 전체",
                            title = "모집 현황",
                            emptyText = "선택 가능한 모집 현황이 없습니다.",
                            options = uiState.availableFilterOptions.statusIds,
                            selected = draft.statusIds,
                            label = { uiState.statusNames[it] ?: statusLabel(it) },
                            onToggle = onToggleStatus
                        )
                    }

                    CalendarFilterTab.EXCLUDE -> {
                        // 제외 탭
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

            // 초기화, 필터 버튼
            EventFilterFooter(
                resultCount = uiState.filteredEventCount,
                isCountLoading = uiState.isFilterCountLoading,
                isLoading = uiState.isLoading,
                onClear = onClear,
                onApply = onApply
            )
        }
    }
}

@Composable
// 상단 4개 탭
private fun FilterTabRow(
    selectedTab: CalendarFilterTab,
    onSelectTab: (CalendarFilterTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp)
    ) {
        CalendarFilterTab.entries.forEach { tab ->
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
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }
    }
}
