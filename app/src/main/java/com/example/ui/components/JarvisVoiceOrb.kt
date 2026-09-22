package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SkyGlow

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun JarvisVoiceOrb(
    state: OrbState = OrbState.IDLE,
    size: Dp = 80.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb")

    // Pulse animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (state == OrbState.THINKING) 0.85f else 0.95f,
        targetValue = if (state == OrbState.THINKING) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == OrbState.THINKING) 600 else 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Rotation animation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == OrbState.THINKING) 2500 else 9000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Core glow opacity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.85f * pulseScale

            // Outer energy aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ArcCyan.copy(alpha = 0.35f * glowAlpha),
                        ElectricBlue.copy(alpha = 0.15f * glowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.3f
                ),
                radius = baseRadius * 1.25f,
                center = center
            )

            // Rotating outer segmented arcs
            rotate(degrees = rotationAngle, pivot = center) {
                drawCircle(
                    color = ArcCyan.copy(alpha = 0.6f),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )

                // 4 satellite power ticks
                for (i in 0 until 4) {
                    val angle = Math.toRadians((i * 90.0))
                    val tickStart = Offset(
                        (center.x + (baseRadius - 4f) * Math.cos(angle)).toFloat(),
                        (center.y + (baseRadius - 4f) * Math.sin(angle)).toFloat()
                    )
                    val tickEnd = Offset(
                        (center.x + (baseRadius + 6f) * Math.cos(angle)).toFloat(),
                        (center.y + (baseRadius + 6f) * Math.sin(angle)).toFloat()
                    )
                    drawLine(
                        color = SkyGlow,
                        start = tickStart,
                        end = tickEnd,
                        strokeWidth = 3f
                    )
                }
            }

            // Counter-rotating inner ring
            rotate(degrees = -rotationAngle * 1.5f, pivot = center) {
                drawCircle(
                    color = ElectricBlue.copy(alpha = 0.7f),
                    radius = baseRadius * 0.65f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }

            // Center glowing core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        ArcCyan,
                        ElectricBlue.copy(alpha = 0.6f)
                    ),
                    center = center,
                    radius = baseRadius * 0.45f
                ),
                radius = baseRadius * 0.42f,
                center = center
            )
        }
    }
}
