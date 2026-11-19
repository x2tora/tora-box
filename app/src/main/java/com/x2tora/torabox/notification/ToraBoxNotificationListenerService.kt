package com.x2tora.torabox.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.x2tora.torabox.data.ToraBoxRepository
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.domain.NotificationMessageParser
import com.x2tora.torabox.domain.WaybillDetector
import com.x2tora.torabox.model.PendingCaptureItem
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.widget.ToraBoxWidgetUpdater
import com.x2tora.torabox.worker.ShipmentSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Opt-in NotificationListenerService that passively extracts parcel waybills
 * from KakaoTalk, SMS, and shopping app notifications on-device.
 */
class ToraBoxNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recentHashes = ConcurrentHashMap<Int, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (text.isBlank()) return

        val keyHash = "$pkg|$title|$text".hashCode()
        val now = System.currentTimeMillis()
        val prevSeen = recentHashes[keyHash]
        if (prevSeen != null && (now - prevSeen) < 300_000L) return
        recentHashes[keyHash] = now

        val parsed = NotificationMessageParser.parse(pkg, title, text) ?: return

        scope.launch {
            val repo = ToraBoxRepository(applicationContext)
            val state = repo.load()
            val settings = state.settings
            if (!settings.autoCaptureNotificationsEnabled) return@launch
            if (pkg !in settings.autoCaptureAllowedPackages) return@launch

            val cleanWaybill = WaybillDetector.normalizeWaybill(parsed.notice.waybillNumber)
            val alreadyExists = state.shipments.any { it.waybillNumber == cleanWaybill }
            if (alreadyExists) {
                ShipmentSyncScheduler.triggerImmediateSync(applicationContext)
                return@launch
            }

            if (settings.autoCaptureRegisterImmediately) {
                val guessedCarrierId = parsed.notice.carrierHintId
                    ?: WaybillDetector.recommendCarriers(cleanWaybill).firstOrNull()?.carrier?.id
                    ?: "kr.cjlogistics"
                val carrier = KoreanCarrierCatalog.findById(guessedCarrierId)
                val newRecord = ShipmentRecord(
                    id = UUID.randomUUID().toString(),
                    waybillNumber = cleanWaybill,
                    carrierId = guessedCarrierId,
                    itemName = parsed.notice.itemNameHint,
                    senderName = parsed.notice.senderNameHint,
                    groupTag = if (carrier?.category?.name == "INTERNATIONAL") "직구" else "자동수집",
                    iconKey = carrier?.iconKey ?: "box",
                    colorHex = carrier?.accentColorHex ?: 0xFF3482FF,
                    createdAtEpochMs = now,
                    lastUpdatedEpochMs = now,
                    lastSyncSource = "${parsed.sourceAppLabel} 자동수집"
                )
                repo.save(
                    state.copy(
                        shipments = listOf(newRecord) + state.shipments,
                        pendingCaptures = state.pendingCaptures.filterNot { it.waybillNumber == cleanWaybill }
                    )
                )
                ToraBoxWidgetUpdater.updateAll(applicationContext)
                ShipmentSyncScheduler.triggerImmediateSync(applicationContext)
            } else {
                if (state.pendingCaptures.any { it.waybillNumber == cleanWaybill }) return@launch
                val pending = PendingCaptureItem(
                    id = UUID.randomUUID().toString(),
                    waybillNumber = cleanWaybill,
                    carrierHintId = parsed.notice.carrierHintId,
                    itemNameHint = parsed.notice.itemNameHint,
                    senderNameHint = parsed.notice.senderNameHint,
                    sourceAppLabel = parsed.sourceAppLabel,
                    capturedAtEpochMs = now
                )
                repo.save(state.copy(pendingCaptures = listOf(pending) + state.pendingCaptures.take(19)))
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ).orEmpty()
            val flatName = ComponentName(context, ToraBoxNotificationListenerService::class.java).flattenToString()
            return enabledListeners.split(':').any { it.equals(flatName, ignoreCase = true) }
        }

        fun openSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
