package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.prefs.ApiKeyConfig
import com.example.data.prefs.KeyStatus
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.StatusInvalidRed
import com.example.ui.theme.StatusUntestedAmber
import com.example.ui.theme.StatusValidGreen
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceContainerDark

@Composable
fun ApiKeyField(
    config: ApiKeyConfig,
    onSave: (String) -> Unit,
    onTest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember(config.currentValue) { mutableStateOf(config.currentValue) }
    var isPasswordHidden by remember { mutableStateOf(true) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = SurfaceContainerDark,
        border = BorderStroke(1.dp, SurfaceBorderDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title + Required Badge + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (config.isRequired) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ArcCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Required",
                                color = ArcCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Status Badge
                KeyStatusBadge(status = config.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = config.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Free tier & provider link
            Text(
                text = "ℹ️ ${config.freeTierInfo}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ArcCyan.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Input TextField
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("key_input_${config.id}"),
                placeholder = {
                    Text(
                        text = if (config.currentValue.isEmpty()) "Enter key or token" else "Key configured",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                },
                singleLine = true,
                visualTransformation = if (isPasswordHidden) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = {
                    IconButton(
                        onClick = { isPasswordHidden = !isPasswordHidden },
                        modifier = Modifier.testTag("toggle_visibility_${config.id}")
                    ) {
                        Icon(
                            imageVector = if (isPasswordHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (isPasswordHidden) "Show Key" else "Hide Key",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    onSave(textValue)
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ArcCyan,
                    unfocusedBorderColor = SurfaceBorderDark,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Status message callout
            AnimatedVisibility(visible = config.statusMessage.isNotBlank()) {
                Text(
                    text = config.statusMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = when (config.status) {
                            KeyStatus.VALID -> StatusValidGreen
                            KeyStatus.INVALID -> StatusInvalidRed
                            KeyStatus.TESTING -> ArcCyan
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Test Connection + Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        keyboardController?.hide()
                        onTest(textValue)
                    },
                    modifier = Modifier.testTag("test_btn_${config.id}"),
                    enabled = textValue.isNotBlank() && config.status != KeyStatus.TESTING,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (textValue.isNotBlank()) ArcCyan.copy(alpha = 0.7f) else SurfaceBorderDark)
                ) {
                    if (config.status == KeyStatus.TESTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = ArcCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verifying...", color = ArcCyan)
                    } else {
                        Text("Test Connection", color = if (textValue.isNotBlank()) ArcCyan else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSave(textValue)
                    },
                    modifier = Modifier.testTag("save_btn_${config.id}"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcCyan,
                        contentColor = Color(0xFF060B14)
                    )
                ) {
                    Text(
                        text = if (textValue.isBlank() && config.currentValue.isNotBlank()) "Clear" else "Save",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun KeyStatusBadge(status: KeyStatus) {
    val (bgColor, textColor, icon, label) = when (status) {
        KeyStatus.VALID -> Quad(StatusValidGreen.copy(alpha = 0.18f), StatusValidGreen, Icons.Filled.CheckCircle, "Valid")
        KeyStatus.INVALID -> Quad(StatusInvalidRed.copy(alpha = 0.18f), StatusInvalidRed, Icons.Filled.Error, "Invalid")
        KeyStatus.UNTESTED -> Quad(StatusUntestedAmber.copy(alpha = 0.18f), StatusUntestedAmber, Icons.Filled.HelpOutline, "Untested")
        KeyStatus.TESTING -> Quad(ArcCyan.copy(alpha = 0.18f), ArcCyan, Icons.Filled.HourglassEmpty, "Testing...")
        KeyStatus.NOT_SET -> Quad(Color.Gray.copy(alpha = 0.18f), Color.Gray, Icons.Filled.HelpOutline, "Not Set")
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
