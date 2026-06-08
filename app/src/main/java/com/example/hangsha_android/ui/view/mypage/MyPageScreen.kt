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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink90
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite

private val DividerColor = Color(0xFFE7E7E7)
private val BorderColor = Color(0xFFCACACA)
private val MutedIconColor = Color(0xFF9B9B9B)
private val PriorityChipColors = listOf(
    Color(0xFF88D6F8),
    Color(0xFF83C9F4),
    Color(0xFFEAD452),
    Color(0xFFC6A4FF)
)

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
    onLogoutClick: () -> Unit,
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
                        PrioritySection(interests = uiState.interests)
                        Spacer(modifier = Modifier.height(8.dp))
                        TimetableRegistrationRow()
                        Spacer(modifier = Modifier.height(29.dp))
                        EmptyShortcutSection(
                            title = "찜 목록",
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Bookmark,
                                    contentDescription = null,
                                    tint = MutedIconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            emptyTitle = "아직 찜한 행사가 없습니다.",
                            emptyDescription = "관심있는 행사를 찜해보세요."
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        EmptyShortcutSection(
                            title = "메모 목록",
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = MutedIconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            emptyTitle = "아직 메모가 없습니다.",
                            emptyDescription = "관심있는 행사에 메모를 작성해보세요."
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(17.dp))
                        BugReportSection()
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

// 우선순위 목록 영역
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
                        focusedBorderColor = BorderColor,
                        unfocusedBorderColor = BorderColor,
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
private fun PrioritySection(interests: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = MutedIconColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "행사 보기 우선순위",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink100
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                interests.take(4).forEachIndexed { index, interest ->
                    PriorityChip(
                        text = "${index + 1}순위: $interest",
                        color = PriorityChipColors[index % PriorityChipColors.size]
                    )
                }
                if (interests.isEmpty()) {
                    PriorityChip(
                        text = "우선순위를 설정해보세요",
                        color = Color(0xFFE8E8E8)
                    )
                }
            }
        }
    }
}

// 우선순위 알약 영역
@Composable
private fun PriorityChip(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color)
            .padding(horizontal = 11.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// 시간표 등록 영역
@Composable
private fun TimetableRegistrationRow() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AddCircleOutline,
                contentDescription = null,
                tint = MutedIconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "내 시간표 등록하기",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 빈 목록 안내 영역
@Composable
private fun EmptyShortcutSection(
    title: String,
    icon: @Composable () -> Unit,
    emptyTitle: String,
    emptyDescription: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(3.dp))
            icon()
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "$title 이동",
                tint = MutedIconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "$emptyTitle\n$emptyDescription",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

// 버그 신고 영역
@Composable
private fun BugReportSection() {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.BugReport,
                contentDescription = null,
                tint = Ink100,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "버그 신고",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "사용 중 불편한 문제를 알려주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ink60,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        BugTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = "제목",
            minLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))
        BugTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = "문제가 발생한 상황을 자세히 적어주세요.",
            minLines = 5
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {},
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A4A4A),
                    contentColor = PureWhite
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 15.dp,
                    vertical = 0.dp
                ),
                modifier = Modifier.height(26.dp)
            ) {
                Text(
                    text = "신고하기",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 버그 입력칸 영역
@Composable
private fun BugTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int
) {
    val shape = RoundedCornerShape(5.dp)
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = Ink100,
        fontSize = 11.sp
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (minLines == 1) 34.dp else 86.dp)
            .background(PureWhite, shape)
            .border(1.dp, BorderColor, shape),
        minLines = minLines,
        maxLines = if (minLines == 1) 1 else Int.MAX_VALUE,
        textStyle = textStyle,
        cursorBrush = SolidColor(Ink100),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle,
                        color = Color(0xFF9D9D9D)
                    )
                }
                innerTextField()
            }
        }
    )
}

// 계정 작업 영역
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

// 구분선 영역
@Composable
private fun Divider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerColor)
    )
}
