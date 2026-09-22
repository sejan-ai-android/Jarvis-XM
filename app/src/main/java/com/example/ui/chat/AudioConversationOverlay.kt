package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.JarvisVoiceOrb
import com.example.ui.components.OrbState
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SkyGlow
import com.example.ui.theme.SpaceNavyDark
import com.example.ui.theme.StatusValidGreen
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceDark

@Composable
fun AudioConversationOverlay(
    uiState: ChatUiState,
    onClose: () -> Unit,
    onToggleListening: () -> Unit,
    onToggleContinuousLoop: () -> Unit,
    onToggleAlwaysListening: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SpaceNavyDark.copy(alpha = 0.97f),
                        Color(0xFF070B14),
                        Color.Black
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("audio_conversation_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top HUD Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (uiState.orbState) {
                                    OrbState.LISTENING -> ElectricBlue
                                    OrbState.THINKING -> SkyGlow
                                    OrbState.SPEAKING -> ArcCyan
                                    OrbState.IDLE -> if (uiState.isAlwaysListening) StatusValidGreen else StatusValidGreen.copy(alpha = 0.5f)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "JARVIS NEURAL LINK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ArcCyan
                            )
                        )
                        Text(
                            text = if (uiState.isAlwaysListening) "ALWAYS LISTENING • ACTIVE" else if (uiState.isContinuousLoopEnabled) "CONTINUOUS AUDIO • ON" else "PUSH TO TALK",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = if (uiState.isAlwaysListening) StatusValidGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Always Listening HUD Chip
                    Surface(
                        onClick = onToggleAlwaysListening,
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.isAlwaysListening) StatusValidGreen.copy(alpha = 0.2f) else SurfaceContainerDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uiState.isAlwaysListening) StatusValidGreen else ArcCyan.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("overlay_always_listening_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isAlwaysListening) StatusValidGreen else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isAlwaysListening) "ALWAYS ON" else "AUTO",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (uiState.isAlwaysListening) StatusValidGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Toggle continuous loop button
                    IconButton(
                        onClick = onToggleContinuousLoop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.isContinuousLoopEnabled) ArcCyan.copy(alpha = 0.2f) else SurfaceContainerDark
                            )
                            .testTag("toggle_continuous_loop_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = "Toggle Hands-Free Loop",
                            tint = if (uiState.isContinuousLoopEnabled) ArcCyan else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Close overlay button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerDark)
                            .testTag("close_audio_overlay_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Exit Audio Conversation",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. Center Stage: Giant Arc Reactor Voice Orb with Dynamic Audio Equalizer
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Interactive Arc Reactor Voice Orb (190.dp)
                JarvisVoiceOrb(
                    state = uiState.orbState,
                    size = 190.dp,
                    rmsDb = uiState.rmsDb,
                    onClick = onToggleListening
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Real-time Soundwave Equalizer Bars
                AudioWaveVisualizer(
                    rmsDb = uiState.rmsDb,
                    state = uiState.orbState
                )

                Spacer(modifier = Modifier.height(16.dp))

                // State Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceDark.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (uiState.orbState) {
                            OrbState.LISTENING -> ElectricBlue
                            OrbState.THINKING -> SkyGlow
                            OrbState.SPEAKING -> ArcCyan
                            OrbState.IDLE -> ArcCyan.copy(alpha = 0.3f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (uiState.orbState) {
                                OrbState.LISTENING -> Icons.Filled.Mic
                                OrbState.THINKING -> Icons.Filled.GraphicEq
                                OrbState.SPEAKING -> Icons.Filled.VolumeUp
                                OrbState.IDLE -> Icons.Filled.MicOff
                            },
                            contentDescription = null,
                            tint = ArcCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (uiState.orbState) {
                                OrbState.LISTENING -> "LISTENING TO USER..."
                                OrbState.THINKING -> "PROCESSING QUERY WITH GEMINI..."
                                OrbState.SPEAKING -> "JARVIS SPEAKING..."
                                OrbState.IDLE -> "STANDBY • TAP ORB TO SPEAK"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ArcCyan,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Speech / Transcript HUD Readout Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    val currentText = when {
                        uiState.liveTranscript.isNotBlank() -> uiState.liveTranscript
                        uiState.orbState == OrbState.SPEAKING && uiState.lastSpokenResponse.isNotBlank() -> uiState.lastSpokenResponse
                        uiState.orbState == OrbState.LISTENING -> "Speak freely, Sir. I am tuned to your audio stream."
                        uiState.orbState == OrbState.THINKING -> "Synthesizing strategic response..."
                        else -> "Tap the Arc Reactor to initiate voice input."
                    }

                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArcCyan.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (uiState.liveTranscript.isNotBlank()) "YOU" else if (uiState.orbState == OrbState.SPEAKING) "JARVIS" else "STATUS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (uiState.liveTranscript.isNotBlank()) ElectricBlue else ArcCyan,
                                    letterSpacing = 1.5.sp,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            )
                        }
                    }
                }
            }

            // 3. Bottom Controls HUD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Push to talk / Toggle Mute
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.isListening) ElectricBlue else SurfaceContainerDark
                            )
                            .clickable { onToggleListening() }
                            .testTag("overlay_mic_toggle_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = if (uiState.isListening) "Mute Microphone" else "Listen",
                            tint = if (uiState.isListening) Color.White else ArcCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Tap the Arc Reactor anytime to interrupt or speak",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun AudioWaveVisualizer(
    rmsDb: Float,
    state: OrbState,
    modifier: Modifier = Modifier
) {
    val barCount = 18
    val isActive = state == OrbState.LISTENING || state == OrbState.SPEAKING

    Row(
        modifier = modifier
            .height(28.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val distFromCenter = Math.abs(i - (barCount / 2f)) / (barCount / 2f)
            val baseFactor = (1f - distFromCenter).coerceAtLeast(0.15f)
            val audioScale = if (isActive) (rmsDb / 10f).coerceIn(0f, 1f) else 0.08f
            val heightFraction = ((baseFactor * (0.25f + audioScale * 0.75f)) + ((i % 3) * 0.05f)).coerceIn(0.1f, 1f)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((28.dp * heightFraction).coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isActive) {
                            if (state == OrbState.SPEAKING) ArcCyan else ElectricBlue
                        } else {
                            SurfaceContainerDark
                        }
                    )
            )
        }
    }
}
