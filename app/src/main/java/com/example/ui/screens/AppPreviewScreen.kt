package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ActiveDevServer
import com.example.data.model.ConsoleLogItem
import com.example.data.model.ConsoleLogLevel
import com.example.data.model.WorkspaceScript
import com.example.ui.theme.*

enum class ViewportMode(val label: String, val widthDp: Int?) {
    FULL("Penuh", null),
    IPHONE_SE("375px", 375),
    IPHONE_STD("390px", 390),
    DESKTOP("Desktop", null)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AppPreviewScreen(
    activeServers: List<ActiveDevServer>,
    selectedUrl: String?,
    bridgeHost: String = "",
    isCloudConnection: Boolean = false,
    onSelectUrl: (String) -> Unit,
    onRefreshServers: () -> Unit,
    onTriggerHotReload: () -> Unit,
    onKillPort: (Int) -> Unit,
    onRequestDevTunnel: (Int) -> Unit = {},
    onStopDevTunnel: (Int) -> Unit = {},
    onRequestAiFix: (String) -> Unit = {},
    workspaceScripts: List<WorkspaceScript>,
    onRunScript: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var pageTitle by remember { mutableStateOf("Memuat...") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Viewport & Mode Desktop State
    var selectedViewport by remember { mutableStateOf(ViewportMode.FULL) }
    val isDesktopMode = selectedViewport == ViewportMode.DESKTOP

    // JS Dev Console State
    val consoleLogs = remember { mutableStateListOf<ConsoleLogItem>() }
    var isConsoleDrawerOpen by remember { mutableStateOf(false) }
    var consoleFilterOnlyErrors by remember { mutableStateOf(false) }
    val errorCount = consoleLogs.count { it.level == ConsoleLogLevel.ERROR }

    // Resolve current active server & URL (mengutamakan URL Network LAN / Cloud)
    val activeServer = activeServers.find { it.lanUrl == selectedUrl || it.cloudUrl == selectedUrl }
        ?: activeServers.firstOrNull()

    val effectiveUrl = selectedUrl.takeIf { !it.isNullOrBlank() }
        ?: activeServer?.getBestUrl(isCloudConnection, bridgeHost)
        ?: ""

    var urlInput by remember(effectiveUrl) { mutableStateOf(effectiveUrl) }
    var isEditingUrl by remember { mutableStateOf(false) }
    var portToKill by remember { mutableStateOf<ActiveDevServer?>(null) }

    // Update WebView when effectiveUrl changes
    LaunchedEffect(effectiveUrl) {
        if (effectiveUrl.isNotBlank() && webViewRef != null && webViewRef?.url != effectiveUrl) {
            webViewRef?.loadUrl(effectiveUrl)
        }
    }

    // Toggle Desktop User-Agent dynamically
    LaunchedEffect(isDesktopMode) {
        webViewRef?.settings?.apply {
            userAgentString = if (isDesktopMode) {
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else null
        }
        if (effectiveUrl.isNotBlank()) {
            webViewRef?.reload()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
    ) {
        // Top Control Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Navigation controls (Back, Forward, Reload)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Kembali",
                                tint = if (canGoBack) ContentPrimary else ContentTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { if (webViewRef?.canGoForward() == true) webViewRef?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Maju",
                                tint = if (canGoForward) ContentPrimary else ContentTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Muat Ulang Halaman",
                                tint = ContentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Center: URL Pill / Title
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isEditingUrl = !isEditingUrl },
                        shape = RoundedCornerShape(8.dp),
                        color = CardSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (effectiveUrl.startsWith("https")) Icons.Default.Lock else Icons.Outlined.Language,
                                contentDescription = null,
                                tint = if (effectiveUrl.isNotBlank()) AccentSuccess else ContentTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (effectiveUrl.isBlank()) "Belum ada URL dev server" else effectiveUrl,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (effectiveUrl.isNotBlank()) ContentPrimary else ContentTertiary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCloudConnection) {
                                Text(
                                    text = "CLOUD",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp
                                    )
                                )
                            }
                        }
                    }

                    // Right Actions: Hot Reload & Dev Tools
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Hot Reload Button ('r')
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onTriggerHotReload()
                                    webViewRef?.reload()
                                },
                            shape = RoundedCornerShape(6.dp),
                            color = AccentPrimarySoft,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentPrimary)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Hot Reload",
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Reload",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        // Dev Console Drawer Button (Shows red badge if errors exist)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isConsoleDrawerOpen = !isConsoleDrawerOpen },
                            shape = RoundedCornerShape(6.dp),
                            color = if (errorCount > 0) AccentDangerSoft else if (isConsoleDrawerOpen) CardSurfaceVariant else AppBg,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (errorCount > 0) AccentDanger else CardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Code,
                                    contentDescription = "Dev Console",
                                    tint = if (errorCount > 0) AccentDanger else ContentSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                if (errorCount > 0) {
                                    Text(
                                        text = "$errorCount",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AccentDanger,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }

                        // External Browser Launcher
                        IconButton(
                            onClick = {
                                if (effectiveUrl.isNotBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effectiveUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            },
                            enabled = effectiveUrl.isNotBlank(),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInBrowser,
                                contentDescription = "Buka di Browser Luar",
                                tint = if (effectiveUrl.isNotBlank()) ContentSecondary else ContentTertiary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Expandable Custom URL Input (dengan auto-replace localhost -> bridgeHost)
                if (isEditingUrl) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = { Text("Ketik port atau URL (otomatis ganti localhost ke LAN)", fontSize = 11.sp, color = ContentTertiary) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                isEditingUrl = false
                                if (urlInput.isNotBlank()) {
                                    val clean = urlInput.trim()
                                    val resolvedUrl = when {
                                        clean.all { it.isDigit() } -> "http://${bridgeHost.ifBlank { "127.0.0.1" }}:$clean"
                                        clean.contains("localhost") || clean.contains("127.0.0.1") -> {
                                            if (bridgeHost.isNotBlank() && !bridgeHost.contains("trycloudflare.com")) {
                                                clean.replace("localhost", bridgeHost).replace("127.0.0.1", bridgeHost)
                                            } else clean
                                        }
                                        !clean.startsWith("http://") && !clean.startsWith("https://") -> "http://$clean"
                                        else -> clean
                                    }
                                    onSelectUrl(resolvedUrl)
                                    webViewRef?.loadUrl(resolvedUrl)
                                }
                            }),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = ContentPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = AppBg,
                                unfocusedContainerColor = AppBg
                            )
                        )

                        Button(
                            onClick = {
                                isEditingUrl = false
                                if (urlInput.isNotBlank()) {
                                    val clean = urlInput.trim()
                                    val resolvedUrl = when {
                                        clean.all { it.isDigit() } -> "http://${bridgeHost.ifBlank { "127.0.0.1" }}:$clean"
                                        clean.contains("localhost") || clean.contains("127.0.0.1") -> {
                                            if (bridgeHost.isNotBlank() && !bridgeHost.contains("trycloudflare.com")) {
                                                clean.replace("localhost", bridgeHost).replace("127.0.0.1", bridgeHost)
                                            } else clean
                                        }
                                        !clean.startsWith("http://") && !clean.startsWith("https://") -> "http://$clean"
                                        else -> clean
                                    }
                                    onSelectUrl(resolvedUrl)
                                    webViewRef?.loadUrl(resolvedUrl)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Buka", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Baris Viewport & Mode Desktop Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Viewport Presets
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Layar:",
                            style = MaterialTheme.typography.labelSmall.copy(color = ContentTertiary, fontSize = 10.sp)
                        )
                        ViewportMode.values().forEach { mode ->
                            val isSelected = selectedViewport == mode
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { selectedViewport = mode },
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) AccentPrimarySoft else CardSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) AccentPrimary else CardBorder
                                )
                            ) {
                                Text(
                                    text = mode.label,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) AccentPrimary else ContentSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 9.sp
                                    )
                                )
                            }
                        }
                    }

                    // Scan port action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onRefreshServers() },
                            shape = RoundedCornerShape(4.dp),
                            color = CardSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = "Pindai", tint = ContentSecondary, modifier = Modifier.size(11.dp))
                                Text("Pindai Port", fontSize = 9.sp, color = ContentSecondary)
                            }
                        }
                    }
                }

                // Cloud Tunnel Banner (Khusus Mode Cloud)
                if (isCloudConnection && activeServer != null) {
                    val hasTunnel = !activeServer.cloudUrl.isNullOrBlank()
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (hasTunnel) AccentSuccessSoft else AccentWarningSoft,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (hasTunnel) AccentSuccess else AccentWarning)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (hasTunnel) Icons.Default.Lock else Icons.Outlined.CloudQueue,
                                    contentDescription = null,
                                    tint = if (hasTunnel) AccentSuccess else AccentWarning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (hasTunnel) {
                                        "Tunnel Cloud Aktif: ${activeServer.cloudUrl}"
                                    } else {
                                        "Mode Cloud: Port ${activeServer.port} butuh tunnel publik"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (hasTunnel) AccentSuccess else ContentPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                            }

                            if (hasTunnel) {
                                TextButton(
                                    onClick = { onStopDevTunnel(activeServer.port) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Matikan", color = AccentDanger, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { onRequestDevTunnel(activeServer.port) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("Buka Tunnel", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Active Dev Servers Chip Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (activeServers.isEmpty()) {
                        Text(
                            text = "Tidak ada dev server terdeteksi",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = ContentTertiary)
                        )
                    } else {
                        activeServers.forEach { server ->
                            val targetUrl = server.getBestUrl(isCloudConnection, bridgeHost)
                            val isCurrent = effectiveUrl == targetUrl || effectiveUrl == server.lanUrl || effectiveUrl == server.localUrl
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectUrl(targetUrl)
                                        webViewRef?.loadUrl(targetUrl)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) AccentPrimarySoft else CardSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) AccentPrimary else CardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) AccentPrimary else AccentSuccess)
                                    )

                                    Text(
                                        text = "${server.name} (${server.port})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isCurrent) AccentPrimary else ContentPrimary,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    )

                                    if (!server.cloudUrl.isNullOrBlank()) {
                                        Icon(
                                            imageVector = Icons.Default.Cloud,
                                            contentDescription = "Cloud",
                                            tint = AccentPrimary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }

                                    // Kill port action
                                    IconButton(
                                        onClick = { portToKill = server },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hentikan Port",
                                            tint = ContentTertiary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Loading progress bar
                if (loadingProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = AccentPrimary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        // Main Content Area: WebView OR Empty State with Quick Run Scripts
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(AppBg)
        ) {
            if (effectiveUrl.isBlank() && activeServers.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CardSurfaceVariant,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Devices,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Uji Langsung Aplikasi di Ponsel",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = ContentPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Belum ada dev server yang aktif. Jalankan script dev di bawah atau jalankan 'npm run dev' di PC.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ContentSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (workspaceScripts.isNotEmpty()) {
                        Text(
                            text = "Jalankan Script Dev di Workstation:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ContentTertiary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            workspaceScripts.take(4).forEach { script ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onRunScript(script.command) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = CardSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = AccentSuccess,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = script.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = ContentPrimary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                                Text(
                                                    text = script.command,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = ContentTertiary,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 9.sp
                                                    )
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AccentPrimarySoft
                                        ) {
                                            Text(
                                                text = "Jalankan",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = AccentPrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Live In-App WebView with Optional Simulated Viewport Frame
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val frameModifier = if (selectedViewport.widthDp != null) {
                        Modifier
                            .width(selectedViewport.widthDp!!.dp)
                            .fillMaxHeight()
                            .border(1.dp, AccentPrimarySoft)
                    } else {
                        Modifier.fillMaxSize()
                    }

                    AndroidView(
                        modifier = frameModifier,
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    // Security Hardening: Blokir akses file lokal HP
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    setGeolocationEnabled(false)
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    cacheMode = WebSettings.LOAD_NO_CACHE
                                    if (isDesktopMode) {
                                        userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        pageTitle = view?.title ?: "Selesai"
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        return false // Tetap di dalam WebView
                                    }

                                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                        val url = error?.url ?: view?.url ?: ""
                                        // Bypass SSL hanya untuk IP lokal LAN & trycloudflare dev tunnel
                                        val isDevHost = url.contains("192.168.") ||
                                                url.contains("10.") ||
                                                url.contains("172.") ||
                                                url.contains("localhost") ||
                                                url.contains("127.0.0.1") ||
                                                url.contains(".trycloudflare.com")
                                        if (isDevHost) {
                                            handler?.proceed()
                                        } else {
                                            handler?.cancel()
                                        }
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadingProgress = newProgress
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        pageTitle = title ?: ""
                                    }

                                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                        if (consoleMessage != null) {
                                            val level = when (consoleMessage.messageLevel()) {
                                                ConsoleMessage.MessageLevel.ERROR -> ConsoleLogLevel.ERROR
                                                ConsoleMessage.MessageLevel.WARNING -> ConsoleLogLevel.WARN
                                                ConsoleMessage.MessageLevel.DEBUG -> ConsoleLogLevel.DEBUG
                                                else -> ConsoleLogLevel.LOG
                                            }
                                            val item = ConsoleLogItem(
                                                level = level,
                                                message = consoleMessage.message() ?: "",
                                                lineNumber = consoleMessage.lineNumber(),
                                                sourceId = consoleMessage.sourceId() ?: ""
                                            )
                                            if (consoleLogs.size >= 200) {
                                                consoleLogs.removeAt(0)
                                            }
                                            consoleLogs.add(item)
                                        }
                                        return super.onConsoleMessage(consoleMessage)
                                    }
                                }

                                if (effectiveUrl.isNotBlank()) {
                                    loadUrl(effectiveUrl)
                                }

                                webViewRef = this
                            }
                        },
                        update = { view ->
                            webViewRef = view
                        }
                    )
                }
            }
        }

        // Native JS Dev Console Drawer (Bottom Panel)
        if (isConsoleDrawerOpen) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                color = CardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Drawer Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Dev Console",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ContentPrimary)
                            )
                            Text(
                                text = "(${consoleLogs.size})",
                                style = MaterialTheme.typography.bodySmall.copy(color = ContentTertiary, fontSize = 10.sp)
                            )
                            if (errorCount > 0) {
                                Text(
                                    text = "$errorCount errors",
                                    style = MaterialTheme.typography.bodySmall.copy(color = AccentDanger, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Filter Only Errors toggle
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { consoleFilterOnlyErrors = !consoleFilterOnlyErrors },
                                shape = RoundedCornerShape(4.dp),
                                color = if (consoleFilterOnlyErrors) AccentDangerSoft else CardSurfaceVariant
                            ) {
                                Text(
                                    text = "Error Saja",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (consoleFilterOnlyErrors) AccentDanger else ContentSecondary,
                                        fontSize = 9.sp
                                    )
                                )
                            }

                            // Tanya AI Fix button (if errors exist)
                            if (errorCount > 0) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            val recentErrors = consoleLogs.filter { it.level == ConsoleLogLevel.ERROR }
                                            val lastErr = recentErrors.lastOrNull()
                                            val contextLogs = consoleLogs.takeLast(6).joinToString("\n") { "[${it.level}] ${it.message}" }
                                            val prompt = "Tolong periksa dan perbaiki error JavaScript pada halaman web preview ($effectiveUrl):\nError: ${lastErr?.message ?: "Web error"}\nFile: ${lastErr?.sourceId}:${lastErr?.lineNumber}\nRiwayat log:\n```\n$contextLogs\n```"
                                            onRequestAiFix(prompt)
                                        },
                                    shape = RoundedCornerShape(4.dp),
                                    color = AccentPrimary
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                        Text("Tanya AI Fix", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Clear logs
                            IconButton(
                                onClick = { consoleLogs.clear() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Hapus Log", tint = ContentTertiary, modifier = Modifier.size(14.dp))
                            }

                            // Close drawer
                            IconButton(
                                onClick = { isConsoleDrawerOpen = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = ContentSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = CardBorder, thickness = 1.dp)

                    // Logs List
                    val filteredLogs = if (consoleFilterOnlyErrors) {
                        consoleLogs.filter { it.level == ConsoleLogLevel.ERROR }
                    } else consoleLogs

                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (consoleFilterOnlyErrors) "Tidak ada error JavaScript" else "Belum ada log console",
                                style = MaterialTheme.typography.bodySmall.copy(color = ContentTertiary, fontSize = 11.sp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(filteredLogs) { log ->
                                val textColor = when (log.level) {
                                    ConsoleLogLevel.ERROR -> AccentDanger
                                    ConsoleLogLevel.WARN -> AccentWarning
                                    ConsoleLogLevel.INFO -> AccentPrimary
                                    else -> ContentSecondary
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "[${log.level.name.take(3)}]",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = textColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = log.message + if (log.lineNumber > 0) " (${log.sourceId.substringAfterLast("/")}:${log.lineNumber})" else "",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = textColor
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Konfirmasi Kill Port
    portToKill?.let { server ->
        AlertDialog(
            onDismissRequest = { portToKill = null },
            title = {
                Text(
                    text = "Hentikan Port ${server.port}?",
                    style = MaterialTheme.typography.titleMedium.copy(color = ContentPrimary, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Server '${server.name}' (PID: ${server.pid}) yang mendengarkan di port ${server.port} akan dimatikan di workstation untuk membebaskan port.",
                    style = MaterialTheme.typography.bodySmall.copy(color = ContentSecondary, fontSize = 12.sp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onKillPort(server.port)
                        portToKill = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDanger)
                ) {
                    Text("Matikan Server", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { portToKill = null }) {
                    Text("Batal", color = ContentSecondary)
                }
            },
            containerColor = CardSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
