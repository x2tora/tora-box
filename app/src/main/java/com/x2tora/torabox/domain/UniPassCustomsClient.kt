package com.x2tora.torabox.domain

import com.x2tora.torabox.model.CarrierInfo
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.TrackingEvent
import com.x2tora.torabox.model.TrackingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar

/**
 * Korea Customs Service (UNI-PASS) public cargo clearance progress client
 * for international/cross-border waybills (AliExpress, Temu, EMS, DHL, FedEx, UPS).
 */
object UniPassCustomsClient {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15; SM-S938N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36"

    suspend fun queryCustomsProgress(
        carrier: CarrierInfo,
        waybill: String,
        personalCustomsCode: String = ""
    ): TrackingResult? = withContext(Dispatchers.IO) {
        val clean = WaybillDetector.normalizeWaybill(waybill)
        if (clean.length < 8) return@withContext null
        val year = Calendar.getInstance().get(Calendar.YEAR)

        // UNI-PASS public cargo tracking XML endpoint (HBL query)
        val urlStr = buildString {
            append("https://unipass.customs.go.kr:38010/ext/rest/cargCsclPrgsInfoQry/retrieveCargCsclPrgsInfo")
            append("?blYy=").append(year)
            append("&hblNo=").append(URLEncoder.encode(clean, "UTF-8"))
            if (personalCustomsCode.isNotBlank()) {
                append("&prnm=").append(URLEncoder.encode(personalCustomsCode.trim(), "UTF-8"))
            }
        }

        val body = runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/xml,text/xml,*/*")
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
                } else ""
            } finally {
                conn.disconnect()
            }
        }.getOrDefault("")

        if (body.isBlank()) return@withContext null
        parseCustomsXml(body, clean, carrier.id)
    }

    fun parseCustomsXml(xml: String, waybill: String, carrierId: String): TrackingResult? {
        if (!xml.contains("<cargCsclPrgsInfoQryVo>") && !xml.contains("<cargCsclPrgsInfoDtlQryVo>")) {
            return null
        }

        val itemName = extractTag(xml, "prnm").orEmpty()
        val weight = extractTag(xml, "ttwg").orEmpty()
        val customsStatus = extractTag(xml, "prgsStts").orEmpty()
        val forwardingName = extractTag(xml, "frwrEntsConm").orEmpty()

        val detailBlocks = Regex("<cargCsclPrgsInfoDtlQryVo>(.*?)</cargCsclPrgsInfoDtlQryVo>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .map { it.groupValues[1] }
            .toList()

        val events = detailBlocks.mapNotNull { block ->
            val stepName = extractTag(block, "cargTrcnRelaBsopTpcd").orEmpty().trim()
            if (stepName.isEmpty()) return@mapNotNull null
            val rawTime = extractTag(block, "prcsDttm").orEmpty().trim()
            val formattedTime = formatUniPassTimestamp(rawTime)
            val shed = extractTag(block, "shedNm").orEmpty().trim()
            val note = extractTag(block, "rlbrCn").orEmpty().trim()
            val phase = mapCustomsStepToPhase(stepName)

            TrackingEvent(
                timeIso = formattedTime,
                displayTime = formattedTime,
                statusName = stepName,
                phase = phase,
                carrierStageIndex = phase.stepIndex,
                location = shed.ifBlank { "관세청 UNI-PASS" },
                description = note.ifBlank { stepName }
            )
        }.sortedByDescending { it.timeIso }

        if (events.isEmpty() && customsStatus.isBlank()) return null

        val latest = events.firstOrNull()
        val currentPhase = latest?.phase ?: mapCustomsStepToPhase(customsStatus)
        val statusLabel = latest?.statusName ?: customsStatus.ifBlank { "통관 조회됨" }

        // Look for domestic handoff waybill if mentioned in remarks
        val domesticHint = WaybillDetector.parseNotificationText(
            detailBlocks.joinToString(" ") { extractTag(it, "rlbrCn").orEmpty() }
        )

        return TrackingResult(
            carrierId = carrierId,
            waybillNumber = waybill,
            itemName = itemName,
            itemQuantity = if (weight.isNotBlank()) "${weight}kg" else "",
            senderName = forwardingName,
            currentStatusLabel = statusLabel,
            currentPhase = currentPhase,
            currentCarrierStageIndex = currentPhase.stepIndex,
            events = events,
            syncSource = "관세청 UNI-PASS",
            linkedDomesticWaybill = domesticHint.waybillNumber.takeIf { it != waybill }.orEmpty(),
            linkedDomesticCarrierId = domesticHint.carrierHintId.orEmpty()
        )
    }

    private fun mapCustomsStepToPhase(step: String): StandardDeliveryPhase {
        val s = step.trim()
        return when {
            s.contains("보류") || s.contains("검사") || s.contains("오류") -> StandardDeliveryPhase.EXCEPTION
            s.contains("반출") || s.contains("인계") -> StandardDeliveryPhase.ARRIVED_AT_BRANCH
            s.contains("통관") || s.contains("수리") || s.contains("하선") || s.contains("입항") -> StandardDeliveryPhase.IN_TRANSIT
            else -> StandardDeliveryPhase.IN_TRANSIT
        }
    }

    private fun formatUniPassTimestamp(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        if (digits.length >= 12) {
            return "${digits.substring(0, 4)}-${digits.substring(4, 6)}-${digits.substring(6, 8)} ${digits.substring(8, 10)}:${digits.substring(10, 12)}"
        }
        return raw
    }

    private fun extractTag(xml: String, tag: String): String? {
        val match = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL).find(xml)
        return match?.groupValues?.getOrNull(1)
            ?.replace("<![CDATA[", "")
            ?.replace("]]>", "")
            ?.trim()
    }
}
