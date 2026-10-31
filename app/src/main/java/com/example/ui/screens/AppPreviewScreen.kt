package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
    var webViewError by remember { mutableStateOf<String?>(null) }

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
        webViewError = null
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
        // Single Slim Top Control Header (~48dp)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Navigation controls (Back)
                    IconButton(
                        onClick = { if (webViewRef?.canGoBack() == true) webViewRef?.goBack() },
                        enabled = canGoBack,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = if (canGoBack) ContentPrimary else ContentTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Active Dev Server Selector Pill / Dropdown
                    var showServerDropdown by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    if (activeServers.size > 1) {
                                        showServerDropdown = true
                                    } else if (activeServers.isEmpty()) {
                                        onRefreshServers()
                                    }
                                },
                            shape = RoundedCornerShape(6.dp),
                            color = if (activeServer != null) AccentSuccessSoft else CardSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (activeServer != null) AccentSuccess else CardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (activeServer != null) AccentSuccess else ContentTertiary)
                                )
                                Text(
                                    text = if (activeServer != null) "${activeServer.port}" else "Offline",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (activeServer != null) AccentSuccess else ContentTertiary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                )
                                if (activeServers.size > 1) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = AccentSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        if (activeServers.size > 1) {
                            DropdownMenu(
                                expanded = showServerDropdown,
                                onDismissRequest = { showServerDropdown = false },
                                modifier = Modifier.background(CardSurface)
                            ) {
                                activeServers.forEach { s ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (s.port == activeServer?.port) AccentPrimary else AccentSuccess)
                                                )
                                                Text(
                                                    text = "${s.name} (Port ${s.port})",
                                                    color = if (s.port == activeServer?.port) AccentPrimary else ContentPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (s.port == activeServer?.port) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        },
                                        onClick = {
                                            showServerDropdown = false
                                            val target = s.getBestUrl(isCloudConnection, bridgeHost)
                                            onSelectUrl(target)
                                            webViewRef?.loadUrl(target)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Center: Slim URL Pill
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { isEditingUrl = !isEditingUrl },
                        shape = RoundedCornerShape(6.dp),
                        color = CardSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (effectiveUrl.startsWith("https")) Icons.Default.Lock else Icons.Outlined.Language,
                                contentDescription = null,
                                tint = if (effectiveUrl.isNotBlank()) AccentSuccess else ContentTertiary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (effectiveUrl.isBlank()) "Belum ada server" else effectiveUrl.removePrefix("http://").removePrefix("https://"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (effectiveUrl.isNotBlank()) ContentPrimary else ContentTertiary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Viewport Cycle Button (Pill: Penuh -> 390 -> 375 -> Desktop)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                selectedViewport = when (selectedViewport) {
                                    ViewportMode.FULL -> ViewportMode.IPHONE_STD
                                    ViewportMode.IPHONE_STD -> ViewportMode.IPHONE_SE
                                    ViewportMode.IPHONE_SE -> ViewportMode.DESKTOP
                                    ViewportMode.DESKTOP -> ViewportMode.FULL
                                }
                            },
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedViewport != ViewportMode.FULL) AccentPrimarySoft else CardSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedViewport != ViewportMode.FULL) AccentPrimary else CardBorder
                        )
                    ) {
                        Text(
                            text = when (selectedViewport) {
                                ViewportMode.FULL -> "📱 Penuh"
                                ViewportMode.IPHONE_STD -> "📱 390"
                                ViewportMode.IPHONE_SE -> "📱 375"
                                ViewportMode.DESKTOP -> "💻 PC"
                            },
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (selectedViewport != ViewportMode.FULL) AccentPrimary else ContentSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }

                    // Hot Reload Button ('r')
                    IconButton(
                        onClick = {
                            onTriggerHotReload()
                            webViewRef?.reload()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Hot Reload",
                            tint = AccentPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Dev Console Drawer Button
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
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = "Dev Console",
                                tint = if (errorCount > 0) AccentDanger else ContentSecondary,
                                modifier = Modifier.size(12.dp)
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

                    // More Menu (Refresh, External Browser, Kill Server, Scan)
                    var showMoreMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu Tambahan",
                                tint = ContentSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(CardSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Muat Ulang Halaman", fontSize = 12.sp, color = ContentPrimary) },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = ContentSecondary, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showMoreMenu = false
                                    webViewRef?.reload()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pindai Ulang Port Dev", fontSize = 12.sp, color = ContentPrimary) },
                                leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null, tint = ContentSecondary, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showMoreMenu = false
                                    onRefreshServers()
                                }
                            )
                            if (effectiveUrl.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Buka di Browser Luar", fontSize = 12.sp, color = ContentPrimary) },
                                    leadingIcon = { Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, tint = ContentSecondary, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        showMoreMenu = false
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effectiveUrl))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                )
                            }
                            if (activeServer != null) {
                                DropdownMenuItem(
                                    text = { Text("Matikan Port ${activeServer.port}", fontSize = 12.sp, color = AccentDanger) },
                                    leadingIcon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = AccentDanger, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        showMoreMenu = false
                                        portToKill = activeServer
                                    }
                                )
                            }
                        }
                    }
                }

                // Expandable Custom URL Input
                if (isEditingUrl) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = { Text("Ketik port (e.g. 3000) atau URL", fontSize = 11.sp, color = ContentTertiary) },
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
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = ContentPrimary),
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
                                    val cleanHost = bridgeHost.removePrefix("http://")
                                        .removePrefix("https://")
                                        .removePrefix("ws://")
                                        .removePrefix("wss://")
                                        .substringBefore("/")
                                        .substringBefore(":")
                                        .ifBlank { "127.0.0.1" }
                                    val resolvedUrl = when {
                                        clean.all { it.isDigit() } -> "http://$cleanHost:$clean"
                                        clean.contains("localhost") || clean.contains("127.0.0.1") -> {
                                            if (cleanHost.isNotBlank() && cleanHost != "127.0.0.1") {
                                                clean.replace("localhost", cleanHost).replace("127.0.0.1", cleanHost)
                                            } else clean
                                        }
                                        !clean.startsWith("http://") && !clean.startsWith("https://") -> "http://$clean"
                                        else -> clean
                                    }
                                    webViewError = null
                                    onSelectUrl(resolvedUrl)
                                    webViewRef?.loadUrl(resolvedUrl)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Buka", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                        webViewError = null
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

                                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            val desc = error?.description?.toString() ?: "Koneksi gagal"
                                            webViewError = "Tidak dapat memuat dev preview ($desc)"
                                        }
                                    }

                                    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                                        super.onReceivedHttpError(view, request, errorResponse)
                                        if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 200) >= 400) {
                                            webViewError = "Server HTTP Error: ${errorResponse?.statusCode}"
                                        }
                                    }

                                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                        // Bypass SSL untuk localhost / IP lokal LAN & trycloudflare dev tunnel
                                        handler?.proceed()
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

                    // Error Fallback Screen (Mencegah tampilan layar blank membingungkan)
                    if (webViewError != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AppBg),
                            color = AppBg
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AccentDangerSoft,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = AccentDanger,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Gagal Membuka Dev Preview",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = ContentPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = effectiveUrl,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = AccentPrimary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = webViewError ?: "Koneksi ke server dev workstation terputus.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ContentSecondary,
                                        fontSize = 11.5.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = CardSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("💡 Tips Solusi:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ContentPrimary)
                                        Text("• Pastikan dev server aktif di PC (misal: npm run dev)", fontSize = 10.5.sp, color = ContentSecondary)
                                        Text("• Tambahkan flag host jika Vite: npm run dev -- --host", fontSize = 10.5.sp, color = ContentSecondary)
                                        Text("• Pastikan HP dan PC berada pada jaringan Wi-Fi yang sama", fontSize = 10.5.sp, color = ContentSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            webViewError = null
                                            webViewRef?.loadUrl(effectiveUrl)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Coba Lagi", fontSize = 11.5.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effectiveUrl))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Outlined.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Browser Luar", fontSize = 11.5.sp, color = ContentSecondary)
                                    }
                                }
                            }
                        }
                    }
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
