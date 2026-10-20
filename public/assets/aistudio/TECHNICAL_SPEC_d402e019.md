# DOKUMEN SPESIFIKASI & PANDUAN TEKNIS LENGKAP
# SISTEM REMOTE GOOGLE ANTIGRAVITY (LAN BRIDGE & ANDROID CLIENT)

> **Versi Dokumen:** 1.0.0  
> **Terakhir Diperbarui:** 2026-10-20  
> **Lingkungan:** PC Workstation (Google Antigravity / Code-OSS / VS Code) & Mobile (Android 8.0+)  
> **Protokol Jaringan:** WebSocket (RFC 6455) & HTTP/1.1 via Local Area Network (Wi-Fi/LAN)

---

## DAFTAR ISI

1. [Gambaran Umum Arsitektur Sistem](#1-gambaran-umum-arsitektur-sistem)
2. [Spesifikasi Komponen Workstation (PC / IDE)](#2-spesifikasi-komponen-workstation-pc--ide)
   - 2.1 Mode Ekstensi VS Code / Google Antigravity
   - 2.2 Mode Daemon Standalone Node.js
   - 2.3 Mekanisme Telemetri CPU & RAM PC
   - 2.4 Terminal Manager & Antigravity Agent Execution
3. [Spesifikasi Teknis Aplikasi Android](#3-spesifikasi-teknis-aplikasi-android)
   - 3.1 Stack Teknologi & Library Rekomendasi
   - 3.2 Manifest & Izin Sistem (Permissions)
   - 3.3 State Management & Arsitektur Aplikasi
4. [Protokol Komunikasi Jaringan & Pairing](#4-protokol-komunikasi-jaringan--pairing)
   - 4.1 Format Data QR Code Pairing (Auto-Connect)
   - 4.2 Endpoint HTTP Discovery & Health Check
   - 4.3 Siklus Hidup Koneksi WebSocket & Heartbeat
5. [Skema Pesan Lengkap (JSON Data Contracts)](#5-skema-pesan-lengkap-json-data-contracts)
   - 5.1 Pesan dari PC ke Android (Server -> Client)
   - 5.2 Pesan dari Android ke PC (Client -> Server)
6. [Implementasi Kode Lengkap Android (Kotlin)](#6-implementasi-kode-lengkap-android-kotlin)
   - 6.1 Scanner QR Code dengan Auto-Connect
   - 6.2 Service WebSocket Client (OkHttp)
   - 6.3 Data Models (Kotlin Data Classes)
7. [Panduan Operasional & Pemecahan Masalah (Troubleshooting)](#7-panduan-operasional--pemecahan-masalah-troubleshooting)

---

## 1. GAMBARAN UMUM ARSITEKTUR SISTEM

Sistem ini dirancang untuk menghubungkan workstation pengembangan yang menjalankan **Google Antigravity IDE** ke perangkat smartphone **Android** secara nirkabel melalui jaringan lokal (Wi-Fi / Hotspot yang sama).

### Diagram Alur Data:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        WORKSTATION / PC                                │
│                                                                        │
│  ┌────────────────────────┐         ┌──────────────────────────────┐  │
│  │ Google Antigravity IDE │         │ Antigravity Agent (agy CLI)  │  │
│  └───────────┬────────────┘         └──────────────┬───────────────┘  │
│              │                                     │                  │
│              ▼                                     ▼                  │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │         Antigravity Remote Bridge Server (Port 8080)            │  │
│  │   - HTTP Discovery Server (GET /info, GET /ping)                │  │
│  │   - WebSocket Server (ws://0.0.0.0:8080)                        │  │
│  │   - CPU & Memory Telemetry Poller (1 Detik)                     │  │
│  │   - Dedicated Terminal Runner ("Antigravity Remote")            │  │
│  │   - Dynamic QR Code Generator (JSON & Direct URL)               │  │
│  └─────────────────────────────────┬───────────────────────────────┘  │
└────────────────────────────────────┼──────────────────────────────────┘
                                     │
                     Wi-Fi / Local Area Network (TCP)
                     (WebSocket JSON Messages)
                                     │
┌────────────────────────────────────┼──────────────────────────────────┐
│                                    ▼                                  │
│                       APLIKASI MOBILE ANDROID                         │
│                                                                        │
│  ┌────────────────────────┐         ┌──────────────────────────────┐  │
│  │ QR Camera Scanner      ├────────►│ WebSocket Manager (OkHttp)   │  │
│  │ (Auto-parse & Connect) │         │ (Auto-Reconnect + Heartbeat) │  │
│  └────────────────────────┘         └──────────────┬───────────────┘  │
│                                                    │                  │
│              ┌─────────────────────────────────────┴───────────┐      │
│              ▼                                                 ▼      │
│  ┌─────────────────────────┐                     ┌──────────────────┐ │
│  │ Real-time Telemetry UI  │                     │ Agent Controller │ │
│  │ - CPU Gauge Meter       │                     │ - Prompt Box     │ │
│  │ - RAM Gauge Meter       │                     │ - Slash Commands │ │
│  │ - Active Workspace Info │                     │ - Quick Terminal │ │
│  └─────────────────────────┘                     └──────────────────┘ │
└───────────────────────────────────────────────────────────────────────┘
```

---

## 2. SPESIFIKASI KOMPONEN WORKSTATION (PC / IDE)

Workstation menyediakan layanan server bridge yang mendengarkan koneksi pada port default **`8080`**.

### 2.1 Mode Ekstensi VS Code / Google Antigravity

Ekstensi dibangun menggunakan TypeScript dan dibundel dengan `esbuild` menjadi file tunggal performa tinggi (`dist/extension.js`).

- **Entry Point:** `src/extension.ts`
- **Konfigurasi Pengaturan (`settings.json`):**
  | Kunci Pengaturan | Tipe Data | Nilai Default | Keterangan |
  | :--- | :--- | :--- | :--- |
  | `antiremote.port` | `integer` | `8080` | Port HTTP & WebSocket server. |
  | `antiremote.autoStart` | `boolean` | `true` | Mulai server otomatis saat IDE dibuka. |
  | `antiremote.telemetryInterval` | `integer` | `1000` | Interval broadcast telemetri (ms). |
  | `antiremote.allowTerminalExecution` | `boolean` | `true` | Izin eksekusi perintah terminal dari HP. |
  | `antiremote.allowVscodeCommands` | `boolean` | `true` | Izin eksekusi perintah internal IDE. |
  | `antiremote.authToken` | `string` | `""` | PIN rahasia untuk autentikasi keamanan. |

- **UI Ekstensi:**
  1. **Status Bar Widget:** Terletak di pojok kanan bawah editor (`$(radio-tower) Antiremote: 8080`). Menampilkan status aktif dan jumlah perangkat HP yang terhubung.
  2. **Dashboard Panel (Webview):** Menampilkan QR Code interaktif dengan switch format JSON/URL, live meteran CPU/RAM, log aktivitas, dan command tester.

### 2.2 Mode Daemon Standalone Node.js

Jika pengguna tidak membuka IDE, skrip `antigravity-bridge.js` dapat dijalankan secara langsung via CLI:
```bash
node antigravity-bridge.js
```
Skrip ini memiliki kapabilitas yang sama:
- Server HTTP dan WebSocket pada port `8080`.
- Merender **QR Code ASCII langsung di layar terminal** untuk scanning cepat.
- Eksekusi perintah menggunakan `child_process.exec` dengan penanganan error.

### 2.3 Mekanisme Telemetri CPU & RAM PC

Perhitungan beban CPU dilakukan secara akurat tanpa estimasi acak:
1. Membaca waktu CPU melalui `os.cpus()` pada interval $T_0$ dan $T_1$.
2. Menghitung selisih waktu idle ($\Delta \text{idle}$) dan selisih total waktu CPU ($\Delta \text{total}$).
3. Beban CPU dihitung dengan rumus:
   $$\text{CPU Usage (\%)} = \left(1 - \frac{\Delta \text{idle}}{\Delta \text{total}}\right) \times 100$$
4. RAM dihitung dari `os.totalmem()` dikurangi `os.freemem()` dan dikonversi ke satuan Megabyte (MB).

### 2.4 Terminal Manager & Antigravity Agent Execution

Perintah yang masuk dari HP dipisahkan ke terminal khusus bernama **`Antigravity Remote`**:
- Menghindari interupsi proses terminal yang sedang digunakan programmer di editor.
- Jika pengguna menutup terminal tersebut di IDE, ekstensi otomatis membuat terminal baru saat ada perintah berikutnya.
- Perintah dengan tipe `ANTIGRAVITY_COMMAND` otomatis dipasangi prefix CLI agent `agy` (misal: `agy review code`).

---

## 3. SPESIFIKASI TEKNIS APLIKASI ANDROID

### 3.1 Stack Teknologi & Library Rekomendasi

- **Bahasa:** Kotlin 1.9+
- **Min SDK:** Android 8.0 (API Level 26)
- **Target SDK:** Android 14/15 (API Level 34/35)
- **UI Toolkit:** Jetpack Compose (Material Design 3)
- **Networking Library:**
  - `com.squareup.okhttp3:okhttp:4.12.0` (Engine WebSocket)
  - `com.squareup.okhttp3:logging-interceptor:4.12.0` (Debug log)
- **QR Code Scanning:**
  - `com.journeyapps:zxing-android-embedded:4.3.0` (Paling cepat diintegrasikan) ATAU
  - `com.google.mlkit:barcode-scanning:17.3.0` (ML Kit modern berbasis CameraX)
- **JSON Serialization:** `org.json:json` (Bawaan Android SDK) atau `kotlinx.serialization`

### 3.2 Manifest & Izin Sistem (Permissions)

Tambahkan ke dalam `AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />

    <uses-feature android:name="android.hardware.camera" android:required="false" />

    <application
        android:allowBackup="true"
        android:usesCleartextTraffic="true"
        android:theme="@style/Theme.Material3.Dark">
        <!-- android:usesCleartextTraffic="true" WAJIB agar diizinkan koneksi WebSocket lokal (ws:// tanpa SSL) -->
        ...
    </application>
</manifest>
```

> **PENTING:** Atribut `android:usesCleartextTraffic="true"` harus diaktifkan karena koneksi ke bridge di jaringan lokal menggunakan protokol `ws://` dan `http://` tanpa sertifikat SSL publik.

---

## 4. PROTOKOL KOMUNIKASI JARINGAN & PAIRING

### 4.1 Format Data QR Code Pairing (Auto-Connect)

QR Code yang ditampilkan di PC menyandikan objek JSON berikut:
```json
{
  "app": "antigravity-remote",
  "ip": "192.168.1.10",
  "port": 8080,
  "wsUrl": "ws://192.168.1.10:8080",
  "httpUrl": "http://192.168.1.10:8080"
}
```

#### Logika Parser QR di Android:
1. Periksa apakah string dimulai dengan `{` dan mengandung kunci `"ip"`.
2. Jika ya, ambil `ip` dan `port`.
3. Jika berupa URL (`ws://...` atau `http://...`), lakukan parsing URI untuk mengekstrak host dan port.
4. Jika berupa teks format `192.168.1.10:8080`, lakukan split string dengan separator `:`.
5. Langsung panggil fungsi `connect(ip, port)`.

### 4.2 Endpoint HTTP Discovery & Health Check

Sebelum atau sesudah membuka socket, aplikasi Android dapat memanggil endpoint HTTP:

#### `GET http://<IP_PC>:8080/info`
Response Status `200 OK`:
```json
{
  "status": "ok",
  "server": "Antigravity LAN Bridge",
  "version": "1.0.0",
  "port": 8080,
  "ip": "192.168.1.10",
  "allIps": ["192.168.1.10"],
  "wsUrl": "ws://192.168.1.10:8080",
  "workspace": "my-project",
  "platform": "win32",
  "hostname": "KOMPUTER-USER",
  "connectedClients": 1,
  "authRequired": false
}
```

#### `GET http://<IP_PC>:8080/ping`
Response Status `200 OK`:
```text
pong
```

### 4.3 Siklus Hidup Koneksi WebSocket & Heartbeat

1. **Inisiasi Koneksi:** Android menghubungkan `OkHttpClient.newWebSocket` ke `ws://<IP_PC>:<PORT>`.
2. **Handshake Selesai:** Server mengirimkan pesan awal bertipe `WELCOME`.
3. **Loop Telemetri:** Server secara periodik mengirimkan paket `TELEMETRY` setiap 1000 milidetik.
4. **Heartbeat:** 
   - OkHttp secara otomatis mengelola frame `Ping`/`Pong` di level protokol WebSocket (set interval `pingInterval(5, TimeUnit.SECONDS)`).
   - Android juga dapat mengirimkan pesan manual `{"type":"PING"}` untuk menghitung latensi pulang-pergi (*Round-Trip Time*).
5. **Auto-Reconnect:** Jika koneksi terputus (`onFailure` atau `onClosed`), aplikasi Android memulai *retry timer* dengan exponential backoff (1s, 2s, 4s, maks 10s).

---

## 5. SKEMA PESAN LENGKAP (JSON DATA CONTRACTS)

### 5.1 Pesan dari PC ke Android (Server -> Client)

#### 1. Telemetri Real-time (`type: "TELEMETRY"`)
```json
{
  "type": "TELEMETRY",
  "cpuPercent": 28,
  "memoryUsedMb": 6420,
  "memoryTotalMb": 16384,
  "freeMemMb": 9964,
  "hostname": "DEVELOPER-PC",
  "platform": "win32",
  "workspaceName": "backend-service",
  "activeDocument": "UserController.ts",
  "timestamp": 1729432800000
}
```

#### 2. Pesan Selamat Datang (`type: "WELCOME"`)
```json
{
  "type": "WELCOME",
  "server": "Antigravity LAN Bridge",
  "version": "1.0.0",
  "hostname": "DEVELOPER-PC",
  "platform": "win32",
  "workspaceName": "backend-service",
  "ip": "192.168.1.10",
  "port": 8080,
  "authRequired": false,
  "timestamp": 1729432800000
}
```

#### 3. Hasil Eksekusi Perintah (`type: "COMMAND_RESULT"`)
```json
{
  "type": "COMMAND_RESULT",
  "success": true,
  "command": "git status",
  "message": "Perintah dikirim ke terminal: git status",
  "timestamp": 1729432805000
}
```

#### 4. Respons Pong (`type: "PONG"`)
```json
{
  "type": "PONG",
  "timestamp": 1729432810000
}
```

#### 5. Pesan Kesalahan (`type: "ERROR"`)
```json
{
  "type": "ERROR",
  "message": "Akses ditolak: Auth token tidak valid",
  "code": "UNAUTHORIZED",
  "timestamp": 1729432812000
}
```

---

### 5.2 Pesan dari Android ke PC (Client -> Server)

#### 1. Perintah Terminal Shell (`type: "TERMINAL_COMMAND"`)
Mengeksekusi perintah shell pada sistem workstation (PowerShell di Windows atau Bash di Linux/Mac):
```json
{
  "type": "TERMINAL_COMMAND",
  "command": "npm run dev"
}
```
*(Catatan: Mengirimkan teks biasa seperti `"git pull"` tanpa bungkus JSON juga otomatis ditafsirkan sebagai perintah terminal).*

#### 2. Perintah Agen AI Antigravity (`type: "ANTIGRAVITY_COMMAND"`)
Mengirim instruksi ke CLI Google Antigravity Agent (`agy`):
```json
{
  "type": "ANTIGRAVITY_COMMAND",
  "command": "audit performa aplikasi ini"
}
```
Perintah ini otomatis dijalankan di PC sebagai: `agy audit performa aplikasi ini`.

#### 3. Perintah Slash Agent Antigravity
Untuk memicu perintah slash bawaan agent Antigravity:
```json
{
  "type": "ANTIGRAVITY_COMMAND",
  "command": "/goal selesaikan semua unit test yang gagal"
}
```
Pilihan Slash Commands resmi yang didukung:
- `/goal` : Menjalankan tugas otonom hingga selesai.
- `/plan` : Membuat rencana arsitektur dan langkah implementasi.
- `/grill-me` : Wawancara kritis untuk menguji asumsi teknis.
- `/learn` : Menyimpan preferensi dev ke memori jangka panjang agent.
- `/schedule`: Mengatur pengingat atau tugas otomatis berkala.

#### 4. Perintah Workbench IDE (`type: "VSCODE_COMMAND"`)
Menjalankan perintah internal editor:
```json
{
  "type": "VSCODE_COMMAND",
  "command": "workbench.action.files.saveAll"
}
```
Pintasan umum yang bermanfaat:
- `workbench.action.files.saveAll` : Simpan semua file yang sedang terbuka.
- `workbench.action.terminal.toggleTerminal` : Tampilkan / sembunyikan terminal.
- `workbench.action.reloadWindow` : Muat ulang jendela editor.

#### 5. Menampilkan Notifikasi Toast di PC (`type: "NOTIFICATION"`)
```json
{
  "type": "NOTIFICATION",
  "message": "Kopi siap! Dev server sudah aktif dari HP.",
  "level": "info"
}
```
*(Nilai `level` yang didukung: `"info"`, `"warning"`, `"error"`).*

#### 6. Autentikasi PIN / Token (`type: "AUTH"`)
Digunakan jika opsi `antiremote.authToken` diaktifkan di pengaturan PC:
```json
{
  "type": "AUTH",
  "token": "123456"
}
```

---

## 6. IMPLEMENTASI KODE LENGKAP ANDROID (KOTLIN)

Berikut adalah cetak biru implementasi siap pakai untuk developer Android:

### 6.1 Scanner QR Code dengan Auto-Connect

Tambahkan pustaka berikut di `app/build.gradle`:
```groovy
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

Implementasi di Activity/Fragment:
```kotlin
package com.antigravity.remote

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var wsManager: WorkstationWsManager

    // Inisialisasi ActivityResultLauncher untuk kamera scanner
    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            parseQrAndConnect(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wsManager = WorkstationWsManager(
            onStatusChanged = { isConnected, message ->
                runOnUiThread {
                    // Update indikator warna status koneksi di layar HP
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            },
            onTelemetryReceived = { telemetry ->
                runOnUiThread {
                    // Update tampilan gauge CPU & RAM secara real-time
                    updateTelemetryUi(telemetry)
                }
            }
        )

        // Tombol "Scan QR"
        findViewById<android.view.View>(R.id.btnScanQr).setOnClickListener {
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Arahkan kamera ke QR Code di layar PC")
                setCameraId(0)
                setBeepEnabled(true)
                setOrientationLocked(false)
            }
            qrScannerLauncher.launch(options)
        }
    }

    private fun parseQrAndConnect(rawQrContent: String) {
        try {
            var targetIp = ""
            var targetPort = 8080

            val trimmed = rawQrContent.trim()
            if (trimmed.startsWith("{")) {
                // Skenario 1: JSON Payload (Rekomendasi Utama)
                val json = JSONObject(trimmed)
                targetIp = json.getString("ip")
                targetPort = json.optInt("port", 8080)
            } else if (trimmed.startsWith("ws://") || trimmed.startsWith("http://")) {
                // Skenario 2: Format URI URL
                val uri = java.net.URI(trimmed)
                targetIp = uri.host ?: ""
                targetPort = if (uri.port != -1) uri.port else 8080
            } else if (trimmed.contains(":")) {
                // Skenario 3: Format plain IP:PORT
                val parts = trimmed.split(":")
                targetIp = parts[0].trim()
                targetPort = parts[1].trim().toIntOrNull() ?: 8080
            }

            if (targetIp.isNotBlank()) {
                Toast.makeText(this, "QR Valid! Menyambung ke $targetIp:$targetPort...", Toast.LENGTH_SHORT).show()
                wsManager.connect(targetIp, targetPort)
            } else {
                Toast.makeText(this, "Alamat IP tidak ditemukan di QR", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memproses QR Code: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateTelemetryUi(telemetry: TelemetryModel) {
        // Update elemen UI HP Anda di sini
        // Contoh:
        // binding.tvCpuUsage.text = "${telemetry.cpuPercent}%"
        // binding.progressBarCpu.progress = telemetry.cpuPercent
        // binding.tvRamUsage.text = "${telemetry.memoryUsedMb} / ${telemetry.memoryTotalMb} MB"
    }

    override fun onDestroy() {
        super.onDestroy()
        wsManager.disconnect()
    }
}
```

---

### 6.2 Service WebSocket Client (OkHttp)

File: `WorkstationWsManager.kt`
```kotlin
package com.antigravity.remote

import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TelemetryModel(
    val cpuPercent: Int,
    val memoryUsedMb: Int,
    val memoryTotalMb: Int,
    val freeMemMb: Int,
    val workspaceName: String,
    val activeDocument: String,
    val timestamp: Long
)

class WorkstationWsManager(
    private val onStatusChanged: (isConnected: Boolean, message: String) -> Unit,
    private val onTelemetryReceived: (TelemetryModel) -> Unit
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(5, TimeUnit.SECONDS) // Auto-ping heartbeat
        .retryOnConnectionFailure(true)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // 0 untuk koneksi WebSocket persisten
        .build()

    private var currentIp: String = ""
    private var currentPort: Int = 8080

    fun connect(ip: String, port: Int) {
        currentIp = ip
        currentPort = port

        disconnect()

        val request = Request.Builder()
            .url("ws://$ip:$port")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                onStatusChanged(true, "Terhubung ke Workstation ($ip:$port)")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "TELEMETRY" -> {
                            val telemetry = TelemetryModel(
                                cpuPercent = json.optInt("cpuPercent", 0),
                                memoryUsedMb = json.optInt("memoryUsedMb", 0),
                                memoryTotalMb = json.optInt("memoryTotalMb", 0),
                                freeMemMb = json.optInt("freeMemMb", 0),
                                workspaceName = json.optString("workspaceName", "-"),
                                activeDocument = json.optString("activeDocument", "-"),
                                timestamp = json.optLong("timestamp", System.currentTimeMillis())
                            )
                            onTelemetryReceived(telemetry)
                        }
                        "COMMAND_RESULT" -> {
                            val cmd = json.optString("command")
                            val msg = json.optString("message")
                            // Notifikasi hasil eksekusi remote command
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
                onStatusChanged(false, "Koneksi ditutup: $reason")
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                onStatusChanged(false, "Gagal konek: ${t.message}")
            }
        })
    }

    // Fungsi Pengiriman Perintah dari HP ke PC:

    fun sendTerminalCommand(command: String) {
        val payload = JSONObject().apply {
            put("type", "TERMINAL_COMMAND")
            put("command", command)
        }
        webSocket?.send(payload.toString())
    }

    fun sendAgentPrompt(prompt: String) {
        val payload = JSONObject().apply {
            put("type", "ANTIGRAVITY_COMMAND")
            put("command", prompt)
        }
        webSocket?.send(payload.toString())
    }

    fun sendVscodeAction(action: String) {
        val payload = JSONObject().apply {
            put("type", "VSCODE_COMMAND")
            put("command", action)
        }
        webSocket?.send(payload.toString())
    }

    fun sendToastNotification(message: String, level: String = "info") {
        val payload = JSONObject().apply {
            put("type", "NOTIFICATION")
            put("message", message)
            put("level", level)
        }
        webSocket?.send(payload.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
    }
}
```

---

## 7. PANDUAN OPERASIONAL & PEMECAHAN MASALAH (TROUBLESHOOTING)

### 1. Masalah: HP Menampilkan "Gagal konek: Failed to connect to 192.168.1.X:8080"
- **Penyebab Utama:** Windows Defender Firewall memblokir port masuk `8080`.
- **Solusi Windows:**
  Buka PowerShell (Run as Administrator) dan jalankan perintah satu baris ini untuk mengizinkan port bridge:
  ```powershell
  New-NetFirewallRule -DisplayName "Antigravity Bridge (Port 8080)" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
  ```
- **Penyebab Kedua:** HP dan PC berada di subnet yang berbeda (misal: PC tersambung via LAN kabel kantor dengan isolasi AP, sedangkan HP di Wi-Fi Guest).
- **Solusi:** Pastikan keduanya berada di jaringan Wi-Fi rumah atau hotspot smartphone yang sama.

### 2. Masalah: Layar Scanner QR Tidak Mau Terbuka
- **Solusi:** Pastikan izin runtime kamera telah disetujui di HP (`Manifest.permission.CAMERA`). Pada Android 6.0+, izin kamera wajib diminta secara dinamis saat runtime sebelum kamera diaktifkan.

### 3. Masalah: Alamat IP PC Berubah Saat Wi-Fi Reconnect
- **Solusi:** 
  Cukup buka kembali Dashboard di PC (`Ctrl+Shift+P` -> `Antigravity Remote: Buka Dashboard & QR Code`). Ekstensi secara otomatis mendeteksi perubahan IP antarmuka jaringan dan memperbarui QR Code secara instan. HP Anda cukup melakukan re-scan QR sekali klik.

---

### Ringkasan File Terkait dalam Repositori Ini:
- [antigravity-bridge.js](file:///d:/PROJECT/GAWEAN/PROJECT/EXT/antiremote/antigravity-bridge.js) : Server Bridge standalone CLI dengan terminal QR Code.
- [src/bridgeServer.ts](file:///d:/PROJECT/GAWEAN/PROJECT/EXT/antiremote/src/bridgeServer.ts) : Mesin server HTTP + WebSocket ekstensi.
- [src/dashboardPanel.ts](file:///d:/PROJECT/GAWEAN/PROJECT/EXT/antiremote/src/dashboardPanel.ts) : Webview Dashboard interaktif dengan QR Code dual-mode (JSON & URL).
- [antigravity-remote-bridge-1.0.0.vsix](file:///d:/PROJECT/GAWEAN/PROJECT/EXT/antiremote/antigravity-remote-bridge-1.0.0.vsix) : Paket installer ekstensi siap pasang di VS Code / Antigravity IDE.
