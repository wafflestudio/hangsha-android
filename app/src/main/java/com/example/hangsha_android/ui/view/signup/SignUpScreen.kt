package com.example.hangsha_android.ui.view.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SignUpContentWidth = 280.dp
private val SignUpFieldHeight = 37.dp
private val SignUpButtonHeight = 37.dp
private val SignUpRoundShape = CircleShape
private val SignUpBlack = Color(0xFF000000)
private val SignUpWhite = Color(0xFFFFFFFF)
private val SignUpBorder = Color(0xFFE0E0E0)
private val SignUpPlaceholder = Color(0xFF8F8F8F)
private val SignUpError = Color(0xFFFF4058)
private val SignUpErrorContainer = Color(0xFFFFD8DE)

@Composable
fun SignUpScreen(
    uiState: SignUpUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordConfirmationChanged: (String) -> Unit,
    onSignUpClick: () -> Unit
) {
    val passwordErrors = passwordValidationErrors(uiState.password)
    val hasPasswordError = passwordErrors.isNotEmpty()
    val hasPasswordConfirmationError = uiState.passwordConfirmation.isNotEmpty() &&
        uiState.password != uiState.passwordConfirmation

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SignUpWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(SignUpContentWidth)
                .offset(y = (-5).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "계정 생성",
                color = SignUpBlack,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "이메일과 비밀번호를 설정해주세요",
                color = SignUpBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(46.dp))
            SignUpTextField(
                value = uiState.email,
                onValueChange = onEmailChanged,
                placeholder = "email@snu.ac.kr",
                keyboardType = KeyboardType.Email
            )
            Spacer(modifier = Modifier.height(22.dp))
            SignUpTextField(
                value = uiState.password,
                onValueChange = onPasswordChanged,
                placeholder = "비밀번호",
                isError = hasPasswordError,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (hasPasswordError) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                }
            )
            passwordErrors.forEach { error ->
                Spacer(modifier = Modifier.height(12.dp))
                SignUpErrorPill(text = error)
            }
            Spacer(modifier = Modifier.height(if (passwordErrors.isEmpty()) 22.dp else 20.dp))
            SignUpTextField(
                value = uiState.passwordConfirmation,
                onValueChange = onPasswordConfirmationChanged,
                placeholder = "비밀번호 확인",
                isError = hasPasswordConfirmationError,
                keyboardType = KeyboardType.Password,
                visualTransformation = VisualTransformation.None
            )
            if (hasPasswordConfirmationError) {
                Spacer(modifier = Modifier.height(12.dp))
                SignUpErrorPill(text = "비밀번호가 일치하지 않습니다.")
            }
            Spacer(modifier = Modifier.height(22.dp))
            SignUpSubmitButton(
                onClick = onSignUpClick,
                isLoading = uiState.isSignUpLoading
            )
        }
    }
}

@Composable
private fun SignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val borderColor = if (isError) SignUpError else SignUpBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SignUpFieldHeight)
            .background(SignUpWhite, SignUpRoundShape)
            .border(1.dp, borderColor, SignUpRoundShape)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                color = SignUpBlack,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            ),
            cursorBrush = SolidColor(SignUpBlack),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        color = SignUpPlaceholder,
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
private fun SignUpErrorPill(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(37.dp)
            .background(SignUpErrorContainer, SignUpRoundShape)
            .border(1.dp, SignUpError, SignUpRoundShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .border(2.dp, SignUpError, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                color = SignUpError,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        Text(
            text = text,
            color = SignUpBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        )
    }
}

@Composable
private fun SignUpSubmitButton(
    onClick: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(SignUpButtonHeight),
        enabled = !isLoading,
        shape = SignUpRoundShape,
        color = SignUpBlack,
        contentColor = SignUpWhite
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                SignUpProgressIndicator(size = 16.dp)
            } else {
                Text(
                    text = "계정 생성",
                    color = SignUpWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun SignUpProgressIndicator(size: Dp) {
    CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = SignUpWhite,
        strokeWidth = 2.dp
    )
}

private fun passwordValidationErrors(password: String): List<String> {
    if (password.isEmpty()) {
        return emptyList()
    }

    return buildList {
        if (password.length < 8) {
            add("8자 이상이어야 합니다.")
        }
        if (!password.hasRequiredPasswordCharacters()) {
            add("영문, 숫자, 특수문자를 포함해 주세요.")
        }
        if (password.any(Char::isWhitespace)) {
            add("공백은 사용할 수 없습니다.")
        }
    }
}

private fun String.hasRequiredPasswordCharacters(): Boolean {
    val hasLetter = any(Char::isLetter)
    val hasDigit = any(Char::isDigit)
    val hasSpecial = any { !it.isLetterOrDigit() && !it.isWhitespace() }

    return hasLetter && hasDigit && hasSpecial
}
