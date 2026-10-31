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

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor

/**
 * WhatsApp-style Chat Input Bar:
 * - Pixel-perfect symmetrical 48dp capsule input field containing attachment (+) button
 * - Outside 48dp circular FAB (Send when text/image present, Mic when empty, Stop when streaming)
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

        // 2. WhatsApp-Style Input Row: Symmetrical 48dp capsule + 48dp circular button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        .padding(start = 4.dp, end = 12.dp),
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
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Main Text Input - perfectly aligned vertically with basic text field
                    BasicTextField(
                        value = inputPrompt,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)
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
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = ContentPrimary,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(AccentPrimary),
                        maxLines = 5,
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
                        }),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputPrompt.isEmpty()) {
                                    Text(
                                        text = "Ketik instruksi AI Agent...",
                                        color = ContentTertiary,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }

            // Outside Circular Action Button (Matching 48.dp)
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isStreaming) {
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
}
