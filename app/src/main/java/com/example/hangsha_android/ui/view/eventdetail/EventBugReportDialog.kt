package com.example.hangsha_android.ui.view.eventdetail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.view.mypage.BugReportSection

@Composable
internal fun EventBugReportButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Rounded.BugReport,
            contentDescription = null,
            tint = Ink60
        )
        Text(
            text = "행사 정보 오류 제보하기",
            color = Ink60,
            textDecoration = TextDecoration.Underline
        )
    }
}

@Composable
internal fun EventBugReportDialog(
    uiState: EventDetailUiState,
    onDismissRequest: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onSubmitClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!uiState.isSubmittingBugReport) onDismissRequest()
        },
        title = { Text(text = "행사 정보 오류 제보") },
        text = {
            BugReportSection(
                title = uiState.bugReportTitle,
                content = uiState.bugReportContent,
                isSubmitting = uiState.isSubmittingBugReport,
                onTitleChanged = onTitleChanged,
                onContentChanged = onContentChanged,
                onSubmitClick = onSubmitClick
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !uiState.isSubmittingBugReport
            ) {
                Text(text = "취소")
            }
        }
    )
}
