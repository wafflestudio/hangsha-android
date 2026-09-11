package com.example.hangsha_android.ui.view.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.hangsha_android.R

private val OnboardingContentWidth = 280.dp
private val OnboardingAvatarContainerSize = 190.dp
private val OnboardingAvatarImageSize = 184.dp
private val OnboardingDeleteButtonSize = 42.dp
private val OnboardingCameraButtonSize = 46.dp
private val OnboardingFieldHeight = 37.dp
private val OnboardingButtonHeight = 37.dp
private val OnboardingRoundShape = CircleShape

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onUsernameChanged: (String) -> Unit,
    onProfileImageSelected: (Uri) -> Unit,
    onProfileImageDeleted: () -> Unit,
    onContinueClick: () -> Unit
) {
    val profileImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onProfileImageSelected(uri)
        }
    }
    val profileImageModel = uiState.profileImageUri ?: uiState.profileImageUrl

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(OnboardingContentWidth)
                .offset(y = (-8).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "프로필 설정",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "프로필 사진과 이름을 설정해주세요",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(46.dp))
            OnboardingProfileImage(
                profileImageModel = profileImageModel,
                onPickImageClick = { profileImagePicker.launch("image/*") },
                onDeleteImageClick = onProfileImageDeleted
            )
            Spacer(modifier = Modifier.height(18.dp))
            OnboardingTextField(
                value = uiState.username,
                onValueChange = onUsernameChanged,
                placeholder = "닉네임",
                isError = uiState.usernameErrorMessage != null
            )
            uiState.usernameErrorMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            OnboardingSubmitButton(
                onClick = onContinueClick,
                enabled = uiState.isSubmitEnabled,
                isLoading = uiState.isSavingProfile
            )
        }
    }
}

@Composable
private fun OnboardingProfileImage(
    profileImageModel: Any?,
    onPickImageClick: () -> Unit,
    onDeleteImageClick: () -> Unit
) {
    val hasProfileImage = profileImageModel.hasRealProfileImage()
    val imageModel = if (hasProfileImage) profileImageModel else R.drawable.profile_null_img
    val canDeleteProfileImage = profileImageModel.isUserProfileImage()

    Box(modifier = Modifier.size(OnboardingAvatarContainerSize)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(OnboardingAvatarImageSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = "프로필 이미지",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (canDeleteProfileImage) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(OnboardingDeleteButtonSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onDeleteImageClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "프로필 이미지 삭제",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(OnboardingCameraButtonSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable(onClick = onPickImageClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoCamera,
                contentDescription = "프로필 이미지 선택",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean
) {
    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(OnboardingFieldHeight)
            .background(MaterialTheme.colorScheme.surface, OnboardingRoundShape)
            .border(1.dp, borderColor, OnboardingRoundShape)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun OnboardingSubmitButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(OnboardingButtonHeight),
        enabled = enabled,
        shape = OnboardingRoundShape,
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                OnboardingProgressIndicator(size = 16.dp)
            } else {
                Text(
                    text = "프로필 설정하기",
                    color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingProgressIndicator(size: Dp) {
    CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = MaterialTheme.colorScheme.onPrimary,
        strokeWidth = 2.dp
    )
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
