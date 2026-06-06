package com.example.hangsha_android.ui.view.mypage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite

private val ProfileAvatarColor = Color(0xFF87959E)
private val BorderColor = Color(0xFFCACACA)
private val MutedIconColor = Color(0xFF9B9B9B)

// 프로필 헤더 영역
@Composable
internal fun ProfileHeader(
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

// 프로필 이미지 영역
@Composable
private fun EditableProfileAvatar(
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

// 이미지 표시 판정
private fun Any?.hasRealProfileImage(): Boolean {
    return when (this) {
        is Uri -> true
        is String -> isRealProfileImageUrl()
        else -> false
    }
}

// 사용자 이미지 판정
private fun Any?.isUserProfileImage(): Boolean {
    return when (this) {
        is Uri -> true
        is String -> isRealProfileImageUrl() && !isDefaultProfileImageUrl()
        else -> false
    }
}

// 실제 이미지 URL 판정
private fun String?.isRealProfileImageUrl(): Boolean {
    val normalized = this?.trim().orEmpty()
    return normalized.isNotBlank() &&
        !normalized.equals("null", ignoreCase = true) &&
        (normalized.startsWith("http://") ||
            normalized.startsWith("https://") ||
            normalized.startsWith("content://") ||
            normalized.startsWith("file://"))
}

// 기본 이미지 URL 판정
private fun String?.isDefaultProfileImageUrl(): Boolean {
    val normalized = this?.trim().orEmpty()
    return normalized.contains("/default/43513b43-2f84-4f0f-8de8-7d61120fe3aa.png")
}
