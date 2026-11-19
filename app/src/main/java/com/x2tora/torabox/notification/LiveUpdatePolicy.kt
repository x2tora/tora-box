package com.x2tora.torabox.notification

import com.x2tora.torabox.model.AppSettings
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase

data class LiveUpdatePlan(
    val activeTargets: List<ShipmentRecord>,
    val completedToDemote: List<ShipmentRecord>,
    val idsToCancel: Set<String>
)

object LiveUpdatePolicy {
    const val MAX_CONCURRENT_LIVE_UPDATES = 3

    fun computePlan(
        shipments: List<ShipmentRecord>,
        settings: AppSettings,
        previouslyActiveIds: Set<String>
    ): LiveUpdatePlan {
        if (!settings.liveUpdatesEnabled) {
            return LiveUpdatePlan(
                activeTargets = emptyList(),
                completedToDemote = emptyList(),
                idsToCancel = previouslyActiveIds
            )
        }

        val activeTargets = shipments
            .filter { record ->
                !record.isArchived &&
                    record.currentPhase != StandardDeliveryPhase.DELIVERED &&
                    (record.isLiveUpdatePinned || record.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY)
            }
            .sortedWith(
                compareByDescending<ShipmentRecord> { it.isLiveUpdatePinned }
                    .thenByDescending { it.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY }
                    .thenByDescending { it.isPinned }
                    .thenByDescending { maxOf(it.lastUpdatedEpochMs, it.createdAtEpochMs) }
            )
            .take(MAX_CONCURRENT_LIVE_UPDATES)

        val activeIdSet = activeTargets.map { it.id }.toSet()

        val completedToDemote = shipments.filter { record ->
            record.id in previouslyActiveIds &&
                record.id !in activeIdSet &&
                record.currentPhase == StandardDeliveryPhase.DELIVERED
        }
        val completedIdSet = completedToDemote.map { it.id }.toSet()

        val idsToCancel = previouslyActiveIds - activeIdSet - completedIdSet

        return LiveUpdatePlan(
            activeTargets = activeTargets,
            completedToDemote = completedToDemote,
            idsToCancel = idsToCancel
        )
    }

    fun progressForPhase(phase: StandardDeliveryPhase): Int = when (phase) {
        StandardDeliveryPhase.PREPARING -> 100
        StandardDeliveryPhase.PICKED_UP -> 200
        StandardDeliveryPhase.IN_TRANSIT -> 300
        StandardDeliveryPhase.ARRIVED_AT_BRANCH -> 400
        StandardDeliveryPhase.OUT_FOR_DELIVERY -> 500
        StandardDeliveryPhase.DELIVERED -> 600
        StandardDeliveryPhase.EXCEPTION -> 400
    }

    fun shortChipText(phase: StandardDeliveryPhase): String = when (phase) {
        StandardDeliveryPhase.PREPARING -> "접수"
        StandardDeliveryPhase.PICKED_UP -> "집하"
        StandardDeliveryPhase.IN_TRANSIT -> "이동중"
        StandardDeliveryPhase.ARRIVED_AT_BRANCH -> "지점도착"
        StandardDeliveryPhase.OUT_FOR_DELIVERY -> "배송출발"
        StandardDeliveryPhase.DELIVERED -> "배송완료"
        StandardDeliveryPhase.EXCEPTION -> "확인필요"
    }
}
