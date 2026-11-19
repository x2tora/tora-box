package com.x2tora.torabox.domain

import com.x2tora.torabox.model.DeliveryAnomalyType
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.TrackingEvent
import java.text.SimpleDateFormat
import java.util.Locale

object DeliveryIntelligence {
    private const val STALLED_THRESHOLD_MS = 48L * 3_600_000L

    private val returningKeywords = listOf("반송", "반품", "수취거부", "수취 거절", "주소불명", "주소 불명", "폐기", "반송출고")
    private val misroutedKeywords = listOf("오분류", "오배송", "재분류", "타지역", "잘못된 권역")
    private val customsHoldKeywords = listOf("통관보류", "통관 보류", "검사대상", "서류보완", "과세보류", "통관지연")

    private val dateFormats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy.MM.dd HH:mm:ss",
        "yyyy.MM.dd HH:mm",
        "yyyy/MM/dd HH:mm"
    )

    fun detectAnomaly(
        record: ShipmentRecord,
        nowEpochMs: Long = System.currentTimeMillis()
    ): DeliveryAnomalyType? {
        if (record.currentPhase == StandardDeliveryPhase.DELIVERED) return null

        val latestText = buildString {
            append(record.currentStatusLabel)
            append(' ')
            record.events.take(2).forEach { ev ->
                append(ev.statusName).append(' ').append(ev.description).append(' ')
            }
        }

        if (customsHoldKeywords.any { latestText.contains(it, ignoreCase = true) }) {
            return DeliveryAnomalyType.CUSTOMS_HOLD
        }
        if (returningKeywords.any { latestText.contains(it, ignoreCase = true) }) {
            return DeliveryAnomalyType.RETURNING
        }
        if (misroutedKeywords.any { latestText.contains(it, ignoreCase = true) }) {
            return DeliveryAnomalyType.MISROUTED
        }

        val lastEventEpochMs = record.events.firstOrNull()?.let { parseEventEpochMs(it) }
            ?: record.lastUpdatedEpochMs.takeIf { it > 0L }
            ?: record.createdAtEpochMs

        if (lastEventEpochMs > 0L && (nowEpochMs - lastEventEpochMs) >= STALLED_THRESHOLD_MS) {
            return DeliveryAnomalyType.STALLED
        }
        return null
    }

    fun estimateDeliveryLabel(
        record: ShipmentRecord,
        historicalSameCarrierHours: Double? = null
    ): String {
        return when (record.currentPhase) {
            StandardDeliveryPhase.DELIVERED -> ""
            StandardDeliveryPhase.EXCEPTION -> "확인 필요"
            StandardDeliveryPhase.OUT_FOR_DELIVERY -> "오늘 도착 예상"
            StandardDeliveryPhase.ARRIVED_AT_BRANCH -> {
                if (historicalSameCarrierHours != null && historicalSameCarrierHours <= 30.0) {
                    "오늘·내일 도착 예상"
                } else {
                    "내일 오전·오후 예상"
                }
            }
            StandardDeliveryPhase.IN_TRANSIT -> "1~2일 내 도착 예상"
            StandardDeliveryPhase.PICKED_UP -> "1~3일 내 도착 예상"
            StandardDeliveryPhase.PREPARING -> "발송 대기 중"
        }
    }

    fun computeAverageDeliveryHours(shipments: List<ShipmentRecord>, carrierId: String): Double? {
        val durations = shipments
            .filter { it.carrierId == carrierId && it.currentPhase == StandardDeliveryPhase.DELIVERED && it.events.size >= 2 }
            .mapNotNull { record ->
                val newest = parseEventEpochMs(record.events.first()) ?: return@mapNotNull null
                val oldest = parseEventEpochMs(record.events.last()) ?: return@mapNotNull null
                val diffHours = (newest - oldest).toDouble() / 3_600_000.0
                diffHours.takeIf { it in 2.0..240.0 }
            }
        return if (durations.isEmpty()) null else durations.average()
    }

    internal fun parseEventEpochMs(event: TrackingEvent): Long? {
        val raw = event.timeIso.ifBlank { event.displayTime }.trim()
        if (raw.isEmpty()) return null
        val normalized = raw.replace('T', ' ').take(19)
        for (pattern in dateFormats) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.KOREA).apply { isLenient = false }.parse(normalized)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }
}
