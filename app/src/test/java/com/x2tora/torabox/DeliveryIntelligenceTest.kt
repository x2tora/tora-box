package com.x2tora.torabox

import com.x2tora.torabox.domain.DeliveryIntelligence
import com.x2tora.torabox.model.DeliveryAnomalyType
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.TrackingEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeliveryIntelligenceTest {

    @Test
    fun `detects stalled shipment after 48 hours`() {
        val now = 1_763_568_000_000L // fixed epoch
        val record = ShipmentRecord(
            id = "s1",
            waybillNumber = "123456789012",
            carrierId = "kr.cjlogistics",
            currentStatusLabel = "간선상차",
            currentPhase = StandardDeliveryPhase.IN_TRANSIT,
            lastUpdatedEpochMs = now - (50L * 3_600_000L)
        )
        assertEquals(DeliveryAnomalyType.STALLED, DeliveryIntelligence.detectAnomaly(record, now))
    }

    @Test
    fun `detects returning, misrouted, and customs hold keywords`() {
        val now = 1_763_568_000_000L
        val returning = ShipmentRecord(
            id = "r1",
            waybillNumber = "123456789012",
            carrierId = "kr.cjlogistics",
            currentStatusLabel = "수취거부 반송출고",
            currentPhase = StandardDeliveryPhase.EXCEPTION,
            lastUpdatedEpochMs = now
        )
        val misrouted = returning.copy(id = "m1", currentStatusLabel = "오분류 재분류 이동")
        val customsHold = returning.copy(id = "c1", currentStatusLabel = "통관보류 서류보완 필요")

        assertEquals(DeliveryAnomalyType.RETURNING, DeliveryIntelligence.detectAnomaly(returning, now))
        assertEquals(DeliveryAnomalyType.MISROUTED, DeliveryIntelligence.detectAnomaly(misrouted, now))
        assertEquals(DeliveryAnomalyType.CUSTOMS_HOLD, DeliveryIntelligence.detectAnomaly(customsHold, now))
    }

    @Test
    fun `delivered shipment never reports anomaly`() {
        val now = 1_763_568_000_000L
        val delivered = ShipmentRecord(
            id = "d1",
            waybillNumber = "123456789012",
            carrierId = "kr.cjlogistics",
            currentStatusLabel = "배달완료",
            currentPhase = StandardDeliveryPhase.DELIVERED,
            lastUpdatedEpochMs = now - (90L * 3_600_000L)
        )
        assertNull(DeliveryIntelligence.detectAnomaly(delivered, now))
    }

    @Test
    fun `computes average delivery hours from completed events`() {
        val delivered = ShipmentRecord(
            id = "d2",
            waybillNumber = "123456789012",
            carrierId = "kr.cjlogistics",
            currentPhase = StandardDeliveryPhase.DELIVERED,
            events = listOf(
                TrackingEvent(timeIso = "2026-11-18 14:00"),
                TrackingEvent(timeIso = "2026-11-17 14:00")
            )
        )
        val avg = DeliveryIntelligence.computeAverageDeliveryHours(listOf(delivered), "kr.cjlogistics")
        assertEquals(24.0, avg!!, 0.1)
    }
}
