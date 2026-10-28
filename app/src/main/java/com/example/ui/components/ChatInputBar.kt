package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * WhatsApp-style Chat Input Bar:
 * - Full-width capsule input field containing attachment (+) button
 * - Outside circular FAB (Send when text/image present, Mic when empty, Stop when streaming)
 * - Attached image preview pill above capsule
 * - Does not push or squeeze the textarea into a narrow box
 */
@Composable
fun ChatInputBar(
    inputPrompt: String,
    onInputChange: (String) -> Unit,
    attachedImageUri: Uri?,
    onAttachImageClick: () -> Unit,
    onRemoveAttachedImage: () -> Unit,
    onVoiceInputClick: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onSendImage: ((Uri, String) -> Unit)?,
    isStreaming: Boolean,
    onCancelAgent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // 1. Attached Image Preview Banner (WhatsApp style floating pill)
        AnimatedVisibility(
            visible = attachedImageUri != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = AccentPrimarySoft,
                border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Gambar terlampir untuk AI Agent",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = AccentPrimary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = onRemoveAttachedImage,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus Gambar",
                            tint = ContentSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // 2. WhatsApp-Style Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Capsule Text Input Surface (WhatsApp Bubble)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(24.dp),
                color = CardSurfaceVariant,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment Icon inside capsule on the left (Paperclip / Image)
                    IconButton(
                        onClick = onAttachImageClick,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("btn_attach_image")
                    ) {
                        Icon(
                            imageVector = if (attachedImageUri != null) Icons.Default.Image else Icons.Outlined.AttachFile,
                            contentDescription = "Lampirkan Gambar",
                            tint = if (attachedImageUri != null) AccentPrimary else ContentSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Main Text Input - Takes 100% of the capsule space
                    TextField(
                        value = inputPrompt,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .weight(1f)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.key == Key.Enter && !keyEvent.isShiftPressed && keyEvent.type == KeyEventType.KeyUp) {
                                    if (attachedImageUri != null) {
                                        onSendImage?.invoke(attachedImageUri, inputPrompt)
                                        onRemoveAttachedImage()
                                        onInputChange("")
                                        true
                                    } else if (inputPrompt.isNotBlank()) {
                                        onSendPrompt(inputPrompt)
                                        onInputChange("")
                                        true
                                    } else false
                                } else false
                            }
                            .testTag("agent_chat_input"),
                        placeholder = {
                            Text(
                                text = "Ketik instruksi AI Agent...",
                                color = ContentTertiary,
                                fontSize = 14.sp
                            )
                        },
                        maxLines = 5,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedTextColor = ContentPrimary,
                            unfocusedTextColor = ContentPrimary,
                            cursorColor = AccentPrimary
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send,
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        keyboardActions = KeyboardActions(onSend = {
                            if (attachedImageUri != null) {
                                onSendImage?.invoke(attachedImageUri, inputPrompt)
                                onRemoveAttachedImage()
                                onInputChange("")
                            } else if (inputPrompt.isNotBlank()) {
                                onSendPrompt(inputPrompt)
                                onInputChange("")
                            }
                        })
                    )
                }
            }

            // Outside Circular Action Button (WhatsApp Style FAB)
            if (isStreaming) {
                // Stop Agent Button
                FilledIconButton(
                    onClick = onCancelAgent,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_cancel_agent"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AccentDanger,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Hentikan Agent",
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else if (inputPrompt.isNotBlank() || attachedImageUri != null) {
                // Send Message Button
                FilledIconButton(
                    onClick = {
                        if (attachedImageUri != null) {
                            onSendImage?.invoke(attachedImageUri, inputPrompt)
                            onRemoveAttachedImage()
                            onInputChange("")
                        } else if (inputPrompt.isNotBlank()) {
                            onSendPrompt(inputPrompt)
                            onInputChange("")
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_send_agent_prompt"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AccentPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Kirim Prompt",
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                // Voice Dictation Button (When empty, like WhatsApp mic)
                FilledTonalIconButton(
                    onClick = onVoiceInputClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_voice_input"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = CardSurfaceVariant,
                        contentColor = AccentPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Dikte Suara",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
