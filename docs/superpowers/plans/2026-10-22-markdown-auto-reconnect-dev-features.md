# Markdown Chat, Auto-Reconnect & Developer Power Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform Antiremote into a resilient, developer-first mobile companion by adding native Rich Markdown rendering, eliminating empty tool bubbles, keeping connections persistent across app minimize via auto-reconnect, and providing voice dictation with rapid dev shortcuts.

**Architecture:** 
1. UI Layer: Introduce a modular Jetpack Compose `MarkdownRenderer` for headings, lists, inline code, syntax-styled code blocks with 1-tap copy, and clean tool status badges that collapse empty messages.
2. Persistence Layer: Introduce `ConnectionPreferences` storing the last successful target host, port, and security PIN in Android `SharedPreferences`.
3. Network Layer: Upgrade `LanBridgeClient` with an exponential backoff auto-reconnect loop (1s, 2s, 5s) and resilient ping heartbeat keepalive.
4. Service Layer: Update `BridgeNotificationService` to maintain foreground lifecycle during reconnections instead of dying on temporary network drops.
5. Dev Tools: Wire speech-to-text dictation into the prompt input bar using `RecognizerIntent` and add rapid developer control actions.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android SDK 34/36, OkHttp WebSockets, Android SpeechRecognizer, SharedPreferences, Robolectric / JUnit 4.

**Spec:** User alignment from Grill-Me session covering Q1 (Markdown), Q2 (Auto-Reconnect & Persistence), Q3 (Keep-Alive Service), Q4 (Dev Tools & Voice Dictation), and Q5 (Tool Badge & Empty Bubble Cleanup).

## Global Constraints
- Zero crash policy: All background, notification, and parsing operations must be wrapped in safe failure handlers.
- Native Compose only: No heavy Android WebView dependencies for markdown rendering.
- Offline and local safety: Connection credentials remain on-device in app-private SharedPreferences.
- Backward compatibility: Existing QR code scanning and manual IP pairing continue working seamlessly.

## Review Focus
1. Rapid streaming text: As tokens stream from the AI Agent, markdown parser must not flicker, drop state, or crash on partial markdown delimiters (e.g. unclosed `**` or ```` ``` ````).
2. Minimize & Resume: When the user switches to another app or locks the phone, the WebSocket should gracefully reconnect without dropping the session or requiring QR rescan.
3. Empty tool messages: History containing tool executions with empty text (`text: ""`) must never render empty dark bubble containers.
4. Voice input cancellation: If speech recognition is cancelled or permission is denied, UI must return to standard text input without errors.
5. Reconnect loop throttling: Network disconnects must back off exponentially and avoid flooding the CPU or draining battery.

---

### Task 1: Persistent Connection Profile Storage (`ConnectionPreferences.kt`)

**Files:**
- Create: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\data\storage\ConnectionPreferences.kt`
- Test: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\test\java\com\example\ConnectionPreferencesTest.kt`

**Interfaces:**
- Produces: `ConnectionPreferences`
  - `saveConnection(host: String, port: Int, pin: String?)`
  - `getLastConnection(): SavedConnection?`
  - `clearConnection()`
  - `data class SavedConnection(val host: String, val port: Int, val pin: String?, val lastConnectedAt: Long)`

- [ ] **Step 1: Write unit test for `ConnectionPreferences`**
  - Verify saving host, port, and PIN, and retrieving them.
  - Verify null PIN and default values.
- [ ] **Step 2: Run test to verify it fails**
  - Run `./gradlew testDebugUnitTest --tests com.example.ConnectionPreferencesTest`
  - Expected: Compilation/resolution failure.
- [ ] **Step 3: Implement `ConnectionPreferences`**
  - Implement using `Context.getSharedPreferences("antiremote_prefs", Context.MODE_PRIVATE)`.
- [ ] **Step 4: Run test to verify it passes**
  - Run `./gradlew testDebugUnitTest --tests com.example.ConnectionPreferencesTest`
  - Expected: PASS
- [ ] **Step 5: Commit**
  - `git add app/src/main/java/com/example/data/storage/ app/src/test/java/com/example/ConnectionPreferencesTest.kt`
  - `git commit -m "feat: add ConnectionPreferences for persistent host and PIN storage"`

---

### Task 2: Auto-Reconnect Engine & Heartbeat Keepalive (`LanBridgeClient.kt`)

**Files:**
- Modify: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\data\network\LanBridgeClient.kt`
- Modify: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\ui\viewmodel\AntigravityViewModel.kt`

**Interfaces:**
- Consumes: `ConnectionPreferences`
- Produces:
  - `LanBridgeClient.connectWithPersistence(context: Context, target: String, port: Int, pin: String?)`
  - `LanBridgeClient.reconnectLastKnown()`
  - Auto-reconnect coroutine job with exponential backoff on `onFailure` and `onClosed` (when not user-cancelled).
  - Status transition to `ConnectionStatus.CONNECTING` with message *"Menghubungkan kembali (Percobaan X)..."*

- [ ] **Step 1: Add reconnect state and auto-reconnect logic to `LanBridgeClient`**
  - Store `lastTarget`, `lastPort`, `lastPin`, and `isExplicitDisconnect` flag.
  - In `onFailure` / `onClosed`, if `!isExplicitDisconnect`, launch coroutine with exponential delay (1s, 2s, 5s, max 10s) to call `connect(target, port, pin)`.
  - In `disconnect()`, set `isExplicitDisconnect = true` and cancel reconnect jobs.
- [ ] **Step 2: Save credentials automatically on successful handshake (`onOpen`)**
  - Persist valid credentials via `ConnectionPreferences`.
- [ ] **Step 3: Update `AntigravityViewModel` to auto-restore last connection on startup**
  - If saved host exists in `ConnectionPreferences`, pre-populate `uiState.targetIpInput`, `targetPortInput`, and provide 1-tap reconnect.
- [ ] **Step 4: Verify test suite and build**
  - Run `./gradlew testDebugUnitTest`
  - Expected: PASS
- [ ] **Step 5: Commit**
  - `git add app/src/main/java/com/example/data/network/ app/src/main/java/com/example/ui/viewmodel/`
  - `git commit -m "feat: add auto-reconnect engine with exponential backoff and persistence"`

---

### Task 3: Resilient Background Service Continuity (`BridgeNotificationService.kt`)

**Files:**
- Modify: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\service\BridgeNotificationService.kt`
- Modify: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\MainActivity.kt`

**Interfaces:**
- Consumes: `ConnectionStatus`
- Produces: Graceful reconnecting notification badge; holds FGS alive for up to 60s when disconnected before stopping.

- [ ] **Step 1: Update `observeClientEvents` in `BridgeNotificationService`**
  - When status is `CONNECTING`, update foreground notification to *"Menghubungkan kembali ke Workstation..."*.
  - When status is `DISCONNECTED` or `ERROR`, start a 60-second grace timer before shutting down service. If reconnected within 60s, cancel grace timer and resume active notification.
- [ ] **Step 2: Update `MainActivity.kt` LaunchedEffect**
  - Do not immediately call `BridgeNotificationService.stop` on transient errors while auto-reconnecting.
- [ ] **Step 3: Run service lifecycle unit test**
  - Run `./gradlew testDebugUnitTest --tests com.example.BridgeNotificationServiceTest`
  - Expected: PASS
- [ ] **Step 4: Commit**
  - `git add app/src/main/java/com/example/service/ app/src/main/java/com/example/MainActivity.kt`
  - `git commit -m "feat: prevent foreground service drop during background reconnects"`

---

### Task 4: Rich Markdown Parser & Syntax Code Blocks (`MarkdownRenderer.kt`)

**Files:**
- Create: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\ui\components\MarkdownRenderer.kt`
- Test: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\test\java\com\example\MarkdownParserTest.kt`

**Interfaces:**
- Produces: `@Composable fun RichMarkdownText(content: String, modifier: Modifier, isStreaming: Boolean)`
  - Supports:
    - Headers (`# `, `## `, `### `) with bold typography and scaled sizes.
    - Inline formatting: `**bold**`, `*italic*`, `` `inline code` `` with pill background.
    - Numbered and bulleted lists (`- `, `* `, `1. `) with indentation.
    - Code blocks with language tag, dark theme, copy button, and horizontal scrolling.
    - File links (`file:///...`) and URLs with interactive styling.

- [ ] **Step 1: Write unit tests for Markdown segment parser**
  - Test header extraction, list identification, inline tokenizing, and code block splitting.
- [ ] **Step 2: Run test to verify it fails**
  - Run `./gradlew testDebugUnitTest --tests com.example.MarkdownParserTest`
- [ ] **Step 3: Implement `MarkdownRenderer.kt`**
  - Build pure Compose Composable with `AnnotatedString` builder and styled code block cards.
- [ ] **Step 4: Run test to verify it passes**
  - Run `./gradlew testDebugUnitTest --tests com.example.MarkdownParserTest`
  - Expected: PASS
- [ ] **Step 5: Commit**
  - `git add app/src/main/java/com/example/ui/components/MarkdownRenderer.kt app/src/test/java/com/example/MarkdownParserTest.kt`
  - `git commit -m "feat: add native Jetpack Compose Rich Markdown renderer"`

---

### Task 5: Chat Screen Overhaul: Clean Badges & Empty Bubble Elimination (`AgentChatScreen.kt`)

**Files:**
- Modify: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\ui\screens\AgentChatScreen.kt`

**Interfaces:**
- Consumes: `RichMarkdownText` from Task 4.
- Produces:
  - Empty text messages with tool badges are rendered as **compact single-line tool activity chips** instead of dark empty bubbles.
  - Completed tool steps display a static `✓ Selesai: <toolName>` without ongoing spinning animations.
  - Spinning `CircularProgressIndicator` renders ONLY when `isCurrentlyStreaming == true`.
  - Full rich markdown for all AI response text.

- [ ] **Step 1: Replace raw text in `StreamingChatBubble` with `RichMarkdownText`**
- [ ] **Step 2: Handle empty text tool messages**
  - If `message.text.isBlank()` and `toolBadge != null`: render a sleek compact badge chip (e.g. `🔧 run_command`) rather than an empty large bubble container.
- [ ] **Step 3: Static checkmark for completed tool calls**
  - When `isCurrentlyStreaming == false`, show static checkmark icon `Icons.Default.Check` instead of spinning `CircularProgressIndicator`.
- [ ] **Step 4: Verify layout in tests and assemble**
  - Run `./gradlew testDebugUnitTest`
  - Expected: PASS
- [ ] **Step 5: Commit**
  - `git add app/src/main/java/com/example/ui/screens/AgentChatScreen.kt`
  - `git commit -m "feat: overhaul chat bubbles with rich markdown and clean tool chips"`

---

### Task 6: Remote Dev Power Tools: Voice-to-Prompt & Rapid Git/Build Actions

**Files:**
- Modify: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\ui\screens\AgentChatScreen.kt`
- Modify: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\MainActivity.kt`
- Modify: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\src\main\java\com\example\ui\components\ConnectionDialog.kt`

**Interfaces:**
- Consumes: Android `SpeechRecognizer` (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`)
- Produces:
  - Microphone button in chat bar activates native Android speech-to-text dictation.
  - Dedicated 1-tap Reconnect button in `ConnectionDialog` displaying the last target (e.g. *"Hubungkan ke 192.168.1.5:9876"*).
  - Quick developer action triggers (git status, dev server, test).

- [ ] **Step 1: Wire SpeechRecognizer to Mic button in `AgentChatScreen.kt` & `MainActivity.kt`**
  - Register `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())` for `RecognizerIntent`.
  - On speech result, append recognized text to the chat input field.
- [ ] **Step 2: Add 1-Tap Reconnect card to `ConnectionDialog.kt`**
  - If a saved connection exists, display a high-priority "Sambungkan Ulang Cepat" button at top of dialog.
- [ ] **Step 3: Assemble and verify debug APK**
  - Run `./gradlew assembleDebug`
- [ ] **Step 4: Commit**
  - `git add app/src/main/`
  - `git commit -m "feat: add voice dictation and 1-tap quick reconnect to remote client"`

---

### Task 7: Full Verification & APK Assembly

**Files:**
- Target: `d:\PROJECT\GAWEAN\PROJECT\APK\Antiremote\app\build\outputs\apk\debug\app-debug.apk`

- [ ] **Step 1: Run complete test suite**
  - Run `./gradlew testDebugUnitTest`
- [ ] **Step 2: Compile fresh APK**
  - Run `./gradlew assembleDebug`
- [ ] **Step 3: Verify output APK binary exists and is up to date**
