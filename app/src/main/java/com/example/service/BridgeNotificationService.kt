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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class BridgeNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private val client: LanBridgeClient by lazy { LanBridgeClient.getInstance() }

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
                    stopForegroundSafely()
                    stopSelf()
                    return START_NOT_STICKY
                }
                ACTION_START -> {
                    startForegroundSafely("Terhubung ke Antigravity IDE")
                    observeClientEvents()
                }
                else -> {
                    startForegroundSafely("Antigravity Remote Bridge Aktif")
                    observeClientEvents()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
        }
        return START_STICKY
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
        // Observe Connection Status - auto stop if disconnected
        serviceScope.launch {
            client.connectionConfig.collectLatest { config ->
                try {
                    if (config.status == ConnectionStatus.CONNECTED) {
                        val notif = buildForegroundNotification("Terkoneksi ke ${config.hostname.ifEmpty { "Workstation" }} (${config.ipAddress}:${config.port})")
                        notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notif)
                    } else if (config.status == ConnectionStatus.DISCONNECTED || config.status == ConnectionStatus.ERROR) {
                        stopForegroundSafely()
                        stopSelf()
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
                    if (approval != null) {
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

            val notif = NotificationCompat.Builder(this, CHANNEL_ID_APPROVAL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ Izin Diperlukan: ${approval.title}")
                .setContentText(approval.detail.ifEmpty { "AI Agent meminta persetujuan eksekusi perintah." })
                .setStyle(NotificationCompat.BigTextStyle().bigText(approval.detail))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .addAction(0, "Izinkan (Allow)", allowPendingIntent)
                .addAction(0, "Tolak (Deny)", denyPendingIntent)
                .setVibrate(longArrayOf(0, 250, 150, 250))
                .build()

            notificationManager.notify(NOTIFICATION_ID_APPROVAL, notif)
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
        serviceScope.cancel()
        super.onDestroy()
    }
}
