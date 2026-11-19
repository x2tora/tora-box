package com.x2tora.torabox.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.x2tora.torabox.MainActivity
import com.x2tora.torabox.R
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.model.AppSettings
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase

object LiveUpdateNotificationManager {
    private const val CHANNEL_ID = "torabox_live_updates"
    private const val PREFS_NAME = "torabox_live_update_state"
    private const val KEY_ACTIVE_IDS = "active_live_ids"
    private const val NOTIF_ID_OFFSET = 50_000

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= 36

    fun canPostPromoted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 36) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        return runCatching { manager.canPostPromotedNotifications() }.getOrDefault(false)
    }

    fun openPromotionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < 36) return
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun syncLiveUpdates(
        context: Context,
        shipments: List<ShipmentRecord>,
        settings: AppSettings
    ) {
        if (Build.VERSION.SDK_INT < 36) return
        if (!NotificationHelper.hasNotificationPermission(context)) return

        val appContext = context.applicationContext
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannel(manager)

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prevIds = prefs.getStringSet(KEY_ACTIVE_IDS, emptySet()).orEmpty()
        val plan = LiveUpdatePolicy.computePlan(shipments, settings, prevIds)

        plan.idsToCancel.forEach { id ->
            manager.cancel(notificationIdFor(id))
        }

        plan.completedToDemote.forEach { record ->
            val notification = buildLiveUpdateNotification(appContext, record, isDelivered = true)
            runCatching { manager.notify(notificationIdFor(record.id), notification) }
        }

        plan.activeTargets.forEach { record ->
            val notification = buildLiveUpdateNotification(appContext, record, isDelivered = false)
            runCatching { manager.notify(notificationIdFor(record.id), notification) }
        }

        val nextIds = plan.activeTargets.map { it.id }.toSet()
        prefs.edit().putStringSet(KEY_ACTIVE_IDS, nextIds).apply()
    }

    private fun notificationIdFor(shipmentId: String): Int =
        NOTIF_ID_OFFSET + (shipmentId.hashCode() and 0x0FFF_FFFF)

    @RequiresApi(36)
    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "실시간 배송 진행 (Live Updates)",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Android 16 상태바 칩 및 잠금화면 실시간 배송 진행바를 표시합니다"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    @RequiresApi(36)
    private fun buildLiveUpdateNotification(
        context: Context,
        record: ShipmentRecord,
        isDelivered: Boolean
    ): Notification {
        val carrier = KoreanCarrierCatalog.findById(record.carrierId)
        val carrierShort = carrier?.shortName ?: record.carrierId
        val title = record.displayTitle(carrierShort)
        val progress = if (isDelivered) 600 else LiveUpdatePolicy.progressForPhase(record.currentPhase)
        val completedSteps = (progress / 100).coerceIn(1, 6)

        val accentColorInt = when {
            isDelivered -> 0xFF16A34A.toInt()
            record.currentPhase == StandardDeliveryPhase.EXCEPTION -> 0xFFDC2626.toInt()
            record.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY -> 0xFFF97316.toInt()
            else -> (record.colorHex and 0xFFFFFFFFL).toInt()
        }
        val inactiveColorInt = 0x448E99A8

        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(true)
            .setProgress(progress)
            .setProgressTrackerIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))

        for (step in 1..6) {
            val segColor = if (step <= completedSteps) accentColorInt else inactiveColorInt
            progressStyle.addProgressSegment(
                Notification.ProgressStyle.Segment(100).setColor(segColor)
            )
        }
        progressStyle.addProgressPoint(
            Notification.ProgressStyle.Point(progress).setColor(accentColorInt)
        )

        val latest = record.events.firstOrNull()
        val contentText = buildString {
            append(carrierShort)
            append(" · ")
            append(record.currentStatusLabel.ifBlank { record.currentPhase.label })
            if (latest?.location?.isNotBlank() == true) {
                append(" (").append(latest.location).append(")")
            }
            if (!isDelivered && record.estimatedDeliveryLabel.isNotBlank()) {
                append(" · ").append(record.estimatedDeliveryLabel)
            }
        }

        val detailIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationHelper.EXTRA_SHIPMENT_ID, record.id)
        }
        val detailPendingIntent = PendingIntent.getActivity(
            context,
            record.id.hashCode(),
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(progressStyle)
            .setOngoing(!isDelivered)
            .setAutoCancel(isDelivered)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(!isDelivered)
            .setShortCriticalText(
                if (isDelivered) "배송완료" else LiveUpdatePolicy.shortChipText(record.currentPhase)
            )
            .setContentIntent(detailPendingIntent)

        if (!isDelivered) {
            val refreshIntent = Intent(context, LiveUpdateActionReceiver::class.java).apply {
                action = LiveUpdateActionReceiver.ACTION_REFRESH_NOW
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationIdFor(record.id),
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.mipmap.ic_launcher),
                    "지금 새로고침",
                    refreshPendingIntent
                ).build()
            )
        }

        return builder.build()
    }
}
