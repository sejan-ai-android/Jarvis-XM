package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
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
    rmsDb: Float = 0f,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_arc_reactor")

    // Smooth responsive audio volume boost
    val animatedRms by animateFloatAsState(
        targetValue = if (state == OrbState.LISTENING || state == OrbState.SPEAKING) rmsDb.coerceIn(0f, 10f) else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
        label = "arc_rms_boost"
    )

    // Base pulse animation based on state
    val pulseDuration = when (state) {
        OrbState.THINKING -> 500
        OrbState.LISTENING -> 900
        OrbState.SPEAKING -> 800
        OrbState.IDLE -> 2400
    }

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (state == OrbState.THINKING) 0.88f else 0.96f,
        targetValue = if (state == OrbState.THINKING) 1.14f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Primary rotation speed: spins rapidly when thinking, fluidly when speaking/listening, gently when idle
    val rotationDuration = when (state) {
        OrbState.THINKING -> 1800
        OrbState.LISTENING -> 5500
        OrbState.SPEAKING -> 4000
        OrbState.IDLE -> 12000
    }

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Secondary counter-rotation
    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (rotationDuration * 0.7f).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )

    // Core glow opacity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (state == OrbState.THINKING) 0.6f else 0.45f,
        targetValue = if (state == OrbState.THINKING) 1.0f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == OrbState.THINKING) 600 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Interactive modifier
    val boxModifier = modifier
        .size(size)
        .testTag("arc_reactor_voice_orb")
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = size / 2),
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val audioExpansion = (animatedRms / 10f) * 0.28f
            val baseRadius = (size.toPx() / 2f) * 0.82f * (pulseScale + audioExpansion)

            // 1. External Plasma Energy Aura
            val auraColor1 = when (state) {
                OrbState.THINKING -> SkyGlow.copy(alpha = 0.55f * glowAlpha)
                OrbState.LISTENING -> ElectricBlue.copy(alpha = 0.45f * glowAlpha + (animatedRms * 0.04f))
                OrbState.SPEAKING -> ArcCyan.copy(alpha = 0.50f * glowAlpha + (animatedRms * 0.04f))
                OrbState.IDLE -> ArcCyan.copy(alpha = 0.25f * glowAlpha)
            }
            val auraColor2 = ElectricBlue.copy(alpha = 0.15f * glowAlpha)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor1, auraColor2, Color.Transparent),
                    center = center,
                    radius = baseRadius * 1.45f
                ),
                radius = baseRadius * 1.4f,
                center = center
            )

            // 2. Audio Shockwave Ripple Ring (when active sound or voice is present)
            if (animatedRms > 0.5f || state == OrbState.SPEAKING || state == OrbState.LISTENING) {
                val rippleRadius = baseRadius * (1.12f + (animatedRms / 10f) * 0.28f)
                drawCircle(
                    color = ArcCyan.copy(alpha = (0.35f + animatedRms * 0.05f).coerceIn(0.1f, 0.7f)),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 1.8f)
                )
            }

            // 3. Outer Titanium Structural Ring with 10 Induction Coils (Marvel Arc Reactor signature)
            val outerRingRadius = baseRadius * 0.98f
            drawCircle(
                color = Color(0xFF1E293B),
                radius = outerRingRadius,
                center = center,
                style = Stroke(width = 4.5f)
            )

            // 10 Symmetrical Copper Power Coils
            val numCoils = 10
            for (i in 0 until numCoils) {
                val coilAngle = (i * 36.0)
                val sweep = 18f
                drawArc(
                    color = Color(0xFFD97706).copy(alpha = 0.85f), // Rich copper
                    startAngle = (coilAngle - sweep / 2f).toFloat(),
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRingRadius, center.y - outerRingRadius),
                    size = Size(outerRingRadius * 2, outerRingRadius * 2),
                    style = Stroke(width = 5.0f, cap = StrokeCap.Round)
                )
            }

            // Outer cyan containment bezel
            drawCircle(
                color = ArcCyan.copy(alpha = 0.75f),
                radius = outerRingRadius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // 4. Rotating Energy Ticks & Emitters Ring
            rotate(degrees = rotationAngle, pivot = center) {
                val midRadius = baseRadius * 0.80f
                drawCircle(
                    color = SkyGlow.copy(alpha = 0.5f),
                    radius = midRadius,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // 8 Primary Reactor Flux Radiators
                for (i in 0 until 8) {
                    val angleRad = Math.toRadians(i * 45.0)
                    val r1 = midRadius - 4f
                    val r2 = midRadius + 5f
                    val tickStart = Offset(
                        (center.x + r1 * Math.cos(angleRad)).toFloat(),
                        (center.y + r1 * Math.sin(angleRad)).toFloat()
                    )
                    val tickEnd = Offset(
                        (center.x + r2 * Math.cos(angleRad)).toFloat(),
                        (center.y + r2 * Math.sin(angleRad)).toFloat()
                    )
                    drawLine(
                        color = if (i % 2 == 0) SkyGlow else ElectricBlue,
                        start = tickStart,
                        end = tickEnd,
                        strokeWidth = if (i % 2 == 0) 3.5f else 2.0f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 5. Counter-Rotating Inner Segmented Vortex Ring
            rotate(degrees = counterRotationAngle, pivot = center) {
                val innerRingRadius = baseRadius * 0.58f
                for (s in 0 until 4) {
                    drawArc(
                        color = ElectricBlue.copy(alpha = 0.85f),
                        startAngle = (s * 90f + 10f),
                        sweepAngle = 70f,
                        useCenter = false,
                        topLeft = Offset(center.x - innerRingRadius, center.y - innerRingRadius),
                        size = Size(innerRingRadius * 2, innerRingRadius * 2),
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }
            }

            // 6. High-Intensity Vibranium/Palladium Core
            val coreRadius = baseRadius * 0.40f * (1f + (animatedRms / 10f) * 0.2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        SkyGlow,
                        ArcCyan,
                        ElectricBlue.copy(alpha = 0.7f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius * 1.1f
                ),
                radius = coreRadius,
                center = center
            )

            // Central pure energy point
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = coreRadius * 0.35f,
                center = center
            )
        }
    }
}
