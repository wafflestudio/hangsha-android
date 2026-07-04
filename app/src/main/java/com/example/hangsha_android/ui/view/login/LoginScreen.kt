package com.example.hangsha_android.ui.view.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hangsha_android.R

private val AuthContentWidth = 252.dp
private val AuthButtonHeight = 34.dp
private val AuthFieldHeight = 34.dp
private val AuthRoundShape = CircleShape
private val AuthBlack = Color(0xFF000000)
private val AuthWhite = Color(0xFFFFFFFF)
private val AuthBorder = Color(0xFFE0E0E0)
private val AuthPlaceholder = Color(0xFF8F8F8F)
private val AuthError = Color(0xFFFF4058)
private val AuthYellow = Color(0xFFFFD344)
private val AuthMuted = Color(0xFF777777)
private val KakaoYellow = Color(0xFFFFE812)
private val NaverGreen = Color(0xFF03C75A)

@Composable
fun OpeningScreen(
    loginUiState: LoginUiState,
    onEmailLoginClick: () -> Unit,
    onGoogleLoginClick: () -> Unit,
    onKakaoLoginClick: () -> Unit,
    onNaverLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onGuestContinueClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(AuthContentWidth)
                .offset(y = (-18).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HangshaBrand()
            Spacer(modifier = Modifier.height(36.dp))
            OpeningButton(
                text = "로그인",
                onClick = onEmailLoginClick,
                enabled = !loginUiState.isAnyLoginLoading
            )
            Spacer(modifier = Modifier.height(12.dp))
            OpeningButton(
                text = "구글 계정으로 계속하기",
                onClick = onGoogleLoginClick,
                enabled = !loginUiState.isAnyLoginLoading,
                isLoading = loginUiState.isGoogleLoginLoading,
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OpeningButton(
                text = "카카오톡 계정으로 계속하기",
                onClick = onKakaoLoginClick,
                enabled = !loginUiState.isAnyLoginLoading,
                leadingIcon = {
                    SocialBadge(
                        text = "TALK",
                        containerColor = AuthBlack,
                        contentColor = KakaoYellow,
                        fontSize = 6.sp
                    )
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OpeningButton(
                text = "네이버 계정으로 계속하기",
                onClick = onNaverLoginClick,
                enabled = !loginUiState.isAnyLoginLoading,
                leadingIcon = {
                    SocialBadge(
                        text = "N",
                        containerColor = NaverGreen,
                        contentColor = AuthWhite,
                        fontSize = 16.sp
                    )
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
            OpeningButton(
                text = "회원가입",
                onClick = onSignUpClick,
                enabled = !loginUiState.isAnyLoginLoading,
                containerColor = AuthYellow,
                contentColor = AuthBlack,
                borderColor = AuthYellow,
                shadowElevation = 2.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "로그인 없이 게스트로 계속",
                modifier = Modifier.clickable(
                    enabled = !loginUiState.isAnyLoginLoading,
                    onClick = onGuestContinueClick
                ),
                color = AuthMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center
            )
            loginUiState.loginMessage?.let { message ->
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                    color = AuthError,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    loginUiState: LoginUiState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(AuthContentWidth)
                .offset(y = (-8).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "로그인",
                color = AuthBlack,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(50.dp))
            AuthTextField(
                value = loginUiState.username,
                onValueChange = onUsernameChanged,
                placeholder = "이메일",
                enabled = !loginUiState.isAnyLoginLoading,
                keyboardType = KeyboardType.Email
            )
            Spacer(modifier = Modifier.height(12.dp))
            AuthTextField(
                value = loginUiState.password,
                onValueChange = onPasswordChanged,
                placeholder = "비밀번호",
                enabled = !loginUiState.isAnyLoginLoading,
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation()
            )
            loginUiState.loginMessage?.let { message ->
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                    color = AuthError,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))
            } ?: Spacer(modifier = Modifier.height(16.dp))
            SubmitButton(
                text = "로그인 하기",
                onClick = onLoginClick,
                enabled = !loginUiState.isAnyLoginLoading,
                isLoading = loginUiState.isCredentialLoginLoading
            )
        }
    }
}

@Composable
private fun HangshaBrand() {
    Image(
        painter = painterResource(id = R.drawable.logo_with_text),
        contentDescription = "행샤 로고",
        modifier = Modifier
            .width(170.dp)
            .height(86.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun OpeningButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    containerColor: Color = AuthWhite,
    contentColor: Color = AuthBlack,
    borderColor: Color = AuthBorder,
    shadowElevation: Dp = 2.dp,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(AuthButtonHeight),
        enabled = enabled && !isLoading,
        shape = AuthRoundShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                LoginProgressIndicator(size = 16.dp, color = contentColor)
            } else {
                leadingIcon?.let {
                    Box(
                        modifier = Modifier.align(Alignment.CenterStart),
                        contentAlignment = Alignment.Center
                    ) {
                        it()
                    }
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SocialBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    fontSize: TextUnit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AuthFieldHeight)
            .background(AuthWhite, AuthRoundShape)
            .border(1.dp, AuthBorder, AuthRoundShape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(
                color = AuthBlack,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            ),
            cursorBrush = SolidColor(AuthBlack),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        color = AuthPlaceholder,
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
private fun SubmitButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(AuthButtonHeight),
        enabled = enabled,
        shape = AuthRoundShape,
        color = AuthBlack,
        contentColor = AuthWhite
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                LoginProgressIndicator(size = 16.dp, color = AuthWhite)
            } else {
                Text(
                    text = text,
                    color = AuthWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp
                )
            }
        }
    }
}

@Composable
private fun LoginProgressIndicator(
    size: Dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = 2.dp
    )
}
