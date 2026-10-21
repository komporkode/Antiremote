# Cloudflare Tunnel & VS Code Dashboard Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable secure remote access to Antigravity IDE from anywhere over the cloud using on-demand Cloudflare Quick Tunnels with 6-digit PIN security, accompanied by a complete Linear Dark Minimalist redesign of the VS Code dashboard and auto-detecting Android client.

**Architecture:** 
1. The VS Code extension spawns a `cloudflared` process tunneling `localhost:9876` to an ephemeral public `https://*.trycloudflare.com` domain with zero router setup or account registration.
2. The bridge server enforces a 5-second Zero-Trust PIN authentication barrier on tunnel connections.
3. The VS Code dashboard webview is completely overhauled to a dual-tab Linear Dark Minimalist console with live device telemetry, quick actions, and cloud tunnel controls.
4. The Android app (`LanBridgeClient`) is upgraded to auto-detect `wss://` SSL domains, pass PIN credentials upon handshake, and parse cloud QR payloads.

**Tech Stack:**
- PC Extension: TypeScript, Node.js (`child_process`, `https`), WebSocket (`ws`), VS Code Webview API, SVG/CSS.
- Android: Kotlin, Jetpack Compose, OkHttp WebSocket (TLS/WSS), StateFlow, Coroutines.

---

## Global Constraints
- Minimalist Dark Palette: `#0d0e11` background, `#17181c` surface, `#25272e` border, `#6366f1` primary accent.
- Binary portability: `cloudflared` must run as a standalone portable binary or be automatically downloaded without requiring administrator privileges or package managers.
- Backward compatibility: Local LAN pairing (`ws://192.168.x.x:9876`) must continue working seamlessly without requiring a PIN unless Cloud mode is active.
- Non-blocking: Tunnel downloads and child processes must run asynchronously off the UI thread.

## Review Focus
1. Interrupted Tunnel Download: If download of `cloudflared.exe` is interrupted, partial files must be discarded and cleaned up.
2. Socket Auth Timeout: If an unauthorized bot scans the cloud URL, socket must be forcibly closed within 5 seconds without crashing the server.
3. Network Drop Reconnect: If internet disconnects while tunnel is active, the extension must detect exit and offer 1-click re-tunneling without freezing the webview.
4. Android TLS Parsing: Cloud URLs must connect using OkHttp's standard TLS socket without port appending (e.g. `wss://xyz.trycloudflare.com`).
5. PIN Regeneration: When the user regenerates the PIN in the dashboard, any currently connected unauthenticated client must be severed and the QR code updated immediately.

---

### Task 1: Cloudflare Tunnel Manager (`cloudflaredManager.ts`)

**Files:**
- Create: `D:\PROJECT\GAWEAN\PROJECT\EXT\antiremote\src\cloudflaredManager.ts`
- Modify: `D:\PROJECT\GAWEAN\PROJECT\EXT\antiremote\src\extension.ts`

**Interfaces:**
- Produces: `CloudflaredManager`
  - `startTunnel(localPort: number): Promise<{ url: string; pin: string }>`
  - `stopTunnel(): void`
  - `regeneratePin(): string`
  - `getStatus(): { isRunning: boolean; url: string | null; pin: string | null; error: string | null }`
  - Events: `onTunnelReady`, `onTunnelClosed`, `onTunnelError`

- [x] **Step 1: Implement `CloudflaredManager` with binary detection and download**
  - Check for system `cloudflared` via `where cloudflared` on Windows.
  - If missing, download official release binary `cloudflared-windows-amd64.exe` from `https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe` into extension `bin/` directory with progress reporting.
- [x] **Step 2: Implement process spawning and URL extraction**
  - Spawn `cloudflared tunnel --url http://127.0.0.1:<port>`.
  - Capture stderr/stdout regex matching `https://[a-zA-Z0-9-]+\.trycloudflare\.com`.
  - Generate cryptographically secure 6-digit numeric PIN (`100000..999999`).
- [x] **Step 3: Implement process termination and cleanup**
  - Kill child process on `stopTunnel()` or extension deactivation.
- [x] **Step 4: Verify tunnel startup and teardown via scratch script**

---

### Task 2: Bridge Server PIN Guard & Remote Control Actions (`bridgeServer.ts`)

**Files:**
- Modify: `D:\PROJECT\GAWEAN\PROJECT\EXT\antiremote\src\bridgeServer.ts`
- Modify: `D:\PROJECT\GAWEAN\PROJECT\EXT\antiremote\src\types.ts`

**Interfaces:**
- Consumes: PIN and Cloud status from `CloudflaredManager`
- Produces:
  - `bridgeServer.setSecurityPin(pin: string | null): void`
  - `bridgeServer.disconnectAllClients(): void`
  - `bridgeServer.sendTestNotificationToClients(message: string): void`
  - Incoming message handling: `{"type": "AUTH", "pin": "..."}`

- [x] **Step 1: Add authentication state to client sockets**
  - Add `isAuthenticated: boolean` and `authTimeout: NodeJS.Timeout` to client tracking.
  - If `securityPin` is set, set `isAuthenticated = false`.
  - Start 5000ms timer; if not authenticated by then, send `{"type": "AUTH_FAILED", "reason": "Timeout"}` and call `socket.terminate()`.
- [x] **Step 2: Handle `AUTH` message**
  - Verify client PIN against `securityPin`. If match: cancel timer, set `isAuthenticated = true`, send `{"type": "AUTH_SUCCESS"}`.
  - If mismatch: send `{"type": "AUTH_FAILED", "reason": "Invalid PIN"}` and terminate socket.
- [x] **Step 3: Add quick actions**
  - `sendTestNotificationToClients`: Broadcasts simulated high-priority approval to verify phone vibration/banner.
  - `disconnectAllClients`: Terminates active sockets to kick unauthorized devices.
- [x] **Step 4: Verify PIN guard logic with test websocket client in scratch**

---

### Task 3: VS Code Dashboard Overhaul (`dashboardPanel.ts`)

**Files:**
- Modify: `D:\PROJECT\GAWEAN\PROJECT\EXT\antiremote\src\dashboardPanel.ts`

**Interfaces:**
- Consumes: `CloudflaredManager`, `BridgeServer`
- Produces: High-end Linear Dark Minimalist webview UI with dual-tab pairing controls.

- [x] **Step 1: Redesign CSS and layout to Linear Dark Minimalist**
  - Palette: `#0d0e11` background, `#17181c` cards, `#25272e` borders, `#6366f1` accent, Inter/Segoe UI typography.
  - Responsive header with Status Pill, Port indicator, and Ping latency.
- [x] **Step 2: Build Dual-Tab Navigation**
  - **Tab 1: Local LAN (Wi-Fi)**:
    - Primary IP and port display.
    - Standard Wi-Fi QR Code for local network pairing.
  - **Tab 2: Cloudflare Remote (WAN/4G)**:
    - Big primary toggle: `[🌐 Start Cloud Remote]` / `[🛑 Stop Cloud Tunnel]`.
    - Tunnel status indicator (Active / Offline / Downloading cloudflared).
    - Public `wss://...` URL with Copy button.
    - 6-digit PIN box with `[Regenerate]` button.
    - Cloud QR Code containing `{ "type": "ANTIGRAVITY_BRIDGE", "url": "wss://...", "pin": "..." }`.
- [x] **Step 3: Add Connected Device Card & Quick Actions**
  - Show connected phone hostname/IP, ping latency, and connected timestamp.
  - Action buttons: `[📲 Test Notification]`, `[❌ Disconnect Phone]`, `[🔄 Restart Bridge]`.
- [x] **Step 4: Add Activity Stream with status badges**
  - Realtime log stream showing incoming remote commands, approvals, and terminal activity with timestamped badges.

---

### Task 4: Android App Cloud URL & PIN Handshake Support

**Files:**
- Modify: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\data\network\LanBridgeClient.kt`
- Modify: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\ui\components\ConnectionDialog.kt`
- Modify: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\ui\components\QrScannerDialog.kt`

**Interfaces:**
- Consumes: Cloud QR Code payload `{ "type": "ANTIGRAVITY_BRIDGE", "url": "wss://...", "pin": "..." }` or manual Cloud URL input.
- Produces: Secure WSS connection with immediate PIN authentication.

- [x] **Step 1: Update `LanBridgeClient.connect` to support Cloud URLs and PIN**
  - Detect if target starts with `wss://` or `ws://` or contains domain without port.
  - If URL contains domain (e.g. `trycloudflare.com`), construct proper `wss://<domain>`.
  - Store optional `pin: String?`.
  - On WebSocket `onOpen`, if `pin != null`, immediately dispatch `{"type": "AUTH", "pin": pin}`.
  - Listen for `AUTH_SUCCESS` and `AUTH_FAILED` messages and reflect in `ConnectionStatus`.
- [x] **Step 2: Update QR Scanner to handle Cloud QR payloads**
  - Support parsing JSON QR with `url` and `pin` fields alongside legacy `{ip, port}`.
- [x] **Step 3: Update `ConnectionDialog`**
  - Support entering domain URL or IP, and an optional 6-digit PIN field.
- [x] **Step 4: Build and test compilation with `./gradlew assembleDebug`**

---

### Task 5: Compilation, Packaging & Verification

**Files:**
- Target 1: `D:\PROJECT\GAWEAN\PROJECT\EXT\antiremote\antigravity-remote-bridge-1.0.0.vsix`
- Target 2: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\build\outputs\apk\debug\app-debug.apk`

- [x] **Step 1: Build and package VS Code extension**
  - Run `npm run build` / `esbuild --production`.
  - Package new `.vsix` using `vsce package`.
  - Deploy directly to `C:\Users\kngadit\.antigravity-ide\extensions\antigravity.antigravity-remote-bridge-1.0.0\dist\extension.js`.
- [x] **Step 2: Compile Android APK**
  - Run `./gradlew assembleDebug` with JDK 17.
- [x] **Step 3: Verify end-to-end functionality**
  - Verify Cloudflare tunnel starts and displays URL in dashboard.
  - Verify QR Code generates valid cloud payload.
  - Verify socket authentication passes with PIN and rejects without PIN.
