package com.example.hangsha_android.ui.view.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.window.Dialog
import java.time.Instant
import kotlinx.coroutines.delay

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
    onPrivacyPolicyAgreementChanged: (Boolean) -> Unit,
    onPasswordConfirmationChanged: (String) -> Unit,
    onVerificationCodeChanged: (String) -> Unit,
    onSendVerificationCodeClick: () -> Unit,
    onVerifyVerificationCodeClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    val passwordErrors = passwordValidationErrors(uiState.password)
    val hasPasswordError = passwordErrors.isNotEmpty()
    val hasPasswordConfirmationError = uiState.passwordConfirmation.isNotEmpty() &&
        uiState.password != uiState.passwordConfirmation
    var isPrivacyPolicyDetailsVisible by rememberSaveable { mutableStateOf(false) }

    if (uiState.signupToken.isNullOrBlank()) {
        EmailVerificationContent(
            uiState = uiState,
            onEmailChanged = onEmailChanged,
            onVerificationCodeChanged = onVerificationCodeChanged,
            onSendVerificationCodeClick = onSendVerificationCodeClick,
            onVerifyVerificationCodeClick = onVerifyVerificationCodeClick
        )
        return
    }

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
            Spacer(modifier = Modifier.height(14.dp))
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
            Spacer(modifier = Modifier.height(if (passwordErrors.isEmpty()) 14.dp else 20.dp))
            SignUpTextField(
                value = uiState.passwordConfirmation,
                onValueChange = onPasswordConfirmationChanged,
                placeholder = "비밀번호 확인",
                isError = hasPasswordConfirmationError,
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation()
            )
            if (hasPasswordConfirmationError) {
                Spacer(modifier = Modifier.height(12.dp))
                SignUpErrorPill(text = "비밀번호가 일치하지 않습니다.")
            }
            Spacer(modifier = Modifier.height(30.dp))
            SignUpPrivacyPolicyAgreement(
                isAgreed = uiState.isPrivacyPolicyAgreed,
                onAgreementChanged = onPrivacyPolicyAgreementChanged,
                onShowDetails = { isPrivacyPolicyDetailsVisible = true }
            )
            Spacer(modifier = Modifier.height(22.dp))
            SignUpSubmitButton(
                onClick = onSignUpClick,
                isLoading = uiState.isSignUpLoading
            )
        }
        if (isPrivacyPolicyDetailsVisible) {
            PrivacyPolicyDialog(
                onDismissRequest = { isPrivacyPolicyDetailsVisible = false }
            )
        }
    }
}

@Composable
private fun EmailVerificationContent(
    uiState: SignUpUiState,
    onEmailChanged: (String) -> Unit,
    onVerificationCodeChanged: (String) -> Unit,
    onSendVerificationCodeClick: () -> Unit,
    onVerifyVerificationCodeClick: () -> Unit
) {
    val isCodeStep = uiState.verificationCodeExpiresAt != null
    val expiryMillis = remember(uiState.verificationCodeExpiresAt) {
        runCatching { Instant.parse(uiState.verificationCodeExpiresAt.orEmpty()).toEpochMilli() }.getOrDefault(0L)
    }
    var nowMillis by remember(uiState.verificationCodeExpiresAt) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(expiryMillis) {
        while (expiryMillis > System.currentTimeMillis()) {
            nowMillis = System.currentTimeMillis()
            delay(1_000)
        }
        nowMillis = System.currentTimeMillis()
    }
    var resendSeconds by rememberSaveable(uiState.verificationCodeExpiresAt) { mutableStateOf(if (isCodeStep) 60 else 0) }
    LaunchedEffect(isCodeStep) {
        while (isCodeStep && resendSeconds > 0) {
            delay(1_000)
            resendSeconds -= 1
        }
    }
    val remainingSeconds = ((expiryMillis - nowMillis).coerceAtLeast(0L) / 1_000L).toInt()
    val errorMessage = uiState.signUpMessage

    Box(modifier = Modifier.fillMaxSize().background(SignUpWhite), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.width(SignUpContentWidth).offset(y = (-5).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "\uACC4\uC815 \uC0DD\uC131", color = SignUpBlack, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "\uC774\uBA54\uC77C\uACFC \uBE44\uBC00\uBC88\uD638\uB97C \uC124\uC815\uD574\uC8FC\uC138\uC694", color = SignUpBlack, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(if (isCodeStep) 25.dp else 46.dp))
            SignUpTextField(value = uiState.email, onValueChange = onEmailChanged, placeholder = "email@snu.ac.kr", keyboardType = KeyboardType.Email, readOnly = isCodeStep, isError = errorMessage != null)
            if (!isCodeStep) {
                Spacer(modifier = Modifier.height(20.dp))
                VerificationButton(text = "\uC774\uBA54\uC77C \uC778\uC99D", onClick = onSendVerificationCodeClick, isLoading = uiState.isVerificationCodeSending)
                errorMessage?.let { InlineVerificationError(it) }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                VerificationButton(text = if (resendSeconds > 0) "\uC778\uC99D\uBC88\uD638 \uB2E4\uC2DC \uBC1B\uAE30 (${resendSeconds}\uCD08)" else "\uC778\uC99D\uBC88\uD638 \uB2E4\uC2DC \uBC1B\uAE30", onClick = onSendVerificationCodeClick, enabled = resendSeconds == 0 && !uiState.isVerificationCodeSending, isLoading = uiState.isVerificationCodeSending, color = if (resendSeconds == 0) SignUpBlack else Color(0xFF777777))
                Spacer(modifier = Modifier.height(25.dp))
                Text(text = "\uC774\uBA54\uC77C\uB85C \uC628 \uC778\uC99D\uBC88\uD638\uB97C \uD655\uC778\uD574\uC8FC\uC138\uC694!", color = SignUpBlack, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60), color = SignUpError, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                VerificationCodeBoxes(code = uiState.verificationCode, onCodeChanged = onVerificationCodeChanged, isError = errorMessage != null)
                errorMessage?.let { InlineVerificationError(it) }
                Spacer(modifier = Modifier.height(22.dp))
                VerificationButton(text = "\uC778\uC99D\uD558\uAE30", onClick = onVerifyVerificationCodeClick, enabled = remainingSeconds > 0 && !uiState.isVerificationCodeVerifying, isLoading = uiState.isVerificationCodeVerifying)
            }
        }
    }
}

@Composable
private fun VerificationCodeBoxes(code: String, onCodeChanged: (String) -> Unit, isError: Boolean) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BasicTextField(
        value = code,
        onValueChange = { input -> onCodeChanged(input.filter(Char::isDigit).take(6)) },
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        singleLine = true,
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = { innerTextField ->
            Box {
                innerTextField()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                ) {
                    repeat(6) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .background(SignUpWhite, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isError) SignUpError else SignUpBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = code.getOrNull(index)?.toString().orEmpty(),
                                color = SignUpBlack,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun VerificationButton(text: String, onClick: () -> Unit, enabled: Boolean = true, isLoading: Boolean = false, color: Color = SignUpBlack) {
    Surface(onClick = onClick, enabled = enabled && !isLoading, modifier = Modifier.fillMaxWidth().height(SignUpButtonHeight), shape = SignUpRoundShape, color = color, contentColor = SignUpWhite) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (isLoading) SignUpProgressIndicator(16.dp) else Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun InlineVerificationError(message: String) {
    Spacer(modifier = Modifier.height(10.dp))
    Text(text = message, color = SignUpError, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SignUpPrivacyPolicyAgreement(
    isAgreed: Boolean,
    onAgreementChanged: (Boolean) -> Unit,
    onShowDetails: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = isAgreed,
            onCheckedChange = onAgreementChanged,
            modifier = Modifier.size(24.dp),
            colors = CheckboxDefaults.colors(checkedColor = SignUpBlack, uncheckedColor = SignUpPlaceholder, checkmarkColor = SignUpWhite)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "개인정보 약관 동의(필수)", color = SignUpBlack, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = "상세 내용 보기", color = SignUpPlaceholder, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onShowDetails))
    }
}

@Composable
private fun PrivacyPolicyDialog(onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
            shape = RoundedCornerShape(20.dp),
            color = SignUpWhite
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text("개인정보 수집·이용 동의", color = SignUpBlack, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(20.dp))
                PrivacyPolicySectionTitle("개인정보 수집 및 이용에 대한 동의")
                Spacer(modifier = Modifier.height(8.dp))
                PrivacyPolicyBody("서비스는 회원가입 및 서비스 제공을 위해 아래와 같이 개인정보를 수집·이용합니다.")
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    PrivacyPolicyTableRow("수집 항목", "수집 목적", "보유 및 이용 기간", true)
                    PrivacyPolicyTableRow("이메일 주소", "회원 식별, 로그인, 계정 관리, 서비스 이용 안내", "회원 탈퇴 시까지 (단, 관계 법령에 따라 보관이 필요한 경우 해당 기간 동안 보관)")
                    PrivacyPolicyTableRow("비밀번호(암호화 저장)", "회원 인증 및 계정 보호", "회원 탈퇴 시까지")
                    PrivacyPolicyTableRow("닉네임(선택)", "서비스 내 사용자 식별", "회원 탈퇴 시까지")
                }
                Spacer(modifier = Modifier.height(20.dp))
                PrivacyPolicySectionTitle("수집 목적")
                Spacer(modifier = Modifier.height(8.dp))
                PrivacyPolicyBullet("회원가입 및 본인 식별")
                PrivacyPolicyBullet("로그인 및 계정 관리")
                PrivacyPolicyBullet("서비스 제공 및 운영")
                Spacer(modifier = Modifier.height(20.dp))
                PrivacyPolicySectionTitle("보유 및 이용 기간")
                Spacer(modifier = Modifier.height(8.dp))
                PrivacyPolicyBody("원칙적으로 회원 탈퇴 시 개인정보를 지체 없이 파기합니다.")
                Spacer(modifier = Modifier.height(12.dp))
                PrivacyPolicyBody("다만, 다음의 경우 관련 법령에 따라 일정 기간 보관할 수 있습니다.")
                Spacer(modifier = Modifier.height(8.dp))
                PrivacyPolicyBullet("계약 또는 청약철회 등에 관한 기록: 5년")
                PrivacyPolicyBullet("소비자의 불만 또는 분쟁처리에 관한 기록: 3년")
                PrivacyPolicyBullet("전자적 접속기록: 관련 법령에 따른 보관 기간")
                Spacer(modifier = Modifier.height(16.dp))
                PrivacyPolicyBody("※ 귀하는 개인정보 수집·이용에 대한 동의를 거부할 권리가 있습니다. 다만, 필수 항목에 대한 동의를 거부하는 경우 회원가입 및 서비스 이용이 제한될 수 있습니다.")
                Spacer(modifier = Modifier.height(20.dp))
                Surface(onClick = onDismissRequest, modifier = Modifier.fillMaxWidth().height(40.dp), shape = SignUpRoundShape, color = SignUpBlack, contentColor = SignUpWhite) {
                    Box(contentAlignment = Alignment.Center) { Text("확인", fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicySectionTitle(text: String) {
    Text(text = text, color = SignUpBlack, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun PrivacyPolicyBody(text: String) {
    Text(text = text, color = SignUpBlack, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun PrivacyPolicyBullet(text: String) {
    PrivacyPolicyBody("• $text")
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun PrivacyPolicyTableRow(item: String, purpose: String, retentionPeriod: String, isHeader: Boolean = false) {
    val backgroundColor = if (isHeader) Color(0xFFF3F3F3) else SignUpWhite
    val fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(backgroundColor)
    ) {
        PrivacyPolicyTableCell(item, 0.27f, fontWeight)
        PrivacyPolicyTableCell(purpose, 0.36f, fontWeight)
        PrivacyPolicyTableCell(retentionPeriod, 0.37f, fontWeight)
    }
}

@Composable
private fun RowScope.PrivacyPolicyTableCell(text: String, weight: Float, fontWeight: FontWeight) {
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .border(0.5.dp, SignUpBorder)
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = SignUpBlack, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = fontWeight)
    }
}
@Composable
private fun SignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
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
            readOnly = readOnly,
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
