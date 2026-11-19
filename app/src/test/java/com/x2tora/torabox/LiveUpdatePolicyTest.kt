package com.x2tora.torabox

import com.x2tora.torabox.model.AppSettings
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.notification.LiveUpdatePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveUpdatePolicyTest {

    @Test
    fun `selects out-for-delivery and manually pinned shipments up to cap of 3`() {
        val records = listOf(
            ShipmentRecord(id = "1", waybillNumber = "111", carrierId = "kr.cjlogistics", currentPhase = StandardDeliveryPhase.IN_TRANSIT, isLiveUpdatePinned = true),
            ShipmentRecord(id = "2", waybillNumber = "222", carrierId = "kr.cjlogistics", currentPhase = StandardDeliveryPhase.OUT_FOR_DELIVERY, lastUpdatedEpochMs = 400L),
            ShipmentRecord(id = "3", waybillNumber = "333", carrierId = "kr.cjlogistics", currentPhase = StandardDeliveryPhase.OUT_FOR_DELIVERY, lastUpdatedEpochMs = 300L),
            ShipmentRecord(id = "4", waybillNumber = "444", carrierId = "kr.cjlogistics", currentPhase = StandardDeliveryPhase.OUT_FOR_DELIVERY, lastUpdatedEpochMs = 200L),
            ShipmentRecord(id = "5", waybillNumber = "555", carrierId = "kr.cjlogistics", currentPhase = StandardDeliveryPhase.IN_TRANSIT)
        )

        val plan = LiveUpdatePolicy.computePlan(
            shipments = records,
            settings = AppSettings(liveUpdatesEnabled = true),
            previouslyActiveIds = setOf("4", "5")
        )

        assertEquals(listOf("1", "2", "3"), plan.activeTargets.map { it.id })
        assertTrue(plan.completedToDemote.isEmpty())
        assertEquals(setOf("4", "5"), plan.idsToCancel)
    }

    @Test
    fun `demotes previously active shipment when delivered`() {
        val delivered = ShipmentRecord(
            id = "done1",
            waybillNumber = "999",
            carrierId = "kr.cjlogistics",
            currentPhase = StandardDeliveryPhase.DELIVERED,
            isLiveUpdatePinned = true
        )

        val plan = LiveUpdatePolicy.computePlan(
            shipments = listOf(delivered),
            settings = AppSettings(liveUpdatesEnabled = true),
            previouslyActiveIds = setOf("done1")
        )

        assertTrue(plan.activeTargets.isEmpty())
        assertEquals(listOf("done1"), plan.completedToDemote.map { it.id })
        assertTrue(plan.idsToCancel.isEmpty())
    }

    @Test
    fun `cancels all active live updates when disabled`() {
        val out = ShipmentRecord(
            id = "o1",
            waybillNumber = "111",
            carrierId = "kr.cjlogistics",
            currentPhase = StandardDeliveryPhase.OUT_FOR_DELIVERY
        )
        val plan = LiveUpdatePolicy.computePlan(
            shipments = listOf(out),
            settings = AppSettings(liveUpdatesEnabled = false),
            previouslyActiveIds = setOf("o1")
        )
        assertTrue(plan.activeTargets.isEmpty())
        assertEquals(setOf("o1"), plan.idsToCancel)
    }
}
