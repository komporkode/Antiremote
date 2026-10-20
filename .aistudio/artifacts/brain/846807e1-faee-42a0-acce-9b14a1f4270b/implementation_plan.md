# Antigravity Remote IDE Controller

Aplikasi mobile Android futuristik untuk memantau, mengendalikan, dan menyinkronkan Google Antigravity IDE secara real-time melalui Direct Local LAN Bridge (IP & Port WebSocket/HTTP), dilengkapi dashboard telemetri multi-panel, terminal interaktif, code viewer/editor, dan orkestrasi agen AI.

## User Review & Critical Decisions

> [!IMPORTANT]
> Keputusan penting berdasarkan klarifikasi kebutuhan pengguna:

- **Confirmed Decision (Koneksi & Sinkronisasi)**: Menggunakan **Direct Local LAN Bridge** melalui IP address dan Port target (mendukung WebSocket streaming real-time berlatensi ultra-rendah dan HTTP fallback). Disertai fitur Auto-Discovery/Ping test dan built-in Simulated Live IDE Bridge Mode untuk demonstrasi langsung tanpa server eksternal.
- **Confirmed Decision (Fitur Utama)**: **Dashboard Terpadu 4 Pilar**:
  1. **Terminal Console & Execution**: Live stream terminal output, input command cepat (run, build, test, stop, git), dan filter log.
  2. **Code & Workspace Explorer**: File explorer proyek, syntax-highlighted code viewer/editor dengan tombol push/save ke IDE.
  3. **AI Agent Commander**: Pemantau status instruksi agen Antigravity, antrean task, step debugger, dan prompt dispatch.
  4. **System Telemetry & Metrics**: Real-time gauge untuk CPU, RAM memory heap, I/O latency, FPS render, dan status koneksi socket.
- **Confirmed Decision (Gaya Visual)**: **Modern Antigravity Dark Sci-Fi dengan aksen Neon** (Dark Obsidian `#090D16`, Neon Cyan `#00F5FF`, Matrix Green `#10B981`, Laser Purple `#8B5CF6`, dan Cyber Amber `#F59E0B`).

---

## 1. Overview & Core Concept

- **What It Does**: Memberikan developer kendali penuh atas Google Antigravity IDE langsung dari smartphone atau tablet Android. Pengguna dapat menghubungkan perangkat ke IP workstation IDE (misal `192.168.1.50:8080`), memantau proses build, memicu tugas agen AI otomatis, mengeksekusi perintah shell terminal, dan meninjau perubahan source code secara instan.
- **Target Audience**: Software engineer, prompt engineer, dan developer modern yang menggunakan lingkungan Google Antigravity IDE dan membutuhkan monitor portabel saat compiling atau membiarkan AI agent bekerja.
- **Key Value**: Mobilitas tinggi tanpa kehilangan visibilitas performa IDE, latensi sinkronisasi minimal di jaringan lokal (LAN), dan visualisasi futuristik yang nyaman di mata.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Connection & Discovery**:
   - Pengguna disambut layar Connection Hub dengan kartu status sinyal real-time.
   - Pilihan Quick Connect (memori riwayat koneksi sebelumnya, default port 8080/9090) atau Auto-Scan LAN.
   - Opsi instan "Launch Antigravity Demo Session" untuk mencoba seluruh fitur interaktif secara instan.
2. **Main Unified Dashboard (Tab Bar / Bottom Navigation)**:
   - **Tab 1: Telemetry & Overview**: Tampilan gauge futuristik melingkar untuk CPU Core IDE, Memory Heap, Latensi Jaringan (<15ms ping indicator), Status Server Antigravity, dan Activity Feed terkini.
   - **Tab 2: Agent Commander**: Visualisasi status agen (Thinking, Writing Code, Executing Command, Idle), timeline eksekusi langkah AI, riwayat prompt, dan kotak kirim instruksi baru ke agen.
   - **Tab 3: Terminal Console**: Jendela terminal console dengan syntax ANSI coloring, shortcut tombol cepat (`gradle build`, `npm run dev`, `git status`, `git pull`, `kill -9`), auto-scroll toggle, dan clear buffer.
   - **Tab 4: Workspace Code Viewer**: Tree explorer file project Antigravity (`src/`, `config/`, dll.), tab file aktif, nomor baris, visual diff highlight, dan mini-editor modal.
3. **Session Controls & Emergency Actions**:
   - Floating Action Bar di bagian atas untuk status konektivitas: tombol Quick Reconnect, Pause Agent, Stop Build, dan Settings.

### Visual Identity & Theme Tokens

- **Aesthetic Direction**: *Antigravity Cyberdeck HUD* — presisi teknikal tinggi, rounded corner modern (12-16dp), border neon lembut dengan efek glowing semi-transparan, tanpa visual clutter.
- **Color Palette**:
  - `Background`: `#080C14` (Deep Space Dark)
  - `Surface / Container`: `#0E1524` (Cyber Steel Glass)
  - `Surface Variant / Card`: `#162035`
  - `Primary / Neon Cyan`: `#00F5FF` (Aksen utama tombol aktif, sinyal, status aktif)
  - `Secondary / Neon Mint`: `#00FF9D` (Status online, success build, terminal prompt)
  - `Tertiary / Laser Purple`: `#A855F7` (AI Agent badge, thought process stream)
  - `Warning / Cyber Orange`: `#F59E0B` (Warning alert, memory threshold warning)
  - `Error / Neon Red`: `#FF3B69` (Crash, build fail, disconnected)
  - `Text Primary`: `#F1F5F9`
  - `Text Muted`: `#64748B`
- **Typography**:
  - Header & Metric: Monospace & High-Tech Display (Bold, Clean, tabular numerals).
  - Terminal & Code: Clean Monospace font dengan styling high-contrast readability.
  - Body & Labels: Sans-Serif clean (Inter/Roboto default scaleable).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct Local LAN Bridge vs Cloud Relay Proxy**:
  - *Chosen Approach*: Direct Socket & HTTP REST bridge ke IP lokal workstation Antigravity IDE (`ws://<ip>:<port>/stream` dan `http://<ip>:<port>/api`), dilengkapi fallback built-in Interactive Engine.
  - *Why*: Latensi sub-10 milidetik, tidak bergantung kuota cloud eksternal atau server perantara, data kode aman tetap di jaringan privat lokal pengguna.
  - *Alternatives Considered*: Firebase Cloud Firestore atau relay server publik (ditolak sesuai preferensi eksplisit user yang memilih Direct LAN).
- **Decision 2: Dual Mode (Live Network Socket + Interactive Simulator)**:
  - *Chosen Approach*: Arsitektur Repository yang mendukung koneksi LAN sungguhan ke socket IDE, dengan tombol satu ketukan "Demo / Simulation Mode" yang menghasilkan streaming log, metrik dinamis, dan respons agen AI jika pengguna sedang tidak membuka IDE di PC.
  - *Why*: Memastikan aplikasi selalu dapat diuji dan berfungsi penuh dalam streaming preview AI Studio maupun di perangkat fisik saat terhubung ke PC.
- **Decision 3: Jetpack Compose State Architecture**:
  - *Chosen Approach*: Clean Architecture (Repository -> ViewModel -> UI State Sealed Classes). State terpusat di `AntigravityViewModel` dengan Kotlin Coroutines & StateFlow.

---

## 4. Technical Architecture & Data Strategy

### System & Component Hierarchy Diagram

```
┌───────────────────────────────────────────────────────────────┐
│                    MainActivity (Edge-to-Edge)                │
└───────────────────────────────┬───────────────────────────────┘
                                │
                 ┌──────────────┴──────────────┐
                 │    AntigravityMainScreen     │
                 └──────────────┬──────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│ Top HUD Bar  │        │ Tab Content  │        │ Bottom Nav   │
│ IP & Status  │        │   Manager    │        │  (4 Dest.)   │
└──────────────┘        └───────┬──────┘        └──────────────┘
                                │
        ┌───────────────┬───────┴───────┬───────────────┐
        ▼               ▼               ▼               ▼
┌──────────────┐┌──────────────┐┌──────────────┐┌──────────────┐
│  Telemetry   ││  Terminal    ││  Workspace   ││   AI Agent   │
│  Dashboard   ││ Live Console ││ File/Code    ││  Commander   │
│ (CPU/RAM/Net)││ (ANSI stream)││ (Viewer/Edit)││(Tasks/Steps) │
└──────────────┘└──────────────┘└──────────────┘└──────────────┘
                                │
                        ┌───────▼───────┐
                        │ AntigravityVM │
                        └───────┬───────┘
                                │
                 ┌──────────────┴──────────────┐
                 ▼                             ▼
     ┌───────────────────────┐     ┌───────────────────────┐
     │  LanBridgeRepository  │     │  Local Preferences    │
     │  (OkHttp WS & HTTP)   │     │ (Saved IP/Port Hist.) │
     └───────────────────────┘     └───────────────────────┘
```

### Data Models & State Contracts

- **ConnectionConfig**: `ipAddress: String`, `port: Int`, `isConnected: Boolean`, `pingMs: Long`, `isDemoMode: Boolean`.
- **SystemTelemetry**: `cpuUsage: Float`, `memoryUsedMb: Int`, `memoryTotalMb: Int`, `activeThreads: Int`, `fps: Float`, `uptimeSec: Long`.
- **TerminalLog**: `id: String`, `timestamp: String`, `level: LogLevel (INFO, WARN, ERROR, COMMAND)`, `text: String`.
- **AgentTask**: `id: String`, `title: String`, `status: AgentStatus (PLANNING, EDITING, TESTING, COMPLETE)`, `currentStep: String`, `progress: Float`.
- **WorkspaceFile**: `path: String`, `name: String`, `isDirectory: Boolean`, `content: String`, `language: String`.

---

## Verifikasi & Rencana Validasi

1. **Kompilasi**: Menjalankan `compile_applet` untuk memastikan seluruh komponen Jetpack Compose, Material 3, coroutines, dan model data terkompilasi tanpa error.
2. **Edge-to-Edge & Accessibility**: Memastikan penanganan `WindowInsets`, padding navigasi bawah, dan touch target minimal 48dp.
3. **Interaktivitas Penuh**: Memastikan setiap tombol command terminal, tombol pengiriman prompt agen, switch file editor, dan koneksi IP dapat dieksekusi dengan responsif.
