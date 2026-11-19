package com.x2tora.torabox.domain

import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.TrackingResult

/**
 * Shared domain helpers for merging tracking updates and maintaining archived shipments.
 */
object ShipmentMaintenance {
    private const val DAY_MS = 86_400_000L

    fun applyTrackingResult(
        base: ShipmentRecord,
        tr: TrackingResult,
        nowEpochMs: Long = System.currentTimeMillis(),
        historicalSameCarrierHours: Double? = null
    ): ShipmentRecord {
        val updated = base.copy(
            carrierId = tr.carrierId,
            itemName = tr.itemName.ifBlank { base.itemName },
            itemQuantity = tr.itemQuantity.ifBlank { base.itemQuantity },
            senderName = tr.senderName.ifBlank { base.senderName },
            senderPhone = tr.senderPhone.ifBlank { base.senderPhone },
            senderAddress = tr.senderAddress.ifBlank { base.senderAddress },
            recipientName = tr.recipientName.ifBlank { base.recipientName },
            recipientPhone = tr.recipientPhone.ifBlank { base.recipientPhone },
            recipientAddress = tr.recipientAddress.ifBlank { base.recipientAddress },
            accepterName = tr.accepterName.ifBlank { base.accepterName },
            serviceType = tr.serviceType.ifBlank { base.serviceType },
            originWaybill = tr.originWaybill.ifBlank { base.originWaybill },
            returnWaybill = tr.returnWaybill.ifBlank { base.returnWaybill },
            currentStatusLabel = tr.currentStatusLabel.ifBlank { base.currentStatusLabel },
            currentPhase = tr.currentPhase,
            currentCarrierStageIndex = tr.currentCarrierStageIndex,
            events = if (tr.events.isNotEmpty()) tr.events else base.events,
            lastUpdatedEpochMs = nowEpochMs,
            lastSyncSource = tr.syncSource,
            lastLatencyMs = if (tr.latencyMs > 0L) tr.latencyMs else base.lastLatencyMs
        )
        return updated.copy(
            anomalyType = DeliveryIntelligence.detectAnomaly(updated, nowEpochMs),
            estimatedDeliveryLabel = DeliveryIntelligence.estimateDeliveryLabel(updated, historicalSameCarrierHours)
        )
    }

    /**
     * Automatically archives DELIVERED shipments whose reference timestamp is at least [days] old.
     * Pinned shipments are kept active unless manually archived.
     */
    fun applyAutoArchive(
        shipments: List<ShipmentRecord>,
        days: Int,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Pair<List<ShipmentRecord>, Int> {
        if (days <= 0) return shipments to 0
        val cutoffMs = days.toLong() * DAY_MS
        var archivedCount = 0
        val updated = shipments.map { record ->
            val refMs = maxOf(record.lastUpdatedEpochMs, record.createdAtEpochMs)
            val isExpiredDelivered = !record.isArchived &&
                !record.isPinned &&
                record.currentPhase == StandardDeliveryPhase.DELIVERED &&
                refMs > 0L &&
                (nowEpochMs - refMs) >= cutoffMs
            if (isExpiredDelivered) {
                archivedCount++
                record.copy(isArchived = true)
            } else {
                record
            }
        }
        return updated to archivedCount
    }
}
