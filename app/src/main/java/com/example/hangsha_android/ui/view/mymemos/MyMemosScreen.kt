package com.example.hangsha_android.ui.view.mymemos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink90
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite

private val MemoCardBorderColor = Color(0xFFE8E8E8)
private val MemoCardShadowColor = Color(0x11000000)
private val MemoTagBackground = Color(0xFFE8E8E8)
private val MemoIconColor = Color(0xFF777777)

@Composable
fun MyMemosScreen(
    uiState: MyMemosUiState,
    onNavigateBack: () -> Unit,
    onMemoClick: (Long) -> Unit,
    onDeleteMemoClick: (Long) -> Unit,
    onStartEditMemo: (MyMemoItem) -> Unit,
    onEditContentChanged: (String) -> Unit,
    onStartAddingTag: () -> Unit,
    onEditTagInputChanged: (String) -> Unit,
    onAddEditTag: () -> Unit,
    onRemoveEditTag: (String) -> Unit,
    onSaveEditedMemo: () -> Unit,
    onRetryClick: () -> Unit
) {
    var pendingDeleteMemoId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp)
        ) {
            MyMemosTopBar(onNavigateBack = onNavigateBack)

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    MyMemosErrorState(
                        message = uiState.errorMessage,
                        onRetryClick = onRetryClick
                    )
                }

                uiState.groupedMemos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "아직 작성한 메모가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink60,
                            fontSize = 13.sp
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            top = 27.dp,
                            bottom = 32.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        uiState.groupedMemos.forEach { group ->
                            item(key = "group-${group.dateDisplay}") {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    Text(
                                        text = group.dateDisplay,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Ink60,
                                        fontSize = 15.sp
                                    )
                                    group.memos.forEach { memo ->
                                        val isEditing = uiState.editingMemoId == memo.id
                                        MyMemoCard(
                                            memo = memo,
                                            isEditing = isEditing,
                                            editContent = uiState.editContent,
                                            editTagNames = uiState.editTagNames,
                                            isAddingTag = uiState.isAddingTag,
                                            editTagInput = uiState.editTagInput,
                                            isSaving = uiState.savingMemoId == memo.id,
                                            onClick = { onMemoClick(memo.eventId) },
                                            isDeleting = uiState.deletingMemoId == memo.id,
                                            onDeleteClick = { pendingDeleteMemoId = memo.id },
                                            onEditClick = { onStartEditMemo(memo) },
                                            onEditContentChanged = onEditContentChanged,
                                            onStartAddingTag = onStartAddingTag,
                                            onEditTagInputChanged = onEditTagInputChanged,
                                            onAddEditTag = onAddEditTag,
                                            onRemoveEditTag = onRemoveEditTag,
                                            onSaveEditClick = onSaveEditedMemo
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteMemoId?.let { memoId ->
        ConfirmDeleteMemoDialog(
            onDismiss = { pendingDeleteMemoId = null },
            onConfirm = {
                pendingDeleteMemoId = null
                onDeleteMemoClick(memoId)
            }
        )
    }
}

@Composable
private fun ConfirmDeleteMemoDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "메모 삭제")
        },
        text = {
            Text(text = "정말로 삭제하시겠습니까?")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "삭제")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소")
            }
        }
    )
}

@Composable
private fun MyMemosTopBar(
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "뒤로 가기",
            tint = Color(0xFFB0B0B0),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .size(26.dp)
                .clickable(onClick = onNavigateBack)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "내 메모 목록",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                tint = Color(0xFFB0B0B0),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MyMemoCard(
    memo: MyMemoItem,
    isEditing: Boolean,
    editContent: String,
    editTagNames: List<String>,
    isAddingTag: Boolean,
    editTagInput: String,
    isSaving: Boolean,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onEditContentChanged: (String) -> Unit,
    onStartAddingTag: () -> Unit,
    onEditTagInputChanged: (String) -> Unit,
    onAddEditTag: () -> Unit,
    onRemoveEditTag: (String) -> Unit,
    onSaveEditClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isEditing) {
                    Modifier
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, MemoCardBorderColor),
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (isEditing) {
                MemoEditTextField(
                    value = editContent,
                    onValueChange = onEditContentChanged
                )
            } else {
                Text(
                    text = memo.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink100,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(if (isEditing) 16.dp else 45.dp))
            Text(
                text = memo.eventTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    editTagNames.forEach { tagName ->
                        MyMemoTagChip(
                            text = tagName,
                            onRemoveClick = { onRemoveEditTag(tagName) }
                        )
                    }
                    if (isAddingTag) {
                        MemoTagInputChip(
                            value = editTagInput,
                            onValueChange = onEditTagInputChanged,
                            onConfirmClick = onAddEditTag
                        )
                    }
                    MemoAddTagChip(onClick = onStartAddingTag)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "메모 저장",
                        tint = MemoIconColor,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(enabled = !isSaving, onClick = onSaveEditClick)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        memo.tagNames.forEach { tagName ->
                            MyMemoTagChip(text = tagName)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "메모 삭제",
                            tint = MemoIconColor,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(enabled = !isDeleting, onClick = onDeleteClick)
                        )
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "메모 수정",
                            tint = MemoIconColor,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(onClick = onEditClick)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoEditTextField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(7.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Color(0xFFC9C9C9))
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Ink100,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            cursorBrush = SolidColor(Ink100),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun MyMemoTagChip(
    text: String,
    onRemoveClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MemoTagBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (onRemoveClick != null) {
                Text(
                    text = "x",
                    modifier = Modifier.clickable(onClick = onRemoveClick),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink60,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MemoAddTagChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = MemoTagBackground
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "태그 추가",
            tint = MemoIconColor,
            modifier = Modifier
                .padding(5.dp)
                .size(17.dp)
        )
    }
}

@Composable
private fun MemoTagInputChip(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirmClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MemoTagBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Ink100,
                    fontSize = 12.sp
                ),
                cursorBrush = SolidColor(Ink100),
                modifier = Modifier.size(width = 58.dp, height = 18.dp)
            )
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "태그 추가 완료",
                tint = MemoIconColor,
                modifier = Modifier
                    .size(15.dp)
                    .clickable(onClick = onConfirmClick)
            )
        }
    }
}

@Composable
private fun MyMemosErrorState(
    message: String,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink90,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetryClick) {
            Text(text = "다시 시도")
        }
    }
}
