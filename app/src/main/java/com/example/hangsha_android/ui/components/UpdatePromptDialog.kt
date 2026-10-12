package com.example.hangsha_android.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hangsha_android.R
import com.example.hangsha_android.ui.theme.HangshaTheme

/** 업데이트 가능 여부와 설치 실행은 호출하는 쪽에서 결정한다. */
@Composable
fun UpdatePromptDialog(
    isRequired: Boolean,
    latestVersionName: String,
    releaseNotes: String,
    onUpdateClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { if (!isRequired) onDismissRequest() },
        properties = DialogProperties(
            dismissOnBackPress = !isRequired,
            dismissOnClickOutside = !isRequired
        )
    ) {
        UpdatePromptCard(
            isRequired = isRequired,
            latestVersionName = latestVersionName,
            releaseNotes = releaseNotes,
            onUpdateClick = onUpdateClick,
            onDismissRequest = onDismissRequest,
            modifier = modifier
        )
    }
}

@Composable
private fun UpdatePromptCard(
    isRequired: Boolean,
    latestVersionName: String,
    releaseNotes: String,
    onUpdateClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 400.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(start = 22.dp, end = 18.dp, top = 14.dp, bottom = 16.dp)) {
            UpdateCardHeader(onCloseClick = if (isRequired) null else onDismissRequest)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isRequired) "행샤 업데이트가 필요해요" else "새로운 행샤가 나왔어요",
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (latestVersionName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "버전 %s".format(latestVersionName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = if (isRequired) {
                    "계속 이용하려면 최신 버전으로 업데이트해 주세요."
                } else {
                    "새 버전을 사용할 수 있어요. 지금 업데이트해 보세요."
                },
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (releaseNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "업데이트 내용",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = releaseNotes,
                    modifier = Modifier
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onUpdateClick) {
                    Text(
                        text = "업데이트",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateCardHeader(onCloseClick: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(66.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        if (onCloseClick != null) {
            IconButton(onClick = onCloseClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "닫기",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "업데이트 안내", showBackground = true, widthDp = 390, heightDp = 720)
@Composable
private fun OptionalUpdatePromptPreview() {
    HangshaTheme(darkTheme = false) {
        UpdatePromptPreviewContent(
            isRequired = false,
            releaseNotes = "캘린더 성능을 개선하고 일부 오류를 수정했습니다.\n" +
                "일정 탐색이 더 부드러워졌습니다."
        )
    }
}

@Preview(
    name = "필수 업데이트 · 다크 모드",
    showBackground = true,
    widthDp = 390,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RequiredUpdatePromptPreview() {
    HangshaTheme(darkTheme = true) {
        UpdatePromptPreviewContent(
            isRequired = true,
            releaseNotes = "캘린더 성능을 개선하고 일부 오류를 수정했습니다.\n" +
                "일정 탐색이 더 부드러워졌습니다."
        )
    }
}

@Preview(name = "긴 업데이트 내용", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun LongReleaseNotesUpdatePromptPreview() {
    HangshaTheme(darkTheme = false) {
        UpdatePromptPreviewContent(
            isRequired = false,
            releaseNotes = "캘린더 성능을 개선했습니다.\n" +
                "일부 기기에서 발생하던 표시 오류를 수정했습니다.\n" +
                "업데이트 안내 화면의 가독성을 개선했습니다.\n" +
                "기타 안정성 문제를 수정했습니다."
        )
    }
}

@Composable
private fun UpdatePromptPreviewContent(
    isRequired: Boolean,
    releaseNotes: String
) {
    PreviewScrim {
        UpdatePromptCard(
            isRequired = isRequired,
            latestVersionName = "1.4.0",
            releaseNotes = releaseNotes,
            onUpdateClick = {},
            onDismissRequest = {},
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun PreviewScrim(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "행샤",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.TopStart).padding(24.dp)
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.48f)))
        content()
    }
}
