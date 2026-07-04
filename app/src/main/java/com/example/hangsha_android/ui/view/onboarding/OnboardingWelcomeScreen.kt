package com.example.hangsha_android.ui.view.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingWelcomeScreen(
    onMyPageClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFBFEFF8),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        WelcomeStars()

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-26).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "환영합니다!",
                color = Color(0xFF000000),
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(46.dp))
            WelcomeActionButton(
                text = "마이페이지",
                width = 138.dp,
                onClick = onMyPageClick
            )
            Spacer(modifier = Modifier.height(9.dp))
            WelcomeActionButton(
                text = "캘린더로 가기",
                width = 166.dp,
                onClick = onCalendarClick
            )
        }
    }
}

@Composable
private fun WelcomeStars() {
    val stars = listOf(
        WelcomeStar(x = 0.25f, y = 0.28f, radiusDp = 30f, filled = true, alpha = 0.92f),
        WelcomeStar(x = 0.77f, y = 0.49f, radiusDp = 40f, filled = false, alpha = 0.55f),
        WelcomeStar(x = 0.77f, y = 0.33f, radiusDp = 21f, filled = false, alpha = 0.75f),
        WelcomeStar(x = 0.49f, y = 0.56f, radiusDp = 22f, filled = true, alpha = 0.86f),
        WelcomeStar(x = 0.57f, y = 0.20f, radiusDp = 12f, filled = true, alpha = 0.82f),
        WelcomeStar(x = 0.27f, y = 0.42f, radiusDp = 13f, filled = false, alpha = 0.40f),
        WelcomeStar(x = 0.06f, y = 0.46f, radiusDp = 20f, filled = false, alpha = 0.78f),
        WelcomeStar(x = 0.86f, y = 0.28f, radiusDp = 10f, filled = true, alpha = 0.95f),
        WelcomeStar(x = 0.16f, y = 0.67f, radiusDp = 12f, filled = false, alpha = 0.56f),
        WelcomeStar(x = 0.72f, y = 0.70f, radiusDp = 11f, filled = true, alpha = 0.82f)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEach { star ->
            drawWelcomeStar(star)
        }
    }
}

@Composable
private fun WelcomeActionButton(
    text: String,
    width: Dp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(width)
            .height(35.dp),
        shape = CircleShape,
        color = Color(0xFFFFFFFF),
        contentColor = Color(0xFF6B6B6B),
        shadowElevation = 5.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        }
    }
}

private fun DrawScope.drawWelcomeStar(star: WelcomeStar) {
    val center = Offset(size.width * star.x, size.height * star.y)
    val outerRadius = star.radiusDp.dp.toPx()
    val innerRadius = outerRadius * 0.42f
    val path = starPath(
        center = center,
        outerRadius = outerRadius,
        innerRadius = innerRadius,
        rotationDegrees = star.rotationDegrees
    )
    val color = Color.White.copy(alpha = star.alpha)

    if (star.filled) {
        drawPath(path = path, color = color, style = Fill)
    } else {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

private fun starPath(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    rotationDegrees: Float
): Path {
    val path = Path()
    val rotationRadians = rotationDegrees * PI / 180.0

    repeat(STAR_POINT_COUNT * 2) { index ->
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2.0 + rotationRadians + index * PI / STAR_POINT_COUNT
        val x = center.x + cos(angle).toFloat() * radius
        val y = center.y + sin(angle).toFloat() * radius

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    path.close()
    return path
}

private data class WelcomeStar(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val filled: Boolean,
    val alpha: Float,
    val rotationDegrees: Float = 0f
)

private const val STAR_POINT_COUNT = 5
