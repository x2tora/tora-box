package com.x2tora.torabox.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.x2tora.torabox.MainActivity
import com.x2tora.torabox.R
import com.x2tora.torabox.model.StandardDeliveryPhase

data class PhaseChangeNotice(
    val shipmentId: String,
    val title: String,
    val carrierShortName: String,
    val previousPhase: StandardDeliveryPhase,
    val newPhase: StandardDeliveryPhase,
    val statusLabel: String,
    val location: String
)

object NotificationHelper {
    const val CHANNEL_ID = "torabox_tracking_updates"
    const val EXTRA_SHIPMENT_ID = "extra_shipment_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "배송 상태 알림",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "택배 배송 단계가 변경될 때 알림을 보냅니다"
        }
        manager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notifyPhaseChanges(context: Context, changes: List<PhaseChangeNotice>) {
        if (changes.isEmpty() || !hasNotificationPermission(context)) return
        ensureChannel(context)
        val manager = NotificationManagerCompat.from(context)

        changes.take(5).forEach { notice ->
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_SHIPMENT_ID, notice.shipmentId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notice.shipmentId.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val contentText = buildString {
                append("${notice.carrierShortName} · ${notice.statusLabel.ifBlank { notice.newPhase.label }}")
                if (notice.location.isNotBlank()) {
                    append(" (${notice.location})")
                }
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("${notice.title} · ${notice.newPhase.label}")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            runCatching {
                manager.notify(notice.shipmentId.hashCode(), notification)
            }
        }
    }
}
