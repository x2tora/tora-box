package com.x2tora.torabox.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.x2tora.torabox.data.ToraBoxRepository
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.domain.ParcelTrackingEngine
import com.x2tora.torabox.domain.ShipmentMaintenance
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.notification.LiveUpdateNotificationManager
import com.x2tora.torabox.notification.NotificationHelper
import com.x2tora.torabox.notification.PhaseChangeNotice
import com.x2tora.torabox.widget.ToraBoxWidgetUpdater
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.TimeUnit

class ShipmentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        val repo = ToraBoxRepository(applicationContext)
        val initialState = repo.load()
        val settings = initialState.settings

        val activeTargets = initialState.shipments.filter {
            !it.isArchived && it.currentPhase != StandardDeliveryPhase.DELIVERED
        }

        if (activeTargets.isEmpty()) {
            val (archivedList, archivedCount) = ShipmentMaintenance.applyAutoArchive(
                initialState.shipments,
                settings.autoArchiveDeliveredDays
            )
            if (archivedCount > 0) {
                repo.save(initialState.copy(shipments = archivedList))
                ToraBoxWidgetUpdater.updateAll(applicationContext)
            }
            return@coroutineScope Result.success()
        }

        val trackingMap = activeTargets.map { record ->
            async {
                val carrier = KoreanCarrierCatalog.findById(record.carrierId)
                    ?: return@async record.id to null
                val tracking = ParcelTrackingEngine.track(
                    carrier = carrier,
                    rawWaybill = record.waybillNumber,
                    sweetTrackerApiKey = settings.sweetTrackerApiKey,
                    customTrackerEndpoint = settings.customTrackerEndpoint,
                    appContext = applicationContext,
                    personalCustomsCode = settings.personalCustomsCode
                ).getOrNull()
                record.id to tracking
            }
        }.awaitAll().toMap()

        val phaseChanges = mutableListOf<PhaseChangeNotice>()
        val mergedShipments = initialState.shipments.map { current ->
            val result = trackingMap[current.id] ?: return@map current
            val updated = ShipmentMaintenance.applyTrackingResult(current, result)
            if (updated.currentPhase != current.currentPhase) {
                val carrierShort = KoreanCarrierCatalog.findById(updated.carrierId)?.shortName ?: updated.carrierId
                phaseChanges += PhaseChangeNotice(
                    shipmentId = updated.id,
                    title = updated.displayTitle(carrierShort),
                    carrierShortName = carrierShort,
                    previousPhase = current.currentPhase,
                    newPhase = updated.currentPhase,
                    statusLabel = updated.currentStatusLabel,
                    location = updated.events.firstOrNull()?.location.orEmpty()
                )
            }
            updated
        }

        val (finalShipments, _) = ShipmentMaintenance.applyAutoArchive(
            mergedShipments,
            settings.autoArchiveDeliveredDays
        )
        repo.save(initialState.copy(shipments = finalShipments))
        ToraBoxWidgetUpdater.updateAll(applicationContext)
        LiveUpdateNotificationManager.syncLiveUpdates(applicationContext, finalShipments, settings)
        if (settings.backgroundSyncEnabled && phaseChanges.isNotEmpty()) {
            NotificationHelper.notifyPhaseChanges(applicationContext, phaseChanges, settings)
        }
        if (settings.backgroundSyncEnabled && settings.notifyAnomalies) {
            finalShipments.forEach { record ->
                val prev = initialState.shipments.find { it.id == record.id }
                val anomaly = record.anomalyType
                if (anomaly != null && prev?.anomalyType != anomaly && !record.isArchived) {
                    val carrierShort = KoreanCarrierCatalog.findById(record.carrierId)?.shortName ?: record.carrierId
                    NotificationHelper.notifyAnomaly(
                        context = applicationContext,
                        shipmentId = record.id,
                        title = record.displayTitle(carrierShort),
                        anomaly = anomaly,
                        settings = settings
                    )
                }
            }
        }
        Result.success()
    }
}

object ShipmentSyncScheduler {
    private const val PERIODIC_WORK_NAME = "torabox_periodic_shipment_sync"
    private const val IMMEDIATE_WORK_NAME = "torabox_immediate_shipment_sync"

    fun syncSchedule(context: Context, enabled: Boolean, intervalHours: Int) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }
        val clampedHours = intervalHours.coerceIn(1, 24).toLong()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<ShipmentSyncWorker>(clampedHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun triggerImmediateSync(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<ShipmentSyncWorker>()
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
