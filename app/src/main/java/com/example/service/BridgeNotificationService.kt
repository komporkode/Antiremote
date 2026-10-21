package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
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
        const val CHANNEL_ID_PERSISTENT = "antiremote_persistent_channel"
        const val CHANNEL_ID_APPROVAL = "antiremote_approval_channel"

        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_APPROVAL = 1002

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, BridgeNotificationService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BridgeNotificationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification("Terhubung ke Antigravity IDE"))
                observeClientEvents()
            }
            else -> {
                startForeground(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification("Antigravity Remote Bridge Aktif"))
                observeClientEvents()
            }
        }
        return START_STICKY
    }

    private fun observeClientEvents() {
        // Observe Connection Status - auto stop if disconnected
        serviceScope.launch {
            client.connectionConfig.collectLatest { config ->
                if (config.status == ConnectionStatus.CONNECTED) {
                    val notif = buildForegroundNotification("Terkoneksi ke ${config.hostname.ifEmpty { "Workstation" }} (${config.ipAddress}:${config.port})")
                    notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notif)
                } else if (config.status == ConnectionStatus.DISCONNECTED || config.status == ConnectionStatus.ERROR) {
                    stopForeground(true)
                    stopSelf()
                }
            }
        }

        // Observe Pending Approval Requests
        serviceScope.launch {
            client.pendingApproval.collectLatest { approval ->
                if (approval != null) {
                    showApprovalNotification(approval)
                } else {
                    notificationManager.cancel(NOTIFICATION_ID_APPROVAL)
                }
            }
        }
    }

    private fun showApprovalNotification(approval: AgentApprovalRequest) {
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
            .addAction(android.R.drawable.checkbox_on_background, "Izinkan (Allow)", allowPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Tolak (Deny)", denyPendingIntent)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        notificationManager.notify(NOTIFICATION_ID_APPROVAL, notif)
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_PERSISTENT)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
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
