package com.x2tora.torabox

import com.x2tora.torabox.domain.ShipmentMaintenance
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShipmentMaintenanceTest {

    @Test
    fun `applyAutoArchive archives expired delivered non-pinned shipments`() {
        val now = 1_700_000_000_000L
        val dayMs = 86_400_000L

        val expiredDelivered = ShipmentRecord(
            id = "expired",
            waybillNumber = "123456789012",
            carrierId = "kr.cjlogistics",
            currentPhase = StandardDeliveryPhase.DELIVERED,
            lastUpdatedEpochMs = now - 8 * dayMs
        )
        val pinnedDelivered = ShipmentRecord(
            id = "pinned",
            waybillNumber = "123456789013",
            carrierId = "kr.cjlogistics",
            currentPhase = StandardDeliveryPhase.DELIVERED,
            isPinned = true,
            lastUpdatedEpochMs = now - 10 * dayMs
        )
        val recentDelivered = ShipmentRecord(
            id = "recent",
            waybillNumber = "123456789014",
            carrierId = "kr.cjlogistics",
            currentPhase = StandardDeliveryPhase.DELIVERED,
            lastUpdatedEpochMs = now - 2 * dayMs
        )
        val inTransit = ShipmentRecord(
            id = "transit",
            waybillNumber = "123456789015",
            carrierId = "kr.cjlogistics",
            currentPhase = StandardDeliveryPhase.IN_TRANSIT,
            lastUpdatedEpochMs = now - 20 * dayMs
        )

        val (result, count) = ShipmentMaintenance.applyAutoArchive(
            shipments = listOf(expiredDelivered, pinnedDelivered, recentDelivered, inTransit),
            days = 7,
            nowEpochMs = now
        )

        assertEquals(1, count)
        assertTrue(result.first { it.id == "expired" }.isArchived)
        assertFalse(result.first { it.id == "pinned" }.isArchived)
        assertFalse(result.first { it.id == "recent" }.isArchived)
        assertFalse(result.first { it.id == "transit" }.isArchived)
    }

    @Test
    fun `applyAutoArchive does nothing when days is zero`() {
        val now = 1_700_000_000_000L
        val record = ShipmentRecord(
            id = "1",
            waybillNumber = "123456789012",
            carrierId = "kr.cjlogistics",
            currentPhase = StandardDeliveryPhase.DELIVERED,
            lastUpdatedEpochMs = now - 30 * 86_400_000L
        )
        val (result, count) = ShipmentMaintenance.applyAutoArchive(listOf(record), days = 0, nowEpochMs = now)
        assertEquals(0, count)
        assertFalse(result.first().isArchived)
    }
}
