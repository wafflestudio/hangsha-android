package com.example.hangsha_android.ui.view.mypage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink90
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite

private val ProfileAvatarColor = Color(0xFF87959E)
private val DividerColor = Color(0xFFE7E7E7)
private val BorderColor = Color(0xFFCACACA)
private val MutedIconColor = Color(0xFF9B9B9B)
private val PriorityChipColors = listOf(
    Color(0xFF88D6F8),
    Color(0xFF83C9F4),
    Color(0xFFEAD452),
    Color(0xFFC6A4FF)
)

// 마이페이지 전체 화면 배치부
@Composable
fun MyPageScreen(
    uiState: MyPageUiState,
    onRetryClick: () -> Unit,
    onStartProfileEdit: () -> Unit,
    onDraftUsernameChanged: (String) -> Unit,
    onDraftProfileImageSelected: (Uri) -> Unit,
    onDraftProfileImageDeleted: () -> Unit,
    onSaveProfileEdit: () -> Unit
) {
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
                            title = "내 찜 목록",
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Bookmark,
                                    contentDescription = null,
                                    tint = MutedIconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            emptyTitle = "아직 찜된 행사가 없습니다.",
                            emptyDescription = "관심있는 행사를 찜해보세요!"
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        EmptyShortcutSection(
                            title = "내 메모 목록",
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = MutedIconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            emptyTitle = "아직 메모가 없습니다.",
                            emptyDescription = "다가온 행사나 관심있는 행사에 대한 메모를 작성해보세요!"
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
                            contentColor = PureWhite
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
                            borderColor = Color(0xFFFFA0A0)
                        )
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
    }
}

// 프로필 사진 이름 이메일 표시부
@Composable
private fun ProfileHeader(uiState: MyPageUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(
            username = uiState.username,
            profileImageUrl = uiState.profileImageUrl
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uiState.username.ifBlank { "사용자" },
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = uiState.email.ifBlank { "이메일 정보 없음" },
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = "프로필 수정",
            tint = MutedIconColor,
            modifier = Modifier
                .size(24.dp)
                .padding(3.dp)
        )
    }
}

// 프로필 이미지 또는 기본 글자 표시부
@Composable
private fun ProfileAvatar(
    username: String,
    profileImageUrl: String?
) {
    Box(
        modifier = Modifier
            .size(65.dp)
            .clip(CircleShape)
            .background(ProfileAvatarColor),
        contentAlignment = Alignment.Center
    ) {
        if (profileImageUrl.isRealProfileImageUrl()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "프로필 이미지",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// 관심 카테고리 우선순위 목록 표시부
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

// 우선순위 하나 알약 표시부
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

// 시간표 등록 버튼 줄 표시부
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

// 찜 목록 메모 목록 빈 상태 표시부
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

// 버그 신고 입력 영역 표시부
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
            text = "이용 중 불편한 문제를 알려주세요.",
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

// 버그 신고 텍스트 입력칸 표시부
@Composable
private fun BugTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9D9D9D),
                fontSize = 11.sp
            )
        },
        shape = RoundedCornerShape(5.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BorderColor,
            unfocusedBorderColor = BorderColor,
            focusedContainerColor = PureWhite,
            unfocusedContainerColor = PureWhite
        )
    )
}

// 로그아웃 회원탈퇴 같은 계정 버튼 표시부
@Composable
private fun AccountActionSection(
    title: String,
    description: String,
    buttonText: String,
    icon: @Composable () -> Unit,
    buttonColor: Color,
    contentColor: Color,
    borderColor: Color? = null
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
                .clickable(onClick = {})
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

// 마이페이지 로딩 실패 안내 표시부
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

// 얇은 구분선 표시부
@Composable
private fun Divider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerColor)
    )
}

@Composable
private fun ProfileHeader(
    uiState: MyPageUiState,
    onStartProfileEdit: () -> Unit,
    onDraftUsernameChanged: (String) -> Unit,
    onDraftProfileImageSelected: (Uri) -> Unit,
    onDraftProfileImageDeleted: () -> Unit,
    onSaveProfileEdit: () -> Unit
) {
    var showDeleteProfileImageDialog by remember { mutableStateOf(false) }
    val profileImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onDraftProfileImageSelected(uri)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditableProfileAvatar(
            username = if (uiState.isEditingProfile) uiState.draftUsername else uiState.username,
            profileImageModel = when {
                uiState.isEditingProfile && uiState.draftProfileImageUri != null ->
                    uiState.draftProfileImageUri
                uiState.isEditingProfile -> uiState.draftProfileImageUrl
                else -> uiState.profileImageUrl
            },
            isEditing = uiState.isEditingProfile,
            onPickImageClick = { profileImagePicker.launch("image/*") },
            onDeleteImageClick = { showDeleteProfileImageDialog = true }
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (uiState.isEditingProfile) {
                OutlinedTextField(
                    value = uiState.draftUsername,
                    onValueChange = onDraftUsernameChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    shape = RoundedCornerShape(5.dp),
                    isError = uiState.usernameErrorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BorderColor,
                        unfocusedBorderColor = BorderColor,
                        errorBorderColor = Color(0xFFFF4B4B),
                        focusedContainerColor = PureWhite,
                        unfocusedContainerColor = PureWhite,
                        errorContainerColor = PureWhite
                    )
                )
                if (uiState.usernameErrorMessage != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = uiState.usernameErrorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFF4B4B),
                        fontSize = 10.sp
                    )
                }
                if (uiState.profileSaveErrorMessage != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = uiState.profileSaveErrorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFF4B4B),
                        fontSize = 10.sp
                    )
                }
            } else {
                Text(
                    text = uiState.username.ifBlank { "사용자" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink100,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = uiState.email.ifBlank { "이메일 정보 없음" },
                style = MaterialTheme.typography.bodyMedium,
                color = Ink100,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = {
                if (uiState.isEditingProfile) {
                    onSaveProfileEdit()
                } else {
                    onStartProfileEdit()
                }
            },
            enabled = !uiState.isSavingProfile,
            modifier = Modifier
                .size(30.dp)
                .padding(3.dp)
        ) {
            Icon(
                imageVector = if (uiState.isEditingProfile) Icons.Rounded.Check else Icons.Rounded.Edit,
                contentDescription = if (uiState.isEditingProfile) "프로필 저장" else "프로필 수정",
                tint = if (uiState.isEditingProfile) Color(0xFF2E7D32) else MutedIconColor
            )
        }
    }

    if (showDeleteProfileImageDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteProfileImageDialog = false },
            title = { Text(text = "프로필 이미지 삭제") },
            text = { Text(text = "정말 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteProfileImageDialog = false
                        onDraftProfileImageDeleted()
                    }
                ) {
                    Text(text = "확인", color = Color(0xFFFF4B4B))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteProfileImageDialog = false }) {
                    Text(text = "취소")
                }
            }
        )
    }
}

@Composable
private fun EditableProfileAvatar(
    username: String,
    profileImageModel: Any?,
    isEditing: Boolean,
    onPickImageClick: () -> Unit,
    onDeleteImageClick: () -> Unit
) {
    val hasProfileImage = profileImageModel.hasRealProfileImage()
    val canDeleteProfileImage = profileImageModel.isUserProfileImage()

    Box(modifier = Modifier.size(76.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(65.dp)
                .clip(CircleShape)
                .background(ProfileAvatarColor),
            contentAlignment = Alignment.Center
        ) {
            if (hasProfileImage) {
                AsyncImage(
                    model = profileImageModel,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (isEditing && canDeleteProfileImage) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF4B4B))
                    .clickable(onClick = onDeleteImageClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "프로필 이미지 삭제",
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (isEditing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(PureWhite)
                    .border(1.dp, BorderColor, CircleShape)
                    .clickable(onClick = onPickImageClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = "프로필 이미지 선택",
                    tint = MutedIconColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

private fun Any?.hasRealProfileImage(): Boolean {
    return when (this) {
        is Uri -> true
        is String -> isRealProfileImageUrl()
        else -> false
    }
}

private fun Any?.isUserProfileImage(): Boolean {
    return when (this) {
        is Uri -> true
        is String -> isRealProfileImageUrl() && !isDefaultProfileImageUrl()
        else -> false
    }
}

private fun String?.isRealProfileImageUrl(): Boolean {
    val normalized = this?.trim().orEmpty()
    return normalized.isNotBlank() &&
        !normalized.equals("null", ignoreCase = true) &&
        (normalized.startsWith("http://") ||
            normalized.startsWith("https://") ||
            normalized.startsWith("content://") ||
            normalized.startsWith("file://"))
}

private fun String?.isDefaultProfileImageUrl(): Boolean {
    val normalized = this?.trim().orEmpty()
    return normalized.contains("/default/43513b43-2f84-4f0f-8de8-7d61120fe3aa.png")
}
