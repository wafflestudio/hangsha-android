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
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink90
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite

@Composable
fun InterestPriorityScreen(
    uiState: InterestPriorityUiState,
    onNavigateBack: () -> Unit,
    onCategoryClick: (Long) -> Unit,
    onRetryClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    val categoriesById = uiState.categoryGroups
        .flatMap { group -> group.categories }
        .associateBy { category -> category.id }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F7))
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
                            tint = Ink100
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
                    color = Ink100,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "먼저 보고 싶은 행사의 카테고리\n또는 주체기관을 선택해주세요.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Ink100,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                SelectedInterestPriorityRow(
                    selectedIds = uiState.selectedCategoryIds,
                    categoriesById = categoriesById
                )
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
                            color = Ink90,
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
                                Color(0xFF73C9E3)
                            } else {
                                Color(0xFF6CD39A)
                            },
                            onCategoryClick = onCategoryClick
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }

        if (uiState.isLoading) {
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
            color = PureWhite,
            tonalElevation = 2.dp
        ) {
            Button(
                onClick = onDoneClick,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF555555),
                    contentColor = PureWhite,
                    disabledContainerColor = Color(0xFFBDBDBD),
                    disabledContentColor = PureWhite
                )
            ) {
                Text(
                    text = "완료",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SelectedInterestPriorityRow(
    selectedIds: List<Long>,
    categoriesById: Map<Long, InterestCategoryUiModel>
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

@Composable
private fun SelectedPriorityChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(PureWhite)
            .border(1.dp, Color(0xFFE1E1E1), RoundedCornerShape(100.dp))
            .padding(horizontal = 13.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Ink60,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InterestCategorySection(
    title: String,
    categories: List<InterestCategoryUiModel>,
    selectedIds: List<Long>,
    chipColor: Color,
    onCategoryClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF44BBD8)
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            categories.forEach { category ->
                val selectedOrder = selectedIds.indexOf(category.id)
                InterestCategoryChip(
                    text = category.name,
                    selectedOrder = selectedOrder.takeIf { it >= 0 }?.plus(1),
                    color = chipColor,
                    onClick = { onCategoryClick(category.id) }
                )
            }
        }
    }
}

@Composable
private fun InterestCategoryChip(
    text: String,
    selectedOrder: Int?,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (selectedOrder == null) color else color.copy(alpha = 0.78f))
            .then(
                if (selectedOrder != null) {
                    Modifier.border(1.dp, Ink100.copy(alpha = 0.18f), RoundedCornerShape(100.dp))
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
                color = PureWhite
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
