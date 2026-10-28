# App Redesign, Project Name Sync, Review Tab Removal & Micro-Animations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign mobile companion UI based on user mockup, remove obsolete Review tab, synchronize active project name/git branch/dev port from VS Code extension, render inline diagnosis/patch cards with 1-tap apply, and add smooth micro-animations throughout the app.

**Architecture:**
- **VS Code Extension**: Enrich `WorkspaceManager` and `bridgeServer.ts` to detect and broadcast `WORKSPACE_STATE` (project name, git branch, dev port, sync status) both on handshake (`WELCOME`) and dynamically on git/workspace changes.
- **Android Domain & State**: Remove `DashboardTab.REVIEW` from `DashboardTab` enum; enrich `WorkspaceInfo` with git branch and server port; handle `WORKSPACE_STATE` in `LanBridgeClient` and expose via `AntigravityViewModel`.
- **Android UI & Animations**:
  - **Dock Navigation**: 4 tabs (`Chat`, `Tasks`, `Terminal`, `Preview`) with `AnimatedContent` slide/fade page transitions.
  - **Header & HUD**: Redesign `TopHudBar` and `AgentChatScreen` header to display `Chat`, subtitle `• [ProjectName] • :[Port]` with animated breathing pulse dot, and git branch status capsule chip.
  - **Quick Action Chips**: Scrollable chips (`Test & Fix`, `Git Diff`, `Commit & Push`, `Auto-fix`) above input bar with tactile scale/bounce micro-animations.
  - **Diagnosis & Patch Cards**: Render assistant diagnosis messages with dedicated `DIAGNOSIS` tag, `SOLUSI & STATUS` summary, syntax-tinted diff box, and interactive `[Apply Patch]` (with morphing check animation) + `[Salin]` buttons.

**Tech Stack:** Kotlin, Jetpack Compose, Coroutines, StateFlow, Compose Animation APIs (`AnimatedContent`, `animateFloatAsState`, `infiniteRepeatable`), TypeScript, VS Code Extension API.

**Spec:** User Mockup (`media_1793174742024.png`) and settled Grilling decisions (Q1-Q5 + animations).

## Global Constraints
- Target Android SDK: compileSdk 34, minSdk 26, Kotlin 1.9.22, Compose Compiler 1.5.8.
- Extension: TypeScript target ES2022, Node 18+, WebSocket (ws).
- Do not introduce breaking changes to existing terminal or WebSocket protocol commands (`CHAT_PROMPT`, `AGENT_APPROVAL_RESPONSE`).
- Ensure all tests pass via `./gradlew testDebugUnitTest`.

## Review Focus
1. Workspace with no git repository initialized: fallback to default branch `"main"` without throwing exception.
2. Workspace folder name containing spaces or empty workspace: display `"Antiremote"` fallback instead of blank/null.
3. Switching tabs rapidly during active streaming: `AnimatedContent` transitions smoothly without UI flicker or recomposition churn.
4. Agent message with malformed or partial diff block: syntax diff renderer falls back gracefully to standard markdown block without crash.
5. `Apply Patch` button tapped multiple times rapidly: debounce action so patch instruction is sent only once with immediate visual loading/applied state.

---

### Task 1: Extension - Workspace State Sync (Project Name, Git Branch, Dev Port)

**Files:**
- Modify: `D:\PROJECT\GAWEAN\PROJECT\EXT\antiremote\src\types.ts`
- Modify: `D:\PROJECT\GAWEAN\PROJECT\EXT\antiremote\src\workspaceManager.ts`
- Modify: `D:\PROJECT\GAWEAN\PROJECT\EXT\antiremote\src\bridgeServer.ts`

**Interfaces:**
- Produces: `WORKSPACE_STATE` message payload `{ type: 'WORKSPACE_STATE', projectName: string, gitBranch: string, devPort: number | null, isSync: boolean, timestamp: number }`.
- Produces: Updated `WelcomeMessage` payload containing `gitBranch: string` and `devPort: number | null`.

- [ ] **Step 1: Update TypeScript types in `types.ts`**
Add `gitBranch` and `devPort` to `WelcomeMessage`, and declare `WorkspaceStateMessage`:
```typescript
export interface WorkspaceStateMessage {
  type: 'WORKSPACE_STATE';
  projectName: string;
  gitBranch: string;
  devPort: number | null;
  isSync: boolean;
  timestamp: number;
}
```

- [ ] **Step 2: Add git branch detection to `workspaceManager.ts`**
Implement `getGitBranch(rootPath: string): Promise<string>` using `git rev-parse --abbrev-ref HEAD` with fallback to `"main"`, and add `gitBranch` to `getWorkspaceSummary()`.

- [ ] **Step 3: Update `bridgeServer.ts` to broadcast `WORKSPACE_STATE`**
In `bridgeServer.ts`:
- Include `gitBranch` in `WELCOME` payload.
- Add `broadcastWorkspaceState()` method that reads latest workspace info and emits `WORKSPACE_STATE` to all connected clients.
- Trigger `broadcastWorkspaceState()` when a client connects and when `vscode.workspace.onDidChangeWorkspaceFolders` fires.

- [ ] **Step 4: Compile extension to verify types and build**
Run: `npm run compile` in `D:\PROJECT\GAWEAN\PROJECT\EXT\antiremote`
Expected: Compilation succeeds with 0 errors.

---

### Task 2: Android Domain & State - Remove Review Tab & Enrich WorkspaceInfo

**Files:**
- Modify: `app/src/main/java/com/example/ui/viewmodel/AntigravityUiState.kt:3-10`
- Modify: `app/src/main/java/com/example/data/model/WorkspaceInfo.kt:3-8`
- Modify: `app/src/main/java/com/example/data/network/LanBridgeClient.kt`
- Create: `app/src/test/java/com/example/antiremote/WorkspaceStateTest.kt`

**Interfaces:**
- Consumes: `WELCOME` and `WORKSPACE_STATE` JSON from WebSocket.
- Produces: `DashboardTab` with 4 values (`CHAT`, `TASKS`, `TERMINAL`, `PREVIEW`).
- Produces: `WorkspaceInfo(projectName: String, activeFileName: String, cursorLine: Int, gitBranch: String, devPort: Int?, isSync: Boolean)`.

- [ ] **Step 1: Write failing unit test for WorkspaceState & DashboardTab**
Create `app/src/test/java/com/example/antiremote/WorkspaceStateTest.kt`:
```kotlin
package com.example.antiremote

import com.example.data.model.WorkspaceInfo
import com.example.ui.viewmodel.DashboardTab
import org.junit.Assert.*
import org.junit.Test

class WorkspaceStateTest {
    @Test
    fun testDashboardTab_doesNotContainReview() {
        val tabNames = DashboardTab.values().map { it.name }
        assertFalse("DashboardTab should not contain REVIEW", tabNames.contains("REVIEW"))
        assertEquals(4, DashboardTab.values().size)
    }

    @Test
    fun testWorkspaceInfo_hasBranchAndPort() {
        val info = WorkspaceInfo(
            projectName = "Dengar",
            activeFileName = "App.tsx",
            cursorLine = 12,
            gitBranch = "feature/mobile-sync",
            devPort = 3000,
            isSync = true
        )
        assertEquals("Dengar", info.projectName)
        assertEquals("feature/mobile-sync", info.gitBranch)
        assertEquals(3000, info.devPort)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `.\gradlew testDebugUnitTest --tests com.example.antiremote.WorkspaceStateTest`
Expected: FAIL (because `REVIEW` is still in `DashboardTab` and `WorkspaceInfo` lacks `gitBranch`).

- [ ] **Step 3: Update `AntigravityUiState.kt` and `WorkspaceInfo.kt`**
- In `AntigravityUiState.kt`, remove `REVIEW("Review")` from `DashboardTab`.
- In `WorkspaceInfo.kt`, add `gitBranch: String = "main"`, `devPort: Int? = 3000`, `isSync: Boolean = true`.

- [ ] **Step 4: Update `LanBridgeClient.kt` to parse `WORKSPACE_STATE` and updated `WELCOME`**
In `LanBridgeClient.kt`:
- In `"WELCOME" ->`:
  Extract `gitBranch = json.optString("gitBranch", "main")` and `devPort = if (json.has("devPort")) json.optInt("devPort") else 3000`.
- Add handler `"WORKSPACE_STATE" ->`:
  Parse `projectName`, `gitBranch`, `devPort`, and `isSync`, then call `_workspaceInfo.update { ... }`.

- [ ] **Step 5: Run unit test to verify it passes**
Run: `.\gradlew testDebugUnitTest --tests com.example.antiremote.WorkspaceStateTest`
Expected: PASS.

---

### Task 3: Android UI - Redesign Navigation Dock (4 Tabs) with Animated Tab Transitions

**Files:**
- Modify: `app/src/main/java/com/example/MainActivity.kt:210-380`

**Interfaces:**
- Consumes: `DashboardTab` (4 tabs).
- Produces: Smooth animated tab transition container `AnimatedContent(targetState = uiState.selectedTab)`.

- [ ] **Step 1: Remove Review tab references in `MainActivity.kt`**
- Remove `DashboardTab.REVIEW` case from bottom bar icon mapper.
- Remove `DashboardTab.REVIEW` badge check.
- Remove `DashboardTab.REVIEW -> { ReviewDiffScreen(...) }` from screen selector.

- [ ] **Step 2: Wrap tab screen transitions in `AnimatedContent`**
In `MainActivity.kt`:
Replace static `when (uiState.selectedTab)` with `AnimatedContent`:
```kotlin
AnimatedContent(
    targetState = uiState.selectedTab,
    transitionSpec = {
        if (targetState.ordinal > initialState.ordinal) {
            (slideInHorizontally { width -> width / 3 } + fadeIn(tween(200)))
                .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(150)))
        } else {
            (slideInHorizontally { width -> -width / 3 } + fadeIn(tween(200)))
                .togetherWith(slideOutHorizontally { width -> width / 3 } + fadeOut(tween(150)))
        }
    },
    label = "TabContentTransition"
) { tab ->
    when (tab) {
        DashboardTab.CHAT -> AgentChatScreen(...)
        DashboardTab.TASKS -> TasksScreen(...)
        DashboardTab.TERMINAL -> InteractiveTerminalScreen(...)
        DashboardTab.PREVIEW -> WebLivePreviewScreen(...)
    }
}
```

- [ ] **Step 3: Verify build**
Run: `.\gradlew compileDebugKotlin`
Expected: Build succeeds without missing tab reference errors.

---

### Task 4: Android UI - Redesign Top Bar & Header with Pulsing Live Status & Git Branch Chip

**Files:**
- Modify: `app/src/main/java/com/example/ui/components/TopHudBar.kt`
- Modify: `app/src/main/java/com/example/ui/screens/AgentChatScreen.kt`

**Interfaces:**
- Consumes: `workspaceInfo: WorkspaceInfo`, `config: ConnectionConfig`, `isStreaming: Boolean`.
- Produces: Polished top bar adhering to mockup: Title `Chat`, subtitle `• [ProjectName] • :[Port]`, and branch pill chip `• [branch] sync`.

- [ ] **Step 1: Add live breathing pulse animation**
In `TopHudBar.kt`:
Create `rememberInfiniteTransition` for the green status dot:
```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
        animation = tween(800, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    ),
    label = "PulseAlpha"
)
```

- [ ] **Step 2: Redesign layout to match Mockup Header**
- Left section:
  - Header text: `Chat` (FontWeight.Bold, fontSize 18.sp, ContentPrimary).
  - Subtitle row:
    - Pulsing dot (size 7.dp, CircleShape, AccentSuccess with `alpha = pulseAlpha`).
    - Project name: `workspaceInfo.projectName.ifEmpty { "Dengar" }`.
    - Dot divider: `•`.
    - Dev port: `:${workspaceInfo.devPort ?: 3000}`.
- Middle/Right section:
  - Git Branch Pill Chip:
    - Surface with `RoundedCornerShape(12.dp)`, `border = 1.dp CardBorder`, background `CardSurfaceVariant`.
    - Icon: Small Git branch / sync icon.
    - Text: `${workspaceInfo.gitBranch.ifEmpty { "antiremote / main" }} sync`.
- Right section:
  - Minimal connection settings icon and focus mode button.

- [ ] **Step 3: Verify build**
Run: `.\gradlew compileDebugKotlin`
Expected: Compilation succeeds.

---

### Task 5: Android UI - Quick Action Chips with Micro-Animations & Diagnosis/Patch Cards

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/AgentChatScreen.kt`
- Modify: `app/src/main/java/com/example/ui/components/ChatBubble.kt`

**Interfaces:**
- Consumes: User tap on Quick Action Chip -> triggers `onSendPrompt(actionPrompt)`.
- Consumes: Assistant message with diagnosis / diff -> renders structured card with `Apply Patch` and `Salin` buttons.

- [ ] **Step 1: Add Quick Action Chips above Chat Input in `AgentChatScreen.kt`**
Add horizontal scrolling chip bar directly above `ChatInputBar`:
- Chips: `[▷ Test & Fix]`, `[📄 Git Diff]`, `[☁ Commit & Push]`, `[⚙ Auto-fix]`.
- Add tactile press animation:
  Animate button scale `animateFloatAsState` on click: `Modifier.scale(if (isPressed) 0.94f else 1.0f)`.
- When clicked, call `onSendPrompt` with corresponding instruction.

- [ ] **Step 2: Implement Diagnosis & Diff Card Component in `ChatBubble.kt`**
Create `DiagnosisPatchCard`:
- Header: Badge `DIAGNOSIS` (purple/indigo surface, `RoundedCornerShape(4.dp)`).
- Body: `SOLUSI & STATUS` summary text.
- Code Diff Box:
  - Dark container (`Surface(color = Color(0xFF14171F), shape = RoundedCornerShape(8.dp))`).
  - Filename bar with file icon (e.g., `src/bridgeServer.ts`).
  - Monospace diff text with green (`+`) for additions and red (`-`) for deletions.
- Action Bar at bottom:
  - `[Apply Patch]` button (AccentPrimary, animated checkmark icon on click).
  - `[Salin]` button (Outlined secondary button, copies diff to clipboard with Toast).

- [ ] **Step 3: Add smooth entry animations to chat messages**
In `AgentChatScreen.kt`:
Apply `Modifier.animateItemPlacement()` on items in `LazyColumn` and wrap bubble in `AnimatedVisibility(visible = true, enter = fadeIn(tween(250)) + slideInVertically { it / 4 })`.

- [ ] **Step 4: Verify build**
Run: `.\gradlew compileDebugKotlin`
Expected: Compilation passes.

---

### Task 6: End-to-End Verification & Assembly

**Files:**
- All modified Android files.
- All modified VS Code Extension files.

- [ ] **Step 1: Run all unit tests**
Run: `.\gradlew testDebugUnitTest`
Expected: All tests pass.

- [ ] **Step 2: Build Debug APK**
Run: `.\gradlew assembleDebug`
Expected: `app-debug.apk` built successfully at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Compile and bundle VS Code Extension**
Run: In `D:\PROJECT\GAWEAN\PROJECT\EXT\antiremote`: `npm run compile`
Expected: Bundled extension ready.
