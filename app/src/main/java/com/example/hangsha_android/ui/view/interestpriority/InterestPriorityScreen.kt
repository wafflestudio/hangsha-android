package com.example.hangsha_android.ui.view.interestpriority

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.data.repository.model.CategoryKey
import com.example.hangsha_android.ui.theme.Ink100

// 관심사 설정 화면 구성
@Composable
fun InterestPriorityScreen(
    uiState: InterestPriorityUiState,
    onNavigateBack: () -> Unit,
    onCategoryClick: (CategoryKey) -> Unit,
    onRetryClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val categoriesById = uiState.categoryGroups
        .flatMap { group -> group.categories }
        .associateBy { category -> category.key }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = 10.dp,
                bottom = 88.dp
            )
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "뒤로 가기",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "관심사 설정",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "먼저 보고 싶은 행사의 카테고리\n또는 주체기관을 선택해주세요.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                SelectedInterestPriorityRow(
                    selectedIds = uiState.selectedCategoryIds,
                    categoriesById = categoriesById
                )
                if (uiState.saveErrorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.saveErrorMessage,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            if (uiState.errorMessage != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRetryClick) {
                            Text(text = "다시 시도")
                        }
                    }
                }
            } else {
                uiState.categoryGroups.forEach { group ->
                    item {
                        val isProgramType = group.name == "프로그램 유형"
                        InterestCategorySection(
                            title = if (isProgramType) "카테고리" else group.name,
                            categories = group.categories,
                            selectedIds = uiState.selectedCategoryIds,
                            chipColor = if (isProgramType) {
                                ProgramCategoryChipColor
                            } else {
                                GeneralCategoryChipColor
                            },
                            onCategoryClick = onCategoryClick
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        if (uiState.isLoading || uiState.isSaving) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Button(
                onClick = onDoneClick,
                enabled = !uiState.isLoading && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "완료",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 선택 우선순위 행 구성
@Composable
private fun SelectedInterestPriorityRow(
    selectedIds: List<CategoryKey>,
    categoriesById: Map<CategoryKey, InterestCategoryUiModel>
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (selectedIds.isEmpty()) {
            SelectedPriorityChip(text = "1순위:")
        } else {
            selectedIds.forEachIndexed { index, categoryId ->
                SelectedPriorityChip(
                    text = "${index + 1}순위: ${categoriesById[categoryId]?.name.orEmpty()}"
                )
            }
        }
    }
}

// 선택 우선순위 알약 구성
@Composable
private fun SelectedPriorityChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(100.dp))
            .padding(horizontal = 13.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// 카테고리 묶음 구성
@Composable
private fun InterestCategorySection(
    title: String,
    categories: List<InterestCategoryUiModel>,
    selectedIds: List<CategoryKey>,
    chipColor: Color,
    onCategoryClick: (CategoryKey) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            categories.forEach { category ->
                val selectedOrder = selectedIds.indexOf(category.key)
                InterestCategoryChip(
                    text = category.name,
                    selectedOrder = selectedOrder.takeIf { it >= 0 }?.plus(1),
                    color = chipColor,
                    onClick = { onCategoryClick(category.key) }
                )
            }
        }
    }
}

// 카테고리 알약 구성
@Composable
private fun InterestCategoryChip(
    text: String,
    selectedOrder: Int?,
    color: Color,
    onClick: () -> Unit
) {
    val contentColor = if (selectedOrder == null) {
        Ink100
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selectedOrder == null) color else MaterialTheme.colorScheme.primary)
            .then(
                if (selectedOrder != null) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(100.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedOrder != null) {
            Text(
                text = "$selectedOrder",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
