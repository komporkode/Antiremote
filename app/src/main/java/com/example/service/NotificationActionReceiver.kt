package com.example.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.network.LanBridgeClient

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_APPROVE = "com.example.service.ACTION_APPROVE"
        const val ACTION_DENY = "com.example.service.ACTION_DENY"
        const val EXTRA_APPROVAL_ID = "extra_approval_id"
        const val EXTRA_CHOICE = "extra_choice"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val approvalId = intent.getStringExtra(EXTRA_APPROVAL_ID) ?: return
        val choice = intent.getStringExtra(EXTRA_CHOICE) ?: if (action == ACTION_APPROVE) "Allow" else "Reject"

        val isApproved = (action == ACTION_APPROVE)
        LanBridgeClient.getInstance().sendApprovalResponse(approvalId, isApproved, choice)

        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifManager.cancel(BridgeNotificationService.NOTIFICATION_ID_APPROVAL)

        val message = when {
            choice.equals("Allow", ignoreCase = true) || choice.equals("Izinkan", ignoreCase = true) -> "✓ Izin eksekusi disetujui"
            choice.equals("Reject", ignoreCase = true) || choice.equals("Tolak", ignoreCase = true) -> "✗ Izin eksekusi ditolak"
            choice.length > 30 -> "✓ Terpilih: ${choice.take(27)}..."
            else -> "✓ Terpilih: $choice"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
