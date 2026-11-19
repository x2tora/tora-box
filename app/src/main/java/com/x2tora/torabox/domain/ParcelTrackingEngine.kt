package com.x2tora.torabox.domain

import android.content.Context
import com.x2tora.torabox.model.CarrierCategory
import com.x2tora.torabox.model.CarrierInfo
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.TrackingEvent
import com.x2tora.torabox.model.TrackingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset

object ParcelTrackingEngine {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15; SM-S938N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Mobile Safari/537.36"

    private data class HttpReply(
        val statusCode: Int,
        val body: String,
        val cookies: String = ""
    )

    suspend fun track(
        carrier: CarrierInfo,
        rawWaybill: String,
        sweetTrackerApiKey: String = "",
        customTrackerEndpoint: String = "https://apis.tracker.delivery",
        appContext: Context? = null,
        personalCustomsCode: String = ""
    ): Result<TrackingResult> = withContext(Dispatchers.IO) {
        val startedMs = System.currentTimeMillis()
        val waybill = WaybillDetector.normalizeWaybill(rawWaybill)
        if (waybill.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("송장번호를 입력해 주세요"))
        }

        fun withLatency(res: TrackingResult): TrackingResult =
            res.copy(latencyMs = (System.currentTimeMillis() - startedMs).coerceAtLeast(1L))

        // 1. Optional SweetTracker API (covers all 73+ domestic carriers with item/sender metadata)
        val trimmedKey = sweetTrackerApiKey.trim()
        if (trimmedKey.isNotEmpty() && carrier.sweetTrackerCode.all(Char::isDigit)) {
            val stResult = runCatching { querySweetTracker(carrier, waybill, trimmedKey) }.getOrNull()
            if (stResult != null && (stResult.events.isNotEmpty() || stResult.itemName.isNotBlank())) {
                return@withContext Result.success(withLatency(stResult))
            }
        }

        // 2. Direct Korean Carrier Endpoint / Scraper (no API key needed)
        val directResult = runCatching { queryDirectCarrier(carrier, waybill) }.getOrNull()
        if (directResult != null && (directResult.events.isNotEmpty() || directResult.itemName.isNotBlank() || directResult.senderName.isNotBlank())) {
            return@withContext Result.success(withLatency(directResult))
        }

        // 3. Universal public tracker.delivery REST endpoint fallback
        val trackerId = carrier.trackerDeliveryId
        if (!trackerId.isNullOrBlank()) {
            val fallback = runCatching {
                queryTrackerDeliveryRest(carrier, waybill, customTrackerEndpoint)
            }
            val res = fallback.getOrNull()
            if (res != null && res.events.isNotEmpty()) {
                return@withContext Result.success(withLatency(res))
            }
        }

        // 4. For convenience-store parcels (GS25/CU), fallback to CJ Logistics if CVSnet returned empty
        if (carrier.id == "kr.cvsnet" || carrier.id == "kr.cupost") {
            val cjCarrier = KoreanCarrierCatalog.findById("kr.cjlogistics")
            if (cjCarrier != null) {
                val cjRes = runCatching { queryCjLogistics(cjCarrier, waybill) }.getOrNull()
                if (cjRes != null && (cjRes.events.isNotEmpty() || cjRes.itemName.isNotBlank())) {
                    return@withContext Result.success(
                        withLatency(
                            cjRes.copy(
                                carrierId = carrier.id,
                                serviceType = cjRes.serviceType.ifBlank { "${carrier.shortName} 연계배송" }
                            )
                        )
                    )
                }
            }
        }

        // 5. UNI-PASS Korea Customs lookup for international shipments
        if (carrier.category == CarrierCategory.INTERNATIONAL || carrier.id == "kr.epost.ems") {
            val customsRes = runCatching {
                UniPassCustomsClient.queryCustomsProgress(carrier, waybill, personalCustomsCode)
            }.getOrNull()
            if (customsRes != null && customsRes.events.isNotEmpty()) {
                return@withContext Result.success(withLatency(customsRes))
            }
        }

        // 6. Offscreen WebView JS-rendered fallback when HTTP endpoints returned empty
        if (appContext != null) {
            val webRes = runCatching {
                HiddenWebViewScraper.fetchTracking(appContext, carrier, waybill)
            }.getOrNull()
            if (webRes != null && webRes.events.isNotEmpty()) {
                return@withContext Result.success(withLatency(webRes))
            }
        }

        Result.failure(
            IllegalStateException(
                "${carrier.shortName}에서 송장번호(${waybill})의 배송 정보를 찾지 못했습니다. 송장번호를 확인하거나 공식 조회 페이지를 열어보세요."
            )
        )
    }

    suspend fun probeAndTrackWaybill(
        rawWaybill: String,
        carrierHintId: String? = null,
        sweetTrackerApiKey: String = "",
        customTrackerEndpoint: String = "https://apis.tracker.delivery",
        appContext: Context? = null,
        personalCustomsCode: String = ""
    ): Result<TrackingResult> = coroutineScope {
        val waybill = WaybillDetector.normalizeWaybill(rawWaybill)
        val candidates = WaybillDetector.recommendCarriers(waybill, carrierHintId)
            .map { it.carrier }
            .distinctBy { it.id }
            .take(6)

        if (candidates.isEmpty()) {
            return@coroutineScope Result.failure(IllegalArgumentException("추측 가능한 택배사 형식이 아닙니다"))
        }

        val deferredResults = candidates.map { candidate ->
            async(Dispatchers.IO) {
                track(
                    carrier = candidate,
                    rawWaybill = waybill,
                    sweetTrackerApiKey = sweetTrackerApiKey,
                    customTrackerEndpoint = customTrackerEndpoint,
                    appContext = appContext,
                    personalCustomsCode = personalCustomsCode
                ).getOrNull()
            }
        }
        val hits = deferredResults.awaitAll().filterNotNull()
        val best = hits.maxByOrNull { hit ->
            var score = hit.events.size * 10
            if (hit.itemName.isNotBlank()) score += 25
            if (hit.senderName.isNotBlank()) score += 15
            score
        }
        if (best != null) {
            Result.success(best)
        } else {
            Result.failure(
                IllegalStateException("추천된 ${candidates.size}개 택배사에서 아직 등록된 배송 이력을 찾지 못했습니다.")
            )
        }
    }

    private fun queryDirectCarrier(carrier: CarrierInfo, waybill: String): TrackingResult? {
        return when (carrier.id) {
            "kr.cjlogistics", "kr.cjlogistics.intl" -> queryCjLogistics(carrier, waybill)
            "kr.epost" -> queryKoreaPost(carrier, waybill)
            "kr.epost.ems" -> queryKoreaPostEms(carrier, waybill)
            "kr.hanjin", "kr.hanjin.b2b" -> queryHanjin(carrier, waybill)
            "kr.lotte", "kr.lotte.global" -> queryLotte(carrier, waybill)
            "kr.logen" -> queryLogen(carrier, waybill)
            "kr.cvsnet" -> queryCvsnet(carrier, waybill)
            "kr.cupost" -> queryCupost(carrier, waybill)
            "kr.coupangls" -> queryCoupangCls(carrier, waybill)
            "kr.kdexp" -> queryKyungdong(carrier, waybill)
            "kr.daesin" -> queryDaesin(carrier, waybill)
            "kr.chunilps" -> queryChunil(carrier, waybill)
            "kr.ilyanglogis" -> queryIlyang(carrier, waybill)
            "kr.hdexp" -> queryHapdong(carrier, waybill)
            "kr.kunyoung" -> queryKunyoung(carrier, waybill)
            "kr.slx" -> querySlx(carrier, waybill)
            "kr.homepick", "kr.homepick.today" -> queryHomepick(carrier, waybill)
            "kr.todaypickup" -> queryTodayPickup(carrier, waybill)
            "kr.yongmalogis" -> queryYongma(carrier, waybill)
            "kr.honamlogis" -> queryHonam(carrier, waybill)
            "kr.cway" -> queryCway(carrier, waybill)
            "kr.goodstoluck" -> queryGoodsToLuck(carrier, waybill)
            "kr.epantos" -> queryPantos(carrier, waybill)
            else -> null
        }
    }

    // ==================== 1. CJ대한통운 ====================
    private fun queryCjLogistics(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val encoded = URLEncoder.encode(waybill, "UTF-8")
        val waybillReply = httpPost(
            urlStr = "https://trace.cjlogistics.com/next/rest/selectTrackingWaybil.do",
            formBody = "wblNo=$encoded"
        )
        val detailReply = httpPost(
            urlStr = "https://trace.cjlogistics.com/next/rest/selectTrackingDetailList.do",
            formBody = "wblNo=$encoded"
        )
        val restParsed = parseCjNextRestJson(carrier, waybill, waybillReply.body, detailReply.body)
        if (restParsed != null) return restParsed

        // Legacy CSRF endpoint fallback
        val initPage = httpGet("https://www.cjlogistics.com/ko/tool/parcel/tracking")
        val csrf = Regex("""name=["']_csrf["'][^>]*value=["']([^"']+)["']""")
            .find(initPage.body)?.groupValues?.getOrNull(1)
            ?: Regex("""value=["']([^"']+)["'][^>]*name=["']_csrf["']""")
                .find(initPage.body)?.groupValues?.getOrNull(1)
            ?: return null
        val postUrl = "https://www.cjlogistics.com/ko/tool/parcel/tracking-detail?paramInvcNo=$encoded&_csrf=${URLEncoder.encode(csrf, "UTF-8")}"
        val detailJsonReply = httpPost(postUrl, "", cookies = initPage.cookies)
        return parseCjLegacyDetailJson(carrier, waybill, detailJsonReply.body)
    }

    fun parseCjNextRestJson(
        carrier: CarrierInfo,
        waybill: String,
        waybillJsonText: String,
        detailJsonText: String
    ): TrackingResult? {
        val wbRoot = runCatching { JSONObject(waybillJsonText) }.getOrNull()
        val dtRoot = runCatching { JSONObject(detailJsonText) }.getOrNull()
        val wbData = wbRoot?.optJSONObject("data")
        val dtData = dtRoot?.optJSONObject("data")
        val list = dtData?.optJSONArray("svcOutList") ?: JSONArray()
        if (wbData == null && list.length() == 0) return null

        val repGoods = wbData?.optString("repGoodsNm", "").orEmpty().trim()
        val dtlGoods = wbData?.optString("goodsDtlNm", "").orEmpty().trim()
        val itemName = listOf(repGoods, dtlGoods).filter { it.isNotBlank() && it != "null" }.distinct().joinToString(" ")
        val qty = cleanValue(wbData?.optString("qty"))
        val senderName = cleanValue(wbData?.optString("sndrNm"))
        val senderPhone = cleanValue(wbData?.optString("sndrClphno"))
        val senderAddr = cleanValue(wbData?.optString("sndrAddr"))
        val recipientName = cleanValue(wbData?.optString("rcvrNm"))
        val recipientPhone = cleanValue(wbData?.optString("rcvrClphno"))
        val recipientAddr = cleanValue(wbData?.optString("rcvrAddr"))
        val accepterName = listOf(cleanValue(wbData?.optString("acprNm")), cleanValue(wbData?.optString("acprRlpDnm")))
            .filter { it.isNotBlank() }
            .joinToString(" · ")

        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until list.length()) {
            val row = list.optJSONObject(i) ?: continue
            val workDt = cleanValue(row.optString("workDt"))
            val workHms = cleanValue(row.optString("workHms"))
            val branNm = cleanValue(row.optString("branNm"))
            val tel = cleanValue(row.optString("procBranTelNo"))
            val statusName = cleanValue(row.optString("crgStDnm")).ifBlank { "이동중" }
            val desc = cleanValue(row.optString("crgStDcdVal"))
            val partner = cleanValue(row.optString("patnBranNm"))
            val fullDesc = buildString {
                append(desc)
                if (partner.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append("상대점소: $partner")
                }
            }
            val (driverName, driverPhone) = extractDriverInfo("$desc $statusName")
            val phase = KoreanCarrierCatalog.inferPhaseFromText(statusName)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, statusName, phase)
            val formattedTime = formatDateTimePair(workDt, workHms)
            events += TrackingEvent(
                timeIso = formattedTime,
                displayTime = formattedTime,
                statusName = statusName,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = branNm,
                branchPhone = tel,
                driverName = driverName,
                driverPhone = driverPhone,
                description = fullDesc
            )
        }
        val sortedEvents = events.sortedByDescending { it.displayTime }
        val latest = sortedEvents.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = itemName,
            itemQuantity = qty,
            senderName = senderName,
            senderPhone = senderPhone,
            senderAddress = senderAddr,
            recipientName = recipientName,
            recipientPhone = recipientPhone,
            recipientAddress = recipientAddr,
            accepterName = accepterName,
            originWaybill = cleanValue(wbData?.optString("ognWblno")),
            returnWaybill = cleanValue(wbData?.optString("rtnWblno")),
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sortedEvents,
            syncSource = "CJ대한통운 직접조회"
        )
    }

    fun parseCjLegacyDetailJson(carrier: CarrierInfo, waybill: String, jsonText: String): TrackingResult? {
        val root = runCatching { JSONObject(jsonText) }.getOrNull() ?: return null
        val parcelResult = root.optJSONObject("parcelResultMap")?.optJSONArray("resultList")?.optJSONObject(0)
        val detailArray = root.optJSONObject("parcelDetailResultMap")?.optJSONArray("resultList") ?: JSONArray()
        if (parcelResult == null && detailArray.length() == 0) return null

        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until detailArray.length()) {
            val row = detailArray.optJSONObject(i) ?: continue
            val scanNm = cleanValue(row.optString("scanNm")).ifBlank { "이동중" }
            val crgNm = cleanValue(row.optString("crgNm"))
            val loc = cleanValue(row.optString("regBranNm"))
            val dTime = cleanValue(row.optString("dTime")).removeSuffix(".0")
            val (driverName, driverPhone) = extractDriverInfo(crgNm)
            val phase = when (cleanValue(row.optString("crgSt"))) {
                "11" -> StandardDeliveryPhase.PICKED_UP
                "21", "41", "42", "44" -> StandardDeliveryPhase.IN_TRANSIT
                "82" -> StandardDeliveryPhase.OUT_FOR_DELIVERY
                "91" -> StandardDeliveryPhase.DELIVERED
                else -> KoreanCarrierCatalog.inferPhaseFromText(scanNm)
            }
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, scanNm, phase)
            val dispTime = dTime.replace("-", ".")
            events += TrackingEvent(
                timeIso = dTime,
                displayTime = dispTime.take(16),
                statusName = scanNm,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = loc,
                driverName = driverName,
                driverPhone = driverPhone,
                description = crgNm
            )
        }
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = cleanValue(parcelResult?.optString("itemNm")),
            itemQuantity = cleanValue(parcelResult?.optString("qty")),
            senderName = cleanValue(parcelResult?.optString("sendrNm")),
            recipientName = cleanValue(parcelResult?.optString("rcvrNm")),
            serviceType = cleanValue(parcelResult?.optString("nsDlvNm")),
            originWaybill = cleanValue(parcelResult?.optString("oriTrspbillnum")),
            returnWaybill = cleanValue(parcelResult?.optString("rtnTrspbillnum")),
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "CJ대한통운 직접조회"
        )
    }

    // ==================== 2. 우체국택배 ====================
    private fun queryKoreaPost(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpGet("https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm?displayHeader=N&sid1=${URLEncoder.encode(waybill, "UTF-8")}")
        return parseKoreaPostHtml(carrier, waybill, reply.body)
    }

    private fun queryKoreaPostEms(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpGet("https://service.epost.go.kr/trace.RetrieveEmsRigiTraceList.comm?displayHeader=N&POST_CODE=${URLEncoder.encode(waybill, "UTF-8")}")
        val parsed = parseKoreaPostHtml(carrier, waybill, reply.body)
        if (parsed != null) return parsed
        return queryKoreaPost(carrier, waybill)
    }

    fun parseKoreaPostHtml(carrier: CarrierInfo, waybill: String, html: String): TrackingResult? {
        val tableColHtml = Regex("""<table[^>]*class=["'][^"']*table_col[^"']*["'][^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1).orEmpty()
        val infoTds = extractTdTextsPreserveBreaks(tableColHtml)

        var senderName = ""
        var recipientName = ""
        var serviceType = ""
        var itemName = ""
        var accepter = ""

        if (infoTds.isNotEmpty()) {
            val senderLines = infoTds.getOrNull(0)?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
            senderName = senderLines.firstOrNull().orEmpty()
        }
        if (infoTds.size >= 2) {
            val recvLines = infoTds.getOrNull(1)?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
            recipientName = recvLines.firstOrNull().orEmpty()
        }
        if (infoTds.size >= 3) {
            val midText = infoTds.getOrNull(2).orEmpty().replace("\n", " ").trim()
            if (midText.isNotBlank()) serviceType = midText
        }
        if (infoTds.size >= 4) {
            val lastText = infoTds.getOrNull(3).orEmpty().replace("\n", " ").trim()
            if (lastText.isNotBlank() && !lastText.contains("배달")) {
                itemName = lastText
            } else if (lastText.contains("수령") || lastText.contains("배달완료")) {
                accepter = lastText
            }
        }
        if (infoTds.size >= 5) {
            val extra = infoTds[4].replace("\n", " ").trim()
            if (extra.isNotBlank()) accepter = extra
        }

        val processTableHtml = Regex("""<table[^>]*id=["']processTable["'][^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1).orEmpty()
        val tbodyHtml = Regex("""<tbody[^>]*>([\s\S]*?)</tbody>""", RegexOption.IGNORE_CASE)
            .find(processTableHtml)?.groupValues?.getOrNull(1) ?: processTableHtml

        val rows = extractTrs(tbodyHtml)
        val events = mutableListOf<TrackingEvent>()
        for (rowHtml in rows) {
            val cols = extractTdTextsPreserveBreaks(rowHtml).map { it.replace("\n", " ").replace(Regex("\\s+"), " ").trim() }
            if (cols.size < 4) continue
            val date = cols[0]
            val time = cols[1]
            val location = cols[2]
            var statusRaw = cols[3]
            if (statusRaw.startsWith("접수 소포 물품 사진") || statusRaw.contains("CDATA")) {
                statusRaw = statusRaw.substringBefore("//").substringBefore("소포 물품 사진").trim().ifBlank { "접수" }
            }
            val (driverName, driverPhone) = extractDriverInfo(statusRaw)
            val phase = KoreanCarrierCatalog.inferPhaseFromText(statusRaw)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, statusRaw, phase)
            val dispTime = "$date $time".trim()
            events += TrackingEvent(
                timeIso = dispTime,
                displayTime = dispTime,
                statusName = statusRaw.ifBlank { "처리중" },
                phase = phase,
                carrierStageIndex = stageIdx,
                location = location,
                driverName = driverName,
                driverPhone = driverPhone,
                description = statusRaw
            )
        }

        if (events.isEmpty() && senderName.isBlank()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = itemName,
            senderName = senderName,
            recipientName = recipientName,
            accepterName = accepter,
            serviceType = serviceType,
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "우체국 직접조회"
        )
    }

    // ==================== 3. 한진택배 ====================
    private fun queryHanjin(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val body = "wblnum=${URLEncoder.encode(waybill, "UTF-8")}&mCode=MN038&schLang=KR"
        val reply = httpPost("https://www.hanjin.com/kor/CMS/DeliveryMgr/WaybillResult.do", body)
        return parseHanjinHtml(carrier, waybill, reply.body)
    }

    fun parseHanjinHtml(carrier: CarrierInfo, waybill: String, html: String): TrackingResult? {
        if (html.contains("운송장이 등록되지 않")) return null
        val tables = Regex("""<table[^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE)
            .findAll(html).map { it.groupValues[1] }.toList()
        if (tables.size < 2) return null

        val infoMap = extractThTdMap(tables[0])
        val infoTds = extractTdTextsPreserveBreaks(tables[0]).map { it.replace("\n", " ").trim() }

        val itemName = infoMap.entries.firstOrNull { it.key.contains("상품") || it.key.contains("품명") }?.value
            ?: infoTds.getOrNull(0).orEmpty()
        val senderName = infoMap.entries.firstOrNull { it.key.contains("보내") || it.key.contains("송하") }?.value
            ?: infoTds.getOrNull(1).orEmpty()
        val recipientName = infoMap.entries.firstOrNull { it.key.contains("받으") || it.key.contains("수하") }?.value
            ?: infoTds.getOrNull(2).orEmpty()

        val events = mutableListOf<TrackingEvent>()
        val eventRows = extractTrs(tables[1])
        for (rowHtml in eventRows) {
            val cols = extractTdTextsPreserveBreaks(rowHtml).map { it.replace("\n", " ").replace(Regex("\\s+"), " ").trim() }
            if (cols.size < 4) continue
            val date = cols[0]
            val time = cols[1]
            val location = cols[2]
            val desc = cols[3]
            val phone = cols.getOrNull(4).orEmpty()
            val (driverName, driverPhone) = extractDriverInfo("$desc $phone")
            val phase = KoreanCarrierCatalog.inferPhaseFromText(desc)
            val statusShort = shortenHanjinStatus(desc)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, "$statusShort $desc", phase)
            val dispTime = "${date.replace("-", ".")} $time".trim()
            events += TrackingEvent(
                timeIso = "$date $time".trim(),
                displayTime = dispTime,
                statusName = statusShort,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = location,
                driverName = driverName,
                driverPhone = driverPhone,
                description = desc
            )
        }

        if (events.isEmpty() && senderName.isBlank() && itemName.isBlank()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = itemName,
            senderName = senderName,
            recipientName = recipientName,
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "한진택배 직접조회"
        )
    }

    private fun shortenHanjinStatus(desc: String): String = when {
        desc.contains("배송완료") -> "배송완료"
        desc.contains("배송출발") -> "배송출발"
        desc.contains("배송준비") || desc.contains("배송을 준비") -> "배송준비중"
        desc.contains("도착") || desc.contains("입고") -> "터미널도착"
        desc.contains("이동중") || desc.contains("상차") -> "터미널이동"
        desc.contains("집하") -> "집하"
        desc.contains("접수") || desc.contains("등록") -> "운송장등록"
        else -> desc.take(18)
    }

    // ==================== 4. 롯데택배 ====================
    private fun queryLotte(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val jsonReply = httpGet("https://ftr.alps.llogis.com:18260/openapi/ftr/getCustomerInvTracking?invNo=${URLEncoder.encode(waybill, "UTF-8")}")
        val apiResult = parseLotteOpenApiJson(carrier, waybill, jsonReply.body)

        // Try public tracking web page to enrich item/sender if available
        val webReply = httpPost(
            "https://www.lotteglogis.com/home/reservation/tracking/linkView",
            "InvNo=${URLEncoder.encode(waybill, "UTF-8")}"
        )
        val webInfo = extractThTdMap(webReply.body)
        val webItem = webInfo.entries.firstOrNull { it.key.contains("상품") || it.key.contains("품명") }?.value.orEmpty()
        val webSender = webInfo.entries.firstOrNull { it.key.contains("보내") || it.key.contains("송하") }?.value.orEmpty()
        val webRecv = webInfo.entries.firstOrNull { it.key.contains("받으") || it.key.contains("수하") }?.value.orEmpty()

        if (apiResult != null) {
            return apiResult.copy(
                itemName = apiResult.itemName.ifBlank { webItem },
                senderName = apiResult.senderName.ifBlank { webSender },
                recipientName = apiResult.recipientName.ifBlank { webRecv }
            )
        }
        return parseLotteHtmlFallback(carrier, waybill, webReply.body)
    }

    fun parseLotteOpenApiJson(carrier: CarrierInfo, waybill: String, jsonText: String): TrackingResult? {
        val root = runCatching { JSONObject(jsonText) }.getOrNull() ?: return null
        val tracking = root.optJSONArray("tracking") ?: return null
        if (tracking.length() == 0) return null

        val userObj = root.optJSONObject("user")
        val resultObj = root.optJSONObject("result")
        val itemName = cleanValue(resultObj?.optString("GODS_NM")).ifBlank { cleanValue(resultObj?.optString("ITEM_NM")) }
        val senderName = cleanValue(userObj?.optString("SNDR_NM")).ifBlank { cleanValue(resultObj?.optString("SNDR_NM")) }
        val recipientName = cleanValue(userObj?.optString("RCVR_NM")).ifBlank { cleanValue(resultObj?.optString("RCVR_NM")) }

        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until tracking.length()) {
            val item = tracking.optJSONObject(i) ?: continue
            val statCd = cleanValue(item.optString("GODS_STAT_CD"))
            val statNm = cleanValue(item.optString("GODS_STAT_NM")).ifBlank { "이동중" }
            val ymd = cleanValue(item.optString("SCAN_YMD"))
            val tme = cleanValue(item.optString("SCAN_TME")).replace("-", "0")
            val branch = cleanValue(item.optString("BRNSHP_NM"))
            val branchTel = cleanValue(item.optString("BRNSHP_TEL"))
            val empNm = cleanValue(item.optString("EMP_NM"))
            val empTel = cleanValue(item.optString("EMP_TEL"))
            val statusDesc = cleanValue(item.optString("STATUS"))
            val partner = cleanValue(item.optString("PTN_BRNSHP_NM"))

            val phase = when (statCd) {
                "09" -> StandardDeliveryPhase.EXCEPTION
                "10" -> StandardDeliveryPhase.PICKED_UP
                "12" -> StandardDeliveryPhase.PREPARING
                "20", "21", "24", "25" -> StandardDeliveryPhase.IN_TRANSIT
                "40" -> StandardDeliveryPhase.OUT_FOR_DELIVERY
                "41", "45" -> StandardDeliveryPhase.DELIVERED
                else -> KoreanCarrierCatalog.inferPhaseFromText(statNm)
            }
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, statNm, phase)
            val dispTime = formatDateTimePair(ymd, tme)
            val fullDesc = listOf(partner, statusDesc).filter { it.isNotBlank() }.joinToString(" · ")
            events += TrackingEvent(
                timeIso = dispTime,
                displayTime = dispTime,
                statusName = statNm,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = branch,
                branchPhone = branchTel,
                driverName = empNm,
                driverPhone = empTel,
                description = fullDesc
            )
        }
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = itemName,
            senderName = senderName,
            recipientName = recipientName,
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "롯데택배 직접조회"
        )
    }

    private fun parseLotteHtmlFallback(carrier: CarrierInfo, waybill: String, html: String): TrackingResult? {
        val infoMap = extractThTdMap(html)
        val events = mutableListOf<TrackingEvent>()
        val tables = Regex("""<table[^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE).findAll(html).map { it.groupValues[1] }.toList()
        for (tbl in tables) {
            if (!tbl.contains("단계") && !tbl.contains("현황") && !tbl.contains("처리")) continue
            for (rowHtml in extractTrs(tbl)) {
                val cols = extractTdTextsPreserveBreaks(rowHtml).map { it.replace("\n", " ").replace(Regex("\\s+"), " ").trim() }
                if (cols.size < 4) continue
                val status = cols[0]
                val dispTime = cols[1].replace("-", ".")
                val location = cols[2]
                val desc = cols[3]
                val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
                val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
                events += TrackingEvent(
                    timeIso = dispTime,
                    displayTime = dispTime,
                    statusName = status,
                    phase = phase,
                    carrierStageIndex = stageIdx,
                    location = location,
                    description = desc
                )
            }
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = infoMap.entries.firstOrNull { it.key.contains("상품") || it.key.contains("품명") }?.value.orEmpty(),
            senderName = infoMap.entries.firstOrNull { it.key.contains("보내") || it.key.contains("송하") }?.value.orEmpty(),
            recipientName = infoMap.entries.firstOrNull { it.key.contains("받으") || it.key.contains("수하") }?.value.orEmpty(),
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "롯데택배 웹조회"
        )
    }

    // ==================== 5. 로젠택배 ====================
    private fun queryLogen(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpGet("https://www.ilogen.com/web/personal/trace/${URLEncoder.encode(waybill, "UTF-8")}")
        return parseLogenHtml(carrier, waybill, reply.body)
    }

    fun parseLogenHtml(carrier: CarrierInfo, waybill: String, html: String): TrackingResult? {
        if (html.contains("배송자료를 조회할 수 없습니다") && !html.contains("tkInfo")) {
            // Check if pdInfo still empty
        }
        val pdTableHtml = Regex("""<table[^>]*class=["'][^"']*pdInfo[^"']*["'][^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1).orEmpty()
        val pdMap = extractThTdMap(pdTableHtml)

        val itemName = pdMap.entries.firstOrNull { it.key.contains("물품") || it.key.contains("품명") || it.key.contains("상품") }?.value.orEmpty()
        val qty = pdMap.entries.firstOrNull { it.key.contains("수량") }?.value.orEmpty()
        val senderName = pdMap.entries.firstOrNull { it.key.contains("보내") || it.key.contains("송하") }?.value.orEmpty()
        val recipientName = pdMap.entries.firstOrNull { it.key.contains("받으") || it.key.contains("수하") }?.value.orEmpty()
        val accepter = pdMap.entries.firstOrNull { it.key.contains("인수") }?.value.orEmpty()

        val tkTableHtml = Regex("""<table[^>]*class=["'][^"']*tkInfo[^"']*["'][^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)
            ?: Regex("""<table[^>]*class=["'][^"']*data[^"']*["'][^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1).orEmpty()
        val tbodyHtml = Regex("""<tbody[^>]*>([\s\S]*?)</tbody>""", RegexOption.IGNORE_CASE)
            .find(tkTableHtml)?.groupValues?.getOrNull(1) ?: tkTableHtml

        val events = mutableListOf<TrackingEvent>()
        for (rowHtml in extractTrs(tbodyHtml)) {
            if (rowHtml.contains("""class="empty"""") || rowHtml.contains("조회할 수 없습니다")) continue
            val cols = extractTdTextsPreserveBreaks(rowHtml).map { it.replace("\n", " ").replace(Regex("\\s+"), " ").trim() }
            if (cols.size < 4) continue
            val dispTime = cols[0]
            val location = cols[1]
            var status = cols[2]
            if (status == "배송완료 사진확인") status = "배송완료"
            val desc = cols[3]
            val driverName = cols.getOrNull(5).orEmpty()
            val driverPhone = cols.getOrNull(6).orEmpty()
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(
                timeIso = dispTime,
                displayTime = dispTime,
                statusName = status.ifBlank { "이동중" },
                phase = phase,
                carrierStageIndex = stageIdx,
                location = location,
                driverName = driverName,
                driverPhone = driverPhone,
                description = desc
            )
        }

        if (events.isEmpty() && itemName.isBlank() && senderName.isBlank()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = itemName,
            itemQuantity = qty,
            senderName = senderName,
            recipientName = recipientName,
            accepterName = accepter,
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "로젠택배 직접조회"
        )
    }

    // ==================== 6. GS Postbox & CU 편의점택배 ====================
    private fun queryCvsnet(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpGet("https://www.cvsnet.co.kr/invoice/tracking.do?invoice_no=${URLEncoder.encode(waybill, "UTF-8")}")
        return parseCvsnetHtml(carrier, waybill, reply.body)
    }

    private fun queryCupost(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpGet("https://www.cupost.co.kr/postbox/delivery/localResult.cupost?invoice_no=${URLEncoder.encode(waybill, "UTF-8")}")
        parseCvsnetHtml(carrier, waybill, reply.body)?.let { return it }
        val allReply = httpGet("https://www.cupost.co.kr/postbox/delivery/allResult.cupost?invoice_no=${URLEncoder.encode(waybill, "UTF-8")}")
        parseCvsnetHtml(carrier, waybill, allReply.body)?.let { return it }
        return null
    }

    fun parseCvsnetHtml(carrier: CarrierInfo, waybill: String, html: String): TrackingResult? {
        val match = Regex("""var\s+trackingInfo\s*=\s*(\{[\s\S]*?\});\s*\n""").find(html) ?: return null
        val json = runCatching { JSONObject(match.groupValues[1]) }.getOrNull() ?: return null
        val code = json.optInt("code", 200)
        if (code in listOf(100, 400, 404)) return null

        val goodsName = cleanValue(json.optString("goodsName"))
        val serviceName = cleanValue(json.optString("serviceName"))
        val senderObj = json.optJSONObject("sender")
        val receiverObj = json.optJSONObject("receiver")
        val details = json.optJSONArray("trackingDetails") ?: JSONArray()

        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until details.length()) {
            val row = details.optJSONObject(i) ?: continue
            val transTime = cleanValue(row.optString("transTime")).replace("T", " ").replace("-", ".").take(16)
            val transKind = cleanValue(row.optString("transKind")).ifBlank { "이동중" }
            val transWhere = cleanValue(row.optString("transWhere"))
            val transCode = cleanValue(row.optString("transCode"))
            val phase = when (transCode) {
                "C01" -> StandardDeliveryPhase.PREPARING
                "C015", "11" -> StandardDeliveryPhase.PICKED_UP
                "C02", "C03", "C04", "C07", "C08", "21", "41", "42" -> StandardDeliveryPhase.IN_TRANSIT
                "C09" -> StandardDeliveryPhase.ARRIVED_AT_BRANCH
                "C095", "C10", "82" -> StandardDeliveryPhase.OUT_FOR_DELIVERY
                "C11", "91" -> StandardDeliveryPhase.DELIVERED
                else -> KoreanCarrierCatalog.inferPhaseFromText(transKind)
            }
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, transKind, phase)
            events += TrackingEvent(
                timeIso = cleanValue(row.optString("transTime")),
                displayTime = transTime,
                statusName = transKind,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = transWhere,
                description = transKind
            )
        }
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = goodsName,
            senderName = cleanValue(senderObj?.optString("name")),
            senderPhone = cleanValue(senderObj?.optString("tel")),
            senderAddress = listOf(cleanValue(senderObj?.optString("baseAddress")), cleanValue(senderObj?.optString("detailAddress")))
                .filter { it.isNotBlank() }.joinToString(" "),
            recipientName = cleanValue(receiverObj?.optString("name")),
            recipientPhone = cleanValue(receiverObj?.optString("tel")),
            recipientAddress = listOf(cleanValue(receiverObj?.optString("baseAddress")), cleanValue(receiverObj?.optString("detailAddress")))
                .filter { it.isNotBlank() }.joinToString(" "),
            serviceType = serviceName,
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "${carrier.shortName} 직접조회"
        )
    }

    // ==================== 7. 쿠팡 로지스틱스(CLS) ====================
    private fun queryCoupangCls(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpGet("https://www.coupangls.com/web/modal/invoice/${URLEncoder.encode(waybill, "UTF-8")}")
        return parseCoupangClsHtml(carrier, waybill, reply.body)
    }

    fun parseCoupangClsHtml(carrier: CarrierInfo, waybill: String, html: String): TrackingResult? {
        if (html.contains("운송장 미등록") || html.contains("waybill is not registered")) return null
        val recipient = Regex("""class=["'][^"']*recipient[^"']*["'][^>]*>[\s\S]*?<div[^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.let(::stripTags)?.removeSuffix("님")?.trim().orEmpty()

        val tableHtml = Regex("""class=["'][^"']*tracking-detail[^"']*["'][^>]*>[\s\S]*?<table[^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1).orEmpty()
        val events = mutableListOf<TrackingEvent>()
        for (rowHtml in extractTrs(tableHtml)) {
            val cols = extractTdTextsPreserveBreaks(rowHtml).map { it.replace("\n", " ").replace(Regex("\\s+"), " ").trim() }
            if (cols.size < 3) continue
            val dispTime = cols[0].replace("-", ".").take(16)
            val loc = cols[1]
            val status = cols[2]
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(
                timeIso = cols[0],
                displayTime = dispTime,
                statusName = status,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = loc,
                description = "$status - $loc"
            )
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            recipientName = recipient,
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "쿠팡 CLS 직접조회"
        )
    }

    // ==================== 8. 경동택배 ====================
    private fun queryKyungdong(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val enc = URLEncoder.encode(waybill, "UTF-8")
        val reply = httpGet("https://kdexp.com/service/delivery/new/ajax_basic.do?barcode=$enc")
        val root = runCatching { JSONObject(reply.body) }.getOrNull()
        if (root != null && root.optString("result") == "suc") {
            val data = root.optJSONObject("data")
            val scanList = data?.optJSONArray("scanList") ?: JSONArray()
            val itemObj = data?.optJSONObject("item") ?: data?.optJSONObject("info")
            val itemName = cleanValue(itemObj?.optString("prodNm")).ifBlank { cleanValue(itemObj?.optString("itemNm")) }
            val sender = cleanValue(itemObj?.optString("sndrNm")).ifBlank { cleanValue(itemObj?.optString("sendNm")) }
            val receiver = cleanValue(itemObj?.optString("rcvrNm")).ifBlank { cleanValue(itemObj?.optString("recvNm")) }

            val events = mutableListOf<TrackingEvent>()
            for (i in 0 until scanList.length()) {
                val row = scanList.optJSONObject(i) ?: continue
                val dt = cleanValue(row.optString("scanDt")).removeSuffix(".0").replace("-", ".").take(16)
                val scanTypeNm = cleanValue(row.optString("scanTypeNm")).ifBlank { "이동중" }
                val loc = cleanValue(row.optString("strtPointNm"))
                val tel = cleanValue(row.optString("strtPointTelno"))
                val phase = KoreanCarrierCatalog.inferPhaseFromText(scanTypeNm)
                val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, scanTypeNm, phase)
                events += TrackingEvent(
                    timeIso = cleanValue(row.optString("scanDt")),
                    displayTime = dt,
                    statusName = scanTypeNm,
                    phase = phase,
                    carrierStageIndex = stageIdx,
                    location = loc,
                    branchPhone = tel,
                    description = "$scanTypeNm - $loc"
                )
            }
            if (events.isNotEmpty()) {
                val sorted = events.sortedByDescending { it.displayTime }
                val latest = sorted.first()
                return TrackingResult(
                    carrierId = carrier.id,
                    waybillNumber = waybill,
                    itemName = itemName,
                    senderName = sender,
                    recipientName = receiver,
                    currentStatusLabel = latest.statusName,
                    currentPhase = latest.phase,
                    currentCarrierStageIndex = latest.carrierStageIndex,
                    events = sorted,
                    syncSource = "경동택배 직접조회"
                )
            }
        }
        // Legacy endpoint fallback
        val legacy = httpGet("https://kdexp.com/service/delivery/ajax_basic.do?barcode=$enc")
        val legRoot = runCatching { JSONObject(legacy.body) }.getOrNull() ?: return null
        if (legRoot.optString("result") != "suc") return null
        val items = legRoot.optJSONArray("items") ?: return null
        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until items.length()) {
            val row = items.optJSONObject(i) ?: continue
            val stat = cleanValue(row.optString("stat")).ifBlank { "이동중" }
            val dt = cleanValue(row.optString("reg_date")).removeSuffix(".0").replace("-", ".").take(16)
            val loc = cleanValue(row.optString("location"))
            val tel = cleanValue(row.optString("tel"))
            val phase = KoreanCarrierCatalog.inferPhaseFromText(stat)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, stat, phase)
            events += TrackingEvent(
                timeIso = dt,
                displayTime = dt,
                statusName = stat,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = loc,
                branchPhone = tel,
                description = "$stat - $loc"
            )
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "경동택배 직접조회"
        )
    }

    // ==================== 9. 대신택배 ====================
    private fun queryDaesin(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpPost(
            "https://www.ds3211.co.kr/freight/internalFreightSearch.ht",
            "billno=${URLEncoder.encode(waybill, "UTF-8")}",
            responseCharset = Charset.forName("EUC-KR")
        )
        return parseDaesinHtml(carrier, waybill, reply.body)
    }

    fun parseDaesinHtml(carrier: CarrierInfo, waybill: String, html: String): TrackingResult? {
        if (html.contains("운송된 내역이 없습니다")) return null
        val tables = Regex("""<table[^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE).findAll(html).map { it.groupValues[1] }.toList()
        if (tables.size < 2) return null

        val infoMap = extractThTdMap(tables[0])
        val infoTds = extractTdTextsPreserveBreaks(tables[0]).map { it.replace("\n", " ").trim() }
        val sender = infoMap.entries.firstOrNull { it.key.contains("보내") }?.value ?: infoTds.getOrNull(0).orEmpty()
        val senderPhone = infoTds.getOrNull(1).orEmpty()
        val receiver = infoMap.entries.firstOrNull { it.key.contains("받으") }?.value ?: infoTds.getOrNull(2).orEmpty()
        val receiverPhone = infoTds.getOrNull(3).orEmpty()
        val itemName = infoMap.entries.firstOrNull { it.key.contains("품명") }?.value ?: infoTds.getOrNull(4).orEmpty()
        val qty = infoMap.entries.firstOrNull { it.key.contains("수량") }?.value ?: infoTds.getOrNull(5).orEmpty()

        val events = mutableListOf<TrackingEvent>()
        for (rowHtml in extractTrs(tables[1]).drop(1)) {
            val cols = extractTdTextsPreserveBreaks(rowHtml).map { it.replace("\n", " ").replace(Regex("\\s+"), " ").trim() }
            if (cols.size < 5) continue
            val stepLabel = cols[0]
            val branch = cols[1]
            val phone = cols[2]
            val enterTime = cols[3].replace("-", ".")
            val leaveTime = cols[4].replace("-", ".")
            val curState = cols.getOrNull(5).orEmpty()
            val chosenTime = leaveTime.ifBlank { enterTime }
            if (chosenTime.isBlank()) continue
            val statusName = when {
                curState == "배송완료" -> "배송완료"
                curState == "배달중" -> "배달중"
                stepLabel == "발송취급점" -> "접수"
                else -> "$stepLabel ${if (leaveTime.isNotBlank()) "출발" else "도착"}"
            }
            val phase = KoreanCarrierCatalog.inferPhaseFromText(statusName)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, statusName, phase)
            events += TrackingEvent(
                timeIso = chosenTime,
                displayTime = chosenTime,
                statusName = statusName,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = branch,
                branchPhone = phone,
                description = curState
            )
        }
        if (events.isEmpty() && sender.isBlank() && itemName.isBlank()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = itemName,
            itemQuantity = qty,
            senderName = sender,
            senderPhone = senderPhone,
            recipientName = receiver,
            recipientPhone = receiverPhone,
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "대신택배 직접조회"
        )
    }

    // ==================== 10. 천일택배 ====================
    private fun queryChunil(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpGet(
            "http://www.chunil.co.kr/HTrace/HTrace.jsp?transNo=${URLEncoder.encode(waybill, "UTF-8")}",
            responseCharset = Charset.forName("EUC-KR")
        )
        val tables = Regex("""<table[^>]*cellspacing=["']1["'][^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE)
            .findAll(reply.body).map { it.groupValues[1] }.toList()
        if (tables.size < 5) return null
        val senderTds = extractTdTextsPreserveBreaks(tables[0])
        val recvTds = extractTdTextsPreserveBreaks(tables[1])
        val itemTds = extractTdTextsPreserveBreaks(tables[2])
        val events = mutableListOf<TrackingEvent>()
        for (rowHtml in extractTrs(tables[4]).drop(1)) {
            val cols = extractTdTextsPreserveBreaks(rowHtml).map { it.replace("\n", " ").trim() }
            if (cols.size < 4) continue
            val dt = cols[0].replace("-", ".").take(16)
            val loc = cols[1]
            val tel = cols[2]
            val status = cols[3]
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(
                timeIso = cols[0],
                displayTime = dt,
                statusName = status,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = loc,
                branchPhone = tel
            )
        }
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = itemTds.getOrNull(1).orEmpty().trim(),
            itemQuantity = itemTds.getOrNull(3).orEmpty().trim(),
            senderName = senderTds.getOrNull(1).orEmpty().trim(),
            recipientName = recvTds.getOrNull(1).orEmpty().trim(),
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "천일택배 직접조회"
        )
    }

    // ==================== 11. 일양로지스 ====================
    private fun queryIlyang(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpPost(
            "https://www.ilyanglogis.co.kr/include/getAPIResult.asp",
            "req_type=TRACKING&tracking_type=0&blNum=${URLEncoder.encode(waybill, "UTF-8")}"
        )
        val root = runCatching { JSONObject(reply.body) }.getOrNull() ?: return null
        val api = root.optJSONObject("resultAPI") ?: return null
        if (api.optJSONObject("head")?.optString("returnCode") != "R0") return null
        val body = api.optJSONObject("body") ?: return null
        val trackingList = body.optJSONArray("resultList") ?: return null
        val first = trackingList.optJSONObject(0) ?: return null
        val details = first.optJSONArray("tracking") ?: JSONArray()
        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until details.length()) {
            val row = details.optJSONObject(i) ?: continue
            val status = cleanValue(row.optString("statusName")).ifBlank { cleanValue(row.optString("status")) }
            val dt = cleanValue(row.optString("dateTime")).replace("-", ".").take(16)
            val loc = cleanValue(row.optString("officeName"))
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(
                timeIso = dt,
                displayTime = dt,
                statusName = status,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = loc
            )
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            senderName = cleanValue(first.optString("sendName")),
            recipientName = cleanValue(first.optString("receiveName")),
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "일양로지스 직접조회"
        )
    }

    // ==================== 12. 합동 · 건영 · SLX · 홈픽 · 오늘의픽업 · 용마 · 호남 · 우리 · 굿투럭 · 판토스 ====================
    private fun queryHapdong(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpGet("https://hdexp.co.kr/deliverySearch2.hd?barcode=${URLEncoder.encode(waybill, "UTF-8")}")
        val root = runCatching { JSONObject(reply.body) }.getOrNull() ?: return null
        val info = root.optJSONObject("info")
        val items = root.optJSONArray("items") ?: return null
        if (items.length() == 0) return null
        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until items.length()) {
            val row = items.optJSONObject(i) ?: continue
            val status = cleanValue(row.optString("stat")).ifBlank { "이동중" }
            val dt = cleanValue(row.optString("reg_date")).removeSuffix(".0").replace("-", ".").take(16)
            val loc = cleanValue(row.optString("location"))
            val tel = cleanValue(row.optString("tel"))
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(dt, dt, status, phase, stageIdx, loc, branchPhone = tel)
        }
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull() ?: return null
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = cleanValue(info?.optString("prod")),
            senderName = cleanValue(info?.optString("send_name")),
            recipientName = cleanValue(info?.optString("re_name")),
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "합동택배 직접조회"
        )
    }

    private fun queryKunyoung(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpGet(
            "https://www.kunyoung.com/goods/goods_02.php?mulno=${URLEncoder.encode(waybill, "UTF-8")}",
            responseCharset = Charset.forName("EUC-KR")
        )
        val tables = Regex("""<table[^>]*width=["']717["'][^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE)
            .findAll(reply.body).map { it.groupValues[1] }.toList()
        if (tables.size < 4) return null
        val events = mutableListOf<TrackingEvent>()
        for (rowHtml in extractTrs(tables[3])) {
            val cols = extractTdTextsPreserveBreaks(rowHtml).map { it.replace("\n", " ").trim() }
            if (cols.size < 4) continue
            val dt = cols[0].replace("-", ".").take(16)
            val status = cols[1]
            val loc = cols[2]
            val tel = cols[3]
            if (dt.isBlank() || status.isBlank()) continue
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(dt, dt, status, phase, stageIdx, loc, branchPhone = tel)
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "건영택배 직접조회"
        )
    }

    private fun querySlx(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpGet("https://net.slx.co.kr/info/tracking.jsp?iv_no=${URLEncoder.encode(waybill, "UTF-8")}")
        val tables = Regex("""<table[^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE)
            .findAll(reply.body).map { it.groupValues[1] }.toList()
        if (tables.size < 3) return null
        val events = mutableListOf<TrackingEvent>()
        for (rowHtml in extractTrs(tables[2]).drop(1)) {
            val cols = extractTdTextsPreserveBreaks(rowHtml).map { it.replace("\n", " ").trim() }
            if (cols.size < 4) continue
            val dt = cols[0].replace("-", ".").take(16)
            val loc = cols[1]
            val status = cols[2]
            val desc = cols[3]
            if (status.isBlank()) continue
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(dt, dt, status, phase, stageIdx, loc, description = desc)
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "SLX택배 직접조회"
        )
    }

    private fun queryHomepick(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val inquiry = httpGet("https://www.homepick.com/user/api/delivery/universalInquiry?keyword=${URLEncoder.encode(waybill, "UTF-8")}")
        val root = runCatching { JSONObject(inquiry.body) }.getOrNull() ?: return null
        if (!root.optBoolean("success", false)) return null
        val orderBoxId = cleanValue(root.optString("data"))
        if (orderBoxId.isBlank()) return null
        val detail = httpGet("https://www.homepick.com/user/api/delivery/$orderBoxId")
        val dRoot = runCatching { JSONObject(detail.body) }.getOrNull() ?: return null
        val delivery = dRoot.optJSONObject("data")?.optJSONObject("delivery") ?: return null
        val history = delivery.optJSONArray("orderStatusHistoryList") ?: return null
        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until history.length()) {
            val row = history.optJSONObject(i) ?: continue
            val status = cleanValue(row.optString("statusName")).ifBlank { cleanValue(row.optString("status")) }
            val dt = cleanValue(row.optString("createdAt")).replace("T", " ").replace("-", ".").take(16)
            val desc = cleanValue(row.optString("description"))
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(dt, dt, status, phase, stageIdx, location = "", description = desc)
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = cleanValue(delivery.optString("goodsName")),
            senderName = cleanValue(delivery.optString("senderName")),
            recipientName = cleanValue(delivery.optString("receiverName")),
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "홈픽 직접조회"
        )
    }

    private fun queryTodayPickup(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpGet("https://mall.todaypickup.com/front/delivery/list/${URLEncoder.encode(waybill, "UTF-8")}")
        val tables = Regex("""<table[^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE).findAll(reply.body).map { it.groupValues[1] }.toList()
        if (tables.size < 3) return null
        val events = mutableListOf<TrackingEvent>()
        for (rowHtml in extractTrs(tables[2])) {
            val cols = extractTdTextsPreserveBreaks(rowHtml).map { it.replace("\n", " ").trim() }
            if (cols.size < 3) continue
            val dt = cols[0].replace("-", ".").take(16)
            val status = cols[1]
            val loc = cols[2]
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(dt, dt, status, phase, stageIdx, loc)
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "오늘의픽업 직접조회"
        )
    }

    private fun queryYongma(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val enc = URLEncoder.encode(waybill, "UTF-8")
        val init = httpGet("https://eis.yongmalogis.co.kr/dm/dmtrc060/selectDmTrc060?ymd=&conscd=&seq=&ordno=$enc")
        val initJson = runCatching { JSONObject(init.body) }.getOrNull() ?: return null
        val ymd = initJson.optString("ymd", "")
        val code = initJson.optString("code", "")
        val seq = initJson.optString("seqnum", "")
        if (ymd.isBlank()) return null
        val stReply = httpGet("https://eis.yongmalogis.co.kr/dm/dmtrc060/selectDmTrc060Status?ymd=$ymd&conscd=$code&seq=$seq")
        val arr = runCatching { JSONArray(stReply.body) }.getOrNull() ?: return null
        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            val status = cleanValue(row.optString("statNm")).ifBlank { "이동중" }
            val dt = cleanValue(row.optString("regDt")).replace("-", ".").take(16)
            val loc = cleanValue(row.optString("branNm"))
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(dt, dt, status, phase, stageIdx, loc)
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "용마로지스 직접조회"
        )
    }

    private fun queryHonam(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpPost("http://inkoin.com/tracking_number.php", "SLIP_BARCD=${URLEncoder.encode(waybill, "UTF-8")}")
        val root = runCatching { JSONObject(reply.body) }.getOrNull() ?: return null
        if (root.optInt("ODS0_TOTAL", 0) < 1) return null
        val ods = root.optJSONArray("ODS0")?.optJSONObject(0) ?: return null
        val dtl = ods.optJSONArray("TRACKING_DTL") ?: return null
        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until dtl.length()) {
            val row = dtl.optJSONObject(i) ?: continue
            val status = cleanValue(row.optString("SCAN_NM"))
            val dt = cleanValue(row.optString("SCAN_DT")).replace("-", ".").take(16)
            val loc = cleanValue(row.optString("BRAN_NM"))
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(dt, dt, status, phase, stageIdx, loc)
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "호남택배 직접조회"
        )
    }

    private fun queryCway(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val logReply = httpPost("http://cway.hagoto.com/where/hbl/logList", "hblNo=${URLEncoder.encode(waybill, "UTF-8")}&pageNum=NaN&isAsc=asc")
        val root = runCatching { JSONObject(logReply.body) }.getOrNull() ?: return null
        val rows = root.optJSONArray("rows") ?: return null
        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            val status = cleanValue(row.optString("stateName"))
            val dt = cleanValue(row.optString("createTime")).replace("-", ".").take(16)
            val loc = cleanValue(row.optString("branchName"))
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(dt, dt, status, phase, stageIdx, loc)
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "우리택배 직접조회"
        )
    }

    private fun queryGoodsToLuck(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val reply = httpPost(
            "http://www.goodstoluck.co.kr/tracking/tracking_proc.php",
            "RetrieveFlag=SEARCH&Txt_word=${URLEncoder.encode(waybill, "UTF-8")}"
        )
        if (reply.body.contains("result_none_tb")) return null
        val tables = Regex("""<table[^>]*>([\s\S]*?)</table>""", RegexOption.IGNORE_CASE).findAll(reply.body).map { it.groupValues[1] }.toList()
        if (tables.size < 2) return null
        val events = mutableListOf<TrackingEvent>()
        for (rowHtml in extractTrs(tables[1]).drop(1)) {
            val cols = extractTdTextsPreserveBreaks(rowHtml).map { it.replace("\n", " ").trim() }
            if (cols.size < 4) continue
            val dt = cols[0].replace("-", ".").take(16)
            val loc = cols[1]
            val status = cols[2]
            val desc = cols[3]
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(dt, dt, status, phase, stageIdx, loc, description = desc)
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "굿투럭 직접조회"
        )
    }

    private fun queryPantos(carrier: CarrierInfo, waybill: String): TrackingResult? {
        val listReply = httpPostJson(
            "https://www.epantos.com/eCommerce/action/portal.TrackingPopup.retreiveTrackingList",
            JSONObject().put("quickNo", waybill).put("locale", "ko").toString()
        )
        val listRoot = runCatching { JSONObject(listReply.body) }.getOrNull() ?: return null
        val first = listRoot.optJSONObject("body")?.optJSONArray("list")?.optJSONObject(0) ?: return null
        val dtlReply = httpPostJson(
            "https://www.epantos.com/eCommerce/action/portal.TrackingPopup.retreiveTrackingListDtl",
            JSONObject()
                .put("hblNo", first.optString("hblNo"))
                .put("mblNo", first.optString("mblNo"))
                .put("locale", "ko")
                .put("expsBizTypeCd", first.optString("expsBizTypeCd"))
                .toString()
        )
        val dtlRoot = runCatching { JSONObject(dtlReply.body) }.getOrNull() ?: return null
        val arr = dtlRoot.optJSONObject("body")?.optJSONArray("list") ?: return null
        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            val status = cleanValue(row.optString("statusNm"))
            val dt = cleanValue(row.optString("statusDtm")).replace("-", ".").take(16)
            val loc = cleanValue(row.optString("locNm"))
            val phase = KoreanCarrierCatalog.inferPhaseFromText(status)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, status, phase)
            events += TrackingEvent(dt, dt, status, phase, stageIdx, loc)
        }
        if (events.isEmpty()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.first()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            currentStatusLabel = latest.statusName,
            currentPhase = latest.phase,
            currentCarrierStageIndex = latest.carrierStageIndex,
            events = sorted,
            syncSource = "LX판토스 직접조회"
        )
    }

    // ==================== 13. Universal tracker.delivery REST Fallback ====================
    fun queryTrackerDeliveryRest(
        carrier: CarrierInfo,
        waybill: String,
        baseEndpoint: String
    ): TrackingResult? {
        val trackerId = carrier.trackerDeliveryId ?: return null
        val cleanBase = baseEndpoint.trimEnd('/').ifBlank { "https://apis.tracker.delivery" }
        val url = "$cleanBase/carriers/${URLEncoder.encode(trackerId, "UTF-8")}/tracks/${URLEncoder.encode(waybill, "UTF-8")}"
        val reply = httpGet(url)
        if (reply.statusCode !in 200..299) return null
        val root = runCatching { JSONObject(reply.body) }.getOrNull() ?: return null
        val fromName = cleanValue(root.optJSONObject("from")?.optString("name"))
        val toName = cleanValue(root.optJSONObject("to")?.optString("name"))
        val progresses = root.optJSONArray("progresses") ?: JSONArray()
        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until progresses.length()) {
            val p = progresses.optJSONObject(i) ?: continue
            val statusText = cleanValue(p.optJSONObject("status")?.optString("text")).ifBlank { "이동중" }
            val timeRaw = cleanValue(p.optString("time"))
            val dispTime = timeRaw.replace("T", " ").replace("-", ".").take(16)
            val loc = cleanValue(p.optJSONObject("location")?.optString("name"))
            val desc = cleanValue(p.optString("description"))
            val phase = KoreanCarrierCatalog.inferPhaseFromText(statusText)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, statusText, phase)
            events += TrackingEvent(
                timeIso = timeRaw,
                displayTime = dispTime,
                statusName = statusText,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = loc,
                description = desc
            )
        }
        val stateText = cleanValue(root.optJSONObject("state")?.optString("text"))
        if (events.isEmpty() && stateText.isBlank()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            senderName = fromName,
            recipientName = toName,
            currentStatusLabel = latest?.statusName ?: stateText.ifBlank { "조회 완료" },
            currentPhase = latest?.phase ?: KoreanCarrierCatalog.inferPhaseFromText(stateText),
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "Delivery Tracker 통합조회"
        )
    }

    // ==================== 14. Optional SweetTracker API ====================
    private fun querySweetTracker(
        carrier: CarrierInfo,
        waybill: String,
        apiKey: String
    ): TrackingResult? {
        val url = "https://info.sweettracker.co.kr/api/v1/trackingInfo" +
            "?t_key=${URLEncoder.encode(apiKey, "UTF-8")}" +
            "&t_code=${URLEncoder.encode(carrier.sweetTrackerCode, "UTF-8")}" +
            "&t_invoice=${URLEncoder.encode(waybill, "UTF-8")}"
        val reply = httpGet(url)
        val root = runCatching { JSONObject(reply.body) }.getOrNull() ?: return null
        if (root.optBoolean("status", true) == false && root.optString("code").isNotBlank()) return null

        val itemName = cleanValue(root.optString("itemName"))
        val senderName = cleanValue(root.optString("senderName"))
        val receiverName = cleanValue(root.optString("receiverName"))
        val details = root.optJSONArray("trackingDetails") ?: JSONArray()
        val events = mutableListOf<TrackingEvent>()
        for (i in 0 until details.length()) {
            val row = details.optJSONObject(i) ?: continue
            val timeString = cleanValue(row.optString("timeString")).replace("-", ".").take(16)
            val kind = cleanValue(row.optString("kind")).ifBlank { "이동중" }
            val where = cleanValue(row.optString("where"))
            val telno = cleanValue(row.optString("telno"))
            val telno2 = cleanValue(row.optString("telno2"))
            val manName = cleanValue(row.optString("manName"))
            val phase = KoreanCarrierCatalog.inferPhaseFromText(kind)
            val stageIdx = KoreanCarrierCatalog.resolveStageForStatus(carrier, kind, phase)
            events += TrackingEvent(
                timeIso = timeString,
                displayTime = timeString,
                statusName = kind,
                phase = phase,
                carrierStageIndex = stageIdx,
                location = where,
                branchPhone = telno,
                driverName = manName,
                driverPhone = telno2,
                description = kind
            )
        }
        if (events.isEmpty() && itemName.isBlank() && senderName.isBlank()) return null
        val sorted = events.sortedByDescending { it.displayTime }
        val latest = sorted.firstOrNull()
        return TrackingResult(
            carrierId = carrier.id,
            waybillNumber = waybill,
            itemName = itemName,
            senderName = senderName,
            recipientName = receiverName,
            accepterName = cleanValue(root.optString("receiverAddr")),
            currentStatusLabel = latest?.statusName ?: "조회 완료",
            currentPhase = latest?.phase ?: StandardDeliveryPhase.PREPARING,
            currentCarrierStageIndex = latest?.carrierStageIndex ?: 0,
            events = sorted,
            syncSource = "스마트택배 API"
        )
    }

    // ==================== HTTP & HTML/Text Helpers ====================
    private fun httpGet(
        urlStr: String,
        cookies: String = "",
        responseCharset: Charset = Charsets.UTF_8
    ): HttpReply {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 9000
            readTimeout = 12000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "text/html,application/json,*/*")
            if (cookies.isNotBlank()) setRequestProperty("Cookie", cookies)
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(responseCharset)?.use { it.readText() }.orEmpty()
            val setCookies = conn.headerFields["Set-Cookie"]
                ?.joinToString("; ") { it.substringBefore(";") }
                .orEmpty()
            HttpReply(code, body, setCookies)
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPost(
        urlStr: String,
        formBody: String,
        cookies: String = "",
        responseCharset: Charset = Charsets.UTF_8
    ): HttpReply {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 9000
            readTimeout = 12000
            doOutput = true
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("Accept", "application/json,text/html,*/*")
            if (cookies.isNotBlank()) setRequestProperty("Cookie", cookies)
        }
        return try {
            conn.outputStream.use { it.write(formBody.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(responseCharset)?.use { it.readText() }.orEmpty()
            HttpReply(code, body)
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPostJson(urlStr: String, jsonBody: String): HttpReply {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 9000
            readTimeout = 12000
            doOutput = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        return try {
            conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            HttpReply(code, body)
        } finally {
            conn.disconnect()
        }
    }

    private fun extractTrs(html: String): List<String> =
        Regex("""<tr\b[^>]*>([\s\S]*?)</tr>""", RegexOption.IGNORE_CASE)
            .findAll(html).map { it.groupValues[1] }.toList()

    private fun extractTdTextsPreserveBreaks(html: String): List<String> =
        Regex("""<td\b[^>]*>([\s\S]*?)</td>""", RegexOption.IGNORE_CASE)
            .findAll(html)
            .map { match ->
                val withNewlines = match.groupValues[1]
                    .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
                stripTags(withNewlines)
            }
            .toList()

    private fun extractThTdMap(html: String): Map<String, String> {
        val map = linkedMapOf<String, String>()
        val pairRegex = Regex("""<th\b[^>]*>([\s\S]*?)</th>\s*<td\b[^>]*>([\s\S]*?)</td>""", RegexOption.IGNORE_CASE)
        pairRegex.findAll(html).forEach { m ->
            val k = stripTags(m.groupValues[1]).replace(Regex("\\s+"), " ").trim()
            val v = stripTags(m.groupValues[2]).replace(Regex("\\s+"), " ").trim()
            if (k.isNotBlank() && v.isNotBlank()) map[k] = v
        }
        return map
    }

    private fun stripTags(raw: String): String {
        val noComments = raw.replace(Regex("""<!--[\s\S]*?-->"""), "")
        val noScripts = noComments.replace(Regex("""<script\b[^>]*>[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")
        val noTags = noScripts.replace(Regex("""<[^>]+>"""), " ")
        return decodeHtmlEntities(noTags)
            .lines()
            .joinToString("\n") { it.replace(Regex("""[ \t]+"""), " ").trim() }
            .trim()
    }

    private fun decodeHtmlEntities(text: String): String = text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

    private fun cleanValue(raw: String?): String {
        val t = raw?.trim().orEmpty()
        return if (t.equals("null", ignoreCase = true) || t == "-") "" else t
    }

    private fun formatDateTimePair(dateRaw: String, timeRaw: String): String {
        val dDigits = dateRaw.filter(Char::isDigit)
        val formattedDate = if (dDigits.length >= 8) {
            "${dDigits.substring(0, 4)}.${dDigits.substring(4, 6)}.${dDigits.substring(6, 8)}"
        } else {
            dateRaw.replace("-", ".")
        }
        val tDigits = timeRaw.filter(Char::isDigit)
        val formattedTime = when {
            tDigits.length >= 4 -> "${tDigits.substring(0, 2)}:${tDigits.substring(2, 4)}"
            timeRaw.isNotBlank() -> timeRaw
            else -> ""
        }
        return listOf(formattedDate, formattedTime).filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun extractDriverInfo(text: String): Pair<String, String> {
        val phone = Regex("""0\d{1,2}-\d{3,4}-\d{4}""").find(text)?.value.orEmpty()
        val name = Regex("""(?:배송기사|담당자|기사님|집배원|사원)?\s*([가-힣]{2,4})\s*\(?0\d{1,2}-\d{3,4}-\d{4}""")
            .find(text)?.groupValues?.getOrNull(1).orEmpty()
        return name to phone
    }
}
