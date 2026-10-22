package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.storage.ConnectionPreferences
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    preferences: ConnectionPreferences,
    isKeepScreenOn: Boolean,
    onToggleKeepScreenOn: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hapticEnabled by remember { mutableStateOf(preferences.isHapticEnabled()) }
    var autoReconnectEnabled by remember { mutableStateOf(preferences.isAutoReconnectEnabled()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .testTag("settings_dialog"),
            color = CardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = AccentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Pengaturan Aplikasi",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ContentPrimary
                            )
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = ContentSecondary
                        )
                    }
                }

                HorizontalDivider(color = CardBorder)

                // Setting 1: Haptic Feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Getar Haptic",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ContentPrimary,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "Getaran halus saat AI selesai berpikir atau meminta persetujuan",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ContentSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = {
                            hapticEnabled = it
                            preferences.setHapticEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentPrimary
                        )
                    )
                }

                // Setting 2: Auto-Reconnect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Reconnect",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ContentPrimary,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "Menghubungkan otomatis dengan jeda saat jaringan terputus",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ContentSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Switch(
                        checked = autoReconnectEnabled,
                        onCheckedChange = {
                            autoReconnectEnabled = it
                            preferences.setAutoReconnectEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentPrimary
                        )
                    )
                }

                // Setting 3: Mode Meja (Keep Screen On)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mode Meja (Layar Selalu Aktif)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ContentPrimary,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = "Mencegah layar HP terkunci saat ditaruh di stand meja kerja",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ContentSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Switch(
                        checked = isKeepScreenOn,
                        onCheckedChange = { onToggleKeepScreenOn() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentWarning
                        )
                    )
                }

                HorizontalDivider(color = CardBorder)

                // Danger Zone: Hapus Profil Koneksi
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Data Sesi",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ContentTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    OutlinedButton(
                        onClick = {
                            preferences.clearConnection()
                            Toast.makeText(context, "Profil koneksi tersimpan dihapus", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentDanger),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentDanger.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hapus Profil Koneksi Tersimpan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // App Info / Footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = CardSurfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Antiremote v2.1 (Superpower Edition)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ContentPrimary,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "Remote AI Agent & Workspace Controller untuk Antigravity IDE",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ContentTertiary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
