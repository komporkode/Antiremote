package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.AgentApprovalRequest
import com.example.data.model.ConnectionStatus
import com.example.data.network.LanBridgeClient
import android.net.wifi.WifiManager
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class BridgeNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private val client: LanBridgeClient by lazy { LanBridgeClient.getInstance() }
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        private const val TAG = "BridgeNotifService"
        const val CHANNEL_ID_PERSISTENT = "antiremote_persistent_channel"
        const val CHANNEL_ID_APPROVAL = "antiremote_approval_channel"

        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_APPROVAL = 1002

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        fun start(context: Context) {
            try {
                val intent = Intent(context, BridgeNotificationService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service safely", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, BridgeNotificationService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop service safely", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_STOP -> {
                    releaseLocks()
                    stopForegroundSafely()
                    stopSelf()
                    return START_NOT_STICKY
                }
                ACTION_START -> {
                    acquireLocks()
                    startForegroundSafely("Terhubung ke Antigravity IDE")
                    observeClientEvents()
                }
                else -> {
                    acquireLocks()
                    startForegroundSafely("Antigravity Remote Bridge Aktif")
                    observeClientEvents()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
        }
        return START_STICKY
    }

    private fun acquireLocks() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Antiremote:BridgeWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(24 * 60 * 60 * 1000L) // Safeguard 24 jam
                Log.d(TAG, "WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WakeLock", e)
        }

        try {
            if (wifiLock == null) {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                wifiLock = wifiManager?.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "Antiremote:BridgeWifiLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
                Log.d(TAG, "WifiLock acquired")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire WifiLock", e)
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release WakeLock", e)
        }
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
                Log.d(TAG, "WifiLock released")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release WifiLock", e)
        }
    }

    private fun startForegroundSafely(contentText: String) {
        val notification = buildForegroundNotification(contentText)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID_FOREGROUND,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID_FOREGROUND, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed, falling back to standard notification", e)
            try {
                notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notification)
            } catch (inner: Exception) {
                Log.e(TAG, "notify fallback failed", inner)
            }
        }
    }

    private fun stopForegroundSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopForeground failed", e)
        }
    }

    private fun observeClientEvents() {
        // Observe Connection Status - tahan background service selama auto-reconnect aktif
        serviceScope.launch {
            client.connectionConfig.collectLatest { config ->
                try {
                    when (config.status) {
                        ConnectionStatus.CONNECTED -> {
                            acquireLocks()
                            val notif = buildForegroundNotification("Terkoneksi ke ${config.hostname.ifEmpty { "Workstation" }} (${config.ipAddress}:${config.port})")
                            notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notif)
                        }
                        ConnectionStatus.CONNECTING -> {
                            acquireLocks()
                            val notif = buildForegroundNotification("Menghubungkan kembali ke ${config.ipAddress.ifEmpty { "Workstation" }}...")
                            notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notif)
                        }
                        ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> {
                            if (client.isExplicitDisconnectState) {
                                releaseLocks()
                                stopForegroundSafely()
                                stopSelf()
                            } else {
                                // Background temporary drop or auto-reconnecting -> jangan bunuh service
                                val notif = buildForegroundNotification("Koneksi terputus. Menunggu reconnect otomatis...")
                                notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notif)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in connectionConfig observer", e)
                }
            }
        }

        // Observe Pending Approval Requests
        serviceScope.launch {
            client.pendingApproval.collectLatest { approval ->
                try {
                    if (approval != null && approval.status == com.example.data.model.ApprovalStatus.PENDING && !client.resolvedApprovalIds.contains(approval.id)) {
                        showApprovalNotification(approval)
                    } else {
                        notificationManager.cancel(NOTIFICATION_ID_APPROVAL)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in pendingApproval observer", e)
                }
            }
        }
    }

    private fun showApprovalNotification(approval: AgentApprovalRequest) {
        try {
            val contentIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val isQuestion = approval.actionType.equals("ASK_QUESTION", ignoreCase = true)
            val notifTitle = if (isQuestion) "❓ Pertanyaan AI: ${approval.title}" else "⚠️ Izin Diperlukan: ${approval.title}"
            val notifText = approval.detail.ifEmpty {
                if (isQuestion) "AI Agent mengajukan pertanyaan untuk Anda." else "AI Agent meminta persetujuan eksekusi perintah."
            }

            val optionsSummary = if (isQuestion && approval.options.isNotEmpty()) {
                approval.options.mapIndexed { idx, opt -> "${idx + 1}. $opt" }.joinToString("\n")
            } else ""
            val fullBigText = if (optionsSummary.isNotEmpty()) "$notifText\n\n$optionsSummary" else notifText

            val notifBuilder = NotificationCompat.Builder(this, CHANNEL_ID_APPROVAL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(notifTitle)
                .setContentText(notifText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(fullBigText))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setVibrate(longArrayOf(0, 300, 150, 300, 150, 400))

            if (isQuestion && approval.options.isNotEmpty()) {
                // Tambahkan hingga 3 aksi opsi cepat langsung pada notifikasi
                approval.options.take(3).forEachIndexed { index, option ->
                    val optIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                        action = NotificationActionReceiver.ACTION_APPROVE
                        putExtra(NotificationActionReceiver.EXTRA_APPROVAL_ID, approval.id)
                        putExtra(NotificationActionReceiver.EXTRA_CHOICE, option)
                    }
                    val optPendingIntent = PendingIntent.getBroadcast(
                        this,
                        approval.id.hashCode() + index + 10,
                        optIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val shortLabel = if (option.length > 25) {
                        "${index + 1}. ${option.take(22)}..."
                    } else {
                        "${index + 1}. $option"
                    }
                    notifBuilder.addAction(0, shortLabel, optPendingIntent)
                }
            } else {
                val allowIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_APPROVE
                    putExtra(NotificationActionReceiver.EXTRA_APPROVAL_ID, approval.id)
                    putExtra(NotificationActionReceiver.EXTRA_CHOICE, "Allow")
                }
                val allowPendingIntent = PendingIntent.getBroadcast(
                    this,
                    approval.id.hashCode(),
                    allowIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val denyIntent = Intent(this, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_DENY
                    putExtra(NotificationActionReceiver.EXTRA_APPROVAL_ID, approval.id)
                    putExtra(NotificationActionReceiver.EXTRA_CHOICE, "Reject")
                }
                val denyPendingIntent = PendingIntent.getBroadcast(
                    this,
                    approval.id.hashCode() + 1,
                    denyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                notifBuilder.addAction(0, "Izinkan (Allow)", allowPendingIntent)
                notifBuilder.addAction(0, "Tolak (Deny)", denyPendingIntent)
            }

            notificationManager.notify(NOTIFICATION_ID_APPROVAL, notifBuilder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show approval notification", e)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_PERSISTENT)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Antigravity Remote Bridge")
            .setContentText(contentText)
            .setContentIntent(launchIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val persistentChannel = NotificationChannel(
                CHANNEL_ID_PERSISTENT,
                "Status Koneksi Bridge",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menjaga koneksi tetap aktif di latar belakang saat terhubung ke IDE"
                setShowBadge(false)
            }

            val approvalChannel = NotificationChannel(
                CHANNEL_ID_APPROVAL,
                "Persetujuan Aksi Agent (High Priority)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi getar dan suara saat AI Agent meminta izin eksekusi tool"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(persistentChannel)
            notificationManager.createNotificationChannel(approvalChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseLocks()
        serviceScope.cancel()
        super.onDestroy()
    }
}
