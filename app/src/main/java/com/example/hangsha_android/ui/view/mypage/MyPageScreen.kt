package com.example.hangsha_android.ui.view.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink90
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite
import com.example.hangsha_android.ui.view.bookmarks.BookmarkedEventItem
import com.example.hangsha_android.ui.view.event.eventTypeColor

private val MyPageActionBorderColor = Color(0xFFCACACA)
private val MyPageActionMutedIconColor = Color(0xFF9B9B9B)

// 마이페이지 화면 배치
@Composable
fun MyPageScreen(
    uiState: MyPageUiState,
    onRetryClick: () -> Unit,
    onStartProfileEdit: () -> Unit,
    onDraftUsernameChanged: (String) -> Unit,
    onDraftProfileImageSelected: (android.net.Uri) -> Unit,
    onDraftProfileImageDeleted: () -> Unit,
    onSaveProfileEdit: () -> Unit,
    onInterestPriorityClick: () -> Unit,
    onTimetableClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onBookmarkedEventClick: (Long) -> Unit,
    onMemoListClick: () -> Unit,
    onMemoEventClick: (Long) -> Unit,
    onLogoutClick: () -> Unit,
    onBugReportTitleChanged: (String) -> Unit,
    onBugReportContentChanged: (String) -> Unit,
    onSubmitBugReportClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 29.dp,
                end = 29.dp,
                top = 17.dp,
                bottom = 28.dp
            )
        ) {
            item {
                when {
                    uiState.errorMessage != null -> {
                        MyPageErrorState(
                            message = uiState.errorMessage,
                            onRetryClick = onRetryClick
                        )
                    }

                    else -> {
                        ProfileHeader(
                            uiState = uiState,
                            onStartProfileEdit = onStartProfileEdit,
                            onDraftUsernameChanged = onDraftUsernameChanged,
                            onDraftProfileImageSelected = onDraftProfileImageSelected,
                            onDraftProfileImageDeleted = onDraftProfileImageDeleted,
                            onSaveProfileEdit = onSaveProfileEdit
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        PrioritySection(
                            interests = uiState.interests,
                            onClick = onInterestPriorityClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TimetableRegistrationRow(onClick = onTimetableClick)
                        Spacer(modifier = Modifier.height(29.dp))
                        BookmarksPreviewSection(
                            items = uiState.bookmarkedEvents,
                            isLoading = uiState.isBookmarksPreviewLoading,
                            errorMessage = uiState.bookmarksPreviewErrorMessage,
                            hasMoreItems = uiState.hasMoreBookmarkedEvents,
                            onHeaderClick = onBookmarksClick,
                            onEventClick = onBookmarkedEventClick,
                            onMoreClick = onBookmarksClick
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(17.dp))
                        MemosPreviewSection(
                            items = uiState.memoItems,
                            isLoading = uiState.isMemosPreviewLoading,
                            errorMessage = uiState.memosPreviewErrorMessage,
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = MyPageActionMutedIconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onHeaderClick = onMemoListClick,
                            onMemoClick = onMemoEventClick
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(17.dp))
                        BugReportSection(
                            title = uiState.bugReportTitle,
                            content = uiState.bugReportContent,
                            isSubmitting = uiState.isSubmittingBugReport,
                            onTitleChanged = onBugReportTitleChanged,
                            onContentChanged = onBugReportContentChanged,
                            onSubmitClick = onSubmitBugReportClick
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(17.dp))
                        AccountActionSection(
                            title = "로그아웃",
                            description = "현재 계정에서 로그아웃합니다.",
                            buttonText = "로그아웃",
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                            },
                            buttonColor = Color(0xFF555555),
                            contentColor = PureWhite,
                            onClick = onLogoutClick
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(17.dp))
                        AccountActionSection(
                            title = "회원탈퇴",
                            description = "계정을 삭제하면 저장된 정보가 복구되지 않습니다.",
                            buttonText = "회원탈퇴",
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                            },
                            buttonColor = PureWhite,
                            contentColor = Color(0xFFFF4B4B),
                            borderColor = Color(0xFFFFA0A0),
                            onClick = { showDeleteAccountDialog = true }
                        )
                    }
                }
            }
        }

        if (showDeleteAccountDialog) {
            DeleteAccountDialog(
                email = uiState.email,
                isDeletingAccount = uiState.isDeletingAccount,
                errorMessage = uiState.accountDeletionErrorMessage,
                onDismissRequest = {
                    if (!uiState.isDeletingAccount) {
                        showDeleteAccountDialog = false
                    }
                },
                onConfirmClick = onDeleteAccountClick
            )
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    email: String,
    isDeletingAccount: Boolean,
    errorMessage: String?,
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit
) {
    var isUnderstood by remember { mutableStateOf(false) }
    var emailConfirmation by remember { mutableStateOf("") }
    val isEmailMatched = emailConfirmation == email
    val canConfirm = isUnderstood && isEmailMatched && !isDeletingAccount

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "회원 탈퇴")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "탈퇴하면 계정 정보가 삭제되며 복구할 수 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink100,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isDeletingAccount) {
                            isUnderstood = !isUnderstood
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isUnderstood,
                        onCheckedChange = { checked -> isUnderstood = checked },
                        enabled = !isDeletingAccount
                    )
                    Text(
                        text = "위 내용을 이해했습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink100,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "확인을 위해 이메일을 그대로 입력해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink60,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = emailConfirmation,
                    onValueChange = { emailConfirmation = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDeletingAccount,
                    singleLine = true,
                    placeholder = {
                        Text(text = email)
                    },
                    isError = emailConfirmation.isNotEmpty() && !isEmailMatched,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    shape = RoundedCornerShape(5.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MyPageActionBorderColor,
                        unfocusedBorderColor = MyPageActionBorderColor,
                        errorBorderColor = Color(0xFFFF4B4B),
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite,
                        disabledContainerColor = PureWhite,
                        errorContainerColor = PureWhite
                    )
                )
                if (emailConfirmation.isNotEmpty() && !isEmailMatched) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "이메일이 일치하지 않습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFF4B4B),
                        fontSize = 10.sp
                    )
                }
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFF4B4B),
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmClick,
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4B4B),
                    contentColor = PureWhite,
                    disabledContainerColor = Color(0xFFFFD6D6),
                    disabledContentColor = PureWhite
                )
            ) {
                if (isDeletingAccount) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PureWhite
                    )
                } else {
                    Text(text = "승인")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isDeletingAccount
            ) {
                Text(text = "취소")
            }
        }
    )
}

@Composable
private fun AccountActionSection(
    title: String,
    description: String,
    buttonText: String,
    icon: @Composable () -> Unit,
    buttonColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink60,
                fontSize = 10.sp
            )
        }
        Row(
            modifier = Modifier
                .height(26.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(buttonColor)
                .then(
                    if (borderColor != null) {
                        Modifier.border(1.dp, borderColor, RoundedCornerShape(5.dp))
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(
                text = buttonText,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 에러 안내 영역
@Composable
private fun MyPageErrorState(
    message: String,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 120.dp),
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

private const val MY_PAGE_PREVIEW_SIZE = 20
