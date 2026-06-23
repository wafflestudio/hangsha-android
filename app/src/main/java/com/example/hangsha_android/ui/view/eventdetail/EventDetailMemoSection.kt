package com.example.hangsha_android.ui.view.eventdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.ui.theme.Cream10
import com.example.hangsha_android.ui.theme.Ink60
import com.example.hangsha_android.ui.theme.Ink90
import com.example.hangsha_android.ui.theme.Ink100
import com.example.hangsha_android.ui.theme.PureWhite

@Composable
fun EventDetailMemoSection(
    isOpen: Boolean,
    savedMemo: EventDetailMemo?,
    content: String,
    tagInput: String,
    tagNames: List<String>,
    isSaving: Boolean,
    onOpen: () -> Unit,
    onContentChanged: (String) -> Unit,
    onTagInputChanged: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    if (!isOpen) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MemoSectionHeader()
            if (savedMemo == null) {
                MemoPlaceholder()
            } else {
                MemoDisplay(
                    content = savedMemo.content,
                    tagNames = savedMemo.tagNames
                )
            }
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = Ink60,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isSaving) "저장 중" else "저장하기",
                    modifier = Modifier
                        .clickable(enabled = !isSaving, onClick = onSaveClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    color = Ink90,
                    fontWeight = FontWeight.SemiBold
                )
            }

            MemoTextInput(
                value = content,
                onValueChange = onContentChanged,
                placeholder = "메모를 입력하세요",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(102.dp)
            )

            if (tagNames.isNotEmpty()) {
                MemoTagRow(
                    tagNames = tagNames,
                    onRemoveTag = onRemoveTag
                )
            }

            // TODO: Connect tag creation/update API for existing memos when the backend contract is ready.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemoTextInput(
                    value = tagInput,
                    onValueChange = onTagInputChanged,
                    placeholder = "태그를 입력하세요",
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    singleLine = true
                )
                Button(
                    onClick = onAddTag,
                    enabled = tagInput.isNotBlank() && !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = Ink90,
                        disabledContainerColor = PureWhite,
                        disabledContentColor = Ink60.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
                ) {
                    Text(text = "추가")
                }
            }
        }
    }
}

@Composable
private fun MemoSectionHeader() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = null,
            tint = Ink60,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "메모하기",
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = Ink90
        )
    }
}

@Composable
private fun MemoPlaceholder() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
    ) {
        Text(
            text = "메모를 입력하세요",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 15.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = Ink60.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun MemoDisplay(
    content: String,
    tagNames: List<String>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, Ink60.copy(alpha = 0.24f))
        ) {
            Text(
                text = content,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = Ink100
            )
        }
        if (tagNames.isNotEmpty()) {
            MemoTagRow(tagNames = tagNames, onRemoveTag = null)
        }
    }
}

@Composable
private fun MemoTagRow(
    tagNames: List<String>,
    onRemoveTag: ((String) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tagNames.forEach { tagName ->
            MemoTagChip(
                text = tagName,
                onClick = onRemoveTag?.let { remove -> { remove(tagName) } }
            )
        }
    }
}

@Composable
private fun MemoTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Ink60.copy(alpha = 0.28f))
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Ink100
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = Ink60.copy(alpha = 0.55f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun MemoTagChip(
    text: String,
    onClick: (() -> Unit)? = null
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$text",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = Ink90
            )
            if (onClick != null) {
                Text(
                    text = "x",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp,
                    color = Ink60
                )
            }
        }
    }

    if (onClick == null) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Cream10,
            border = BorderStroke(1.dp, Ink60.copy(alpha = 0.18f)),
            content = content
        )
    } else {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp),
            color = Cream10,
            border = BorderStroke(1.dp, Ink60.copy(alpha = 0.18f)),
            content = content
        )
    }
}
