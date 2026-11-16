package com.x2tora.torabox.data

import android.content.Context
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.model.AppSettings
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.ToraBoxDataState
import com.x2tora.torabox.model.TrackingEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class ToraBoxRepository(context: Context) {

    private val dataFile = File(context.filesDir, "torabox_data.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): ToraBoxDataState {
        if (!dataFile.exists()) return ToraBoxDataState()
        return runCatching {
            val raw = dataFile.readText(Charsets.UTF_8)
            json.decodeFromString<ToraBoxDataState>(raw)
        }.getOrDefault(ToraBoxDataState())
    }

    fun save(state: ToraBoxDataState) {
        runCatching {
            val tmp = File(dataFile.parentFile, "torabox_data.json.tmp")
            tmp.writeText(json.encodeToString(state), Charsets.UTF_8)
            if (!tmp.renameTo(dataFile)) {
                dataFile.writeText(json.encodeToString(state), Charsets.UTF_8)
                tmp.delete()
            }
        }
    }

    fun exportJson(state: ToraBoxDataState): String = json.encodeToString(state)

    fun parseBackupJson(raw: String): Result<ToraBoxDataState> = runCatching {
        val cleaned = raw.trim()
        require(cleaned.startsWith("{")) { "올바른 JSON 형식이 아닙니다." }
        json.decodeFromString<ToraBoxDataState>(cleaned)
    }

    fun createResetState(keepSettings: AppSettings): ToraBoxDataState =
        ToraBoxDataState(settings = keepSettings)

    fun createSampleShipments(nowMs: Long = System.currentTimeMillis()): List<ShipmentRecord> {
        val cj = KoreanCarrierCatalog.findById("kr.cjlogistics")!!
        val epost = KoreanCarrierCatalog.findById("kr.epost")!!
        val gs = KoreanCarrierCatalog.findById("kr.cvsnet")!!

        return listOf(
            ShipmentRecord(
                id = UUID.randomUUID().toString(),
                waybillNumber = "566820864724",
                carrierId = cj.id,
                itemName = "루헨스 정수기 필터 외 1건",
                itemQuantity = "1",
                senderName = "이*호 (루헨스공식스토어)",
                recipientName = "최*우",
                serviceType = "일반택배",
                currentStatusLabel = "배달출발",
                currentPhase = StandardDeliveryPhase.OUT_FOR_DELIVERY,
                currentCarrierStageIndex = 4,
                isPinned = true,
                iconKey = "box",
                colorHex = cj.accentColorHex,
                createdAtEpochMs = nowMs - 86_400_000L,
                lastUpdatedEpochMs = nowMs,
                lastSyncSource = "샘플 데이터",
                events = listOf(
                    TrackingEvent(
                        timeIso = "2026-11-17 08:15",
                        displayTime = "2026.11.17 08:15",
                        statusName = "배달출발",
                        phase = StandardDeliveryPhase.OUT_FOR_DELIVERY,
                        carrierStageIndex = 4,
                        location = "서울강남대리점",
                        driverName = "김배송",
                        driverPhone = "010-1234-5678",
                        description = "고객님께 12시~14시 사이 배송 예정입니다."
                    ),
                    TrackingEvent(
                        timeIso = "2026-11-17 05:22",
                        displayTime = "2026.11.17 05:22",
                        statusName = "간선하차",
                        phase = StandardDeliveryPhase.ARRIVED_AT_BRANCH,
                        carrierStageIndex = 3,
                        location = "강남Sub",
                        description = "배달 대리점에 도착했습니다."
                    ),
                    TrackingEvent(
                        timeIso = "2026-11-16 22:10",
                        displayTime = "2026.11.16 22:10",
                        statusName = "간선상차",
                        phase = StandardDeliveryPhase.IN_TRANSIT,
                        carrierStageIndex = 2,
                        location = "곤지암Hub",
                        description = "허브 터미널에서 이동중입니다."
                    ),
                    TrackingEvent(
                        timeIso = "2026-11-16 17:40",
                        displayTime = "2026.11.16 17:40",
                        statusName = "집화처리",
                        phase = StandardDeliveryPhase.PICKED_UP,
                        carrierStageIndex = 1,
                        location = "경기용인집배점",
                        description = "보내는 분으로부터 상품을 인수받았습니다."
                    )
                )
            ),
            ShipmentRecord(
                id = UUID.randomUUID().toString(),
                waybillNumber = "6091234567890",
                carrierId = epost.id,
                itemName = "기계식 키보드 저소음 적축",
                itemQuantity = "1",
                senderName = "주식회사 토라기어",
                recipientName = "김*라",
                serviceType = "우체국소포",
                currentStatusLabel = "발송",
                currentPhase = StandardDeliveryPhase.IN_TRANSIT,
                currentCarrierStageIndex = 2,
                iconKey = "mail",
                colorHex = epost.accentColorHex,
                createdAtEpochMs = nowMs - 43_200_000L,
                lastUpdatedEpochMs = nowMs,
                lastSyncSource = "샘플 데이터",
                events = listOf(
                    TrackingEvent(
                        timeIso = "2026-11-17 02:35",
                        displayTime = "2026.11.17 02:35",
                        statusName = "발송",
                        phase = StandardDeliveryPhase.IN_TRANSIT,
                        carrierStageIndex = 2,
                        location = "대전우편집중국",
                        description = "발송 - 대전우편집중국"
                    ),
                    TrackingEvent(
                        timeIso = "2026-11-16 16:08",
                        displayTime = "2026.11.16 16:08",
                        statusName = "접수",
                        phase = StandardDeliveryPhase.PREPARING,
                        carrierStageIndex = 0,
                        location = "성남분당우체국",
                        description = "창구 소포 접수"
                    )
                )
            ),
            ShipmentRecord(
                id = UUID.randomUUID().toString(),
                waybillNumber = "390481920144",
                carrierId = gs.id,
                itemName = "잡화/서적 (중고거래)",
                itemQuantity = "1",
                senderName = "박*민",
                recipientName = "최*우",
                serviceType = "반값택배",
                accepterName = "GS25 역삼센트럴점 픽업완료",
                currentStatusLabel = "고객전달",
                currentPhase = StandardDeliveryPhase.DELIVERED,
                currentCarrierStageIndex = 5,
                iconKey = "store",
                colorHex = gs.accentColorHex,
                createdAtEpochMs = nowMs - 259_200_000L,
                lastUpdatedEpochMs = nowMs,
                lastSyncSource = "샘플 데이터",
                events = listOf(
                    TrackingEvent(
                        timeIso = "2026-11-15 19:40",
                        displayTime = "2026.11.15 19:40",
                        statusName = "고객전달",
                        phase = StandardDeliveryPhase.DELIVERED,
                        carrierStageIndex = 5,
                        location = "GS25 역삼센트럴점",
                        description = "고객전달 하였습니다."
                    ),
                    TrackingEvent(
                        timeIso = "2026-11-15 11:20",
                        displayTime = "2026.11.15 11:20",
                        statusName = "점포도착",
                        phase = StandardDeliveryPhase.OUT_FOR_DELIVERY,
                        carrierStageIndex = 4,
                        location = "GS25 역삼센트럴점",
                        description = "도착 점포에 보관중입니다."
                    )
                )
            )
        )
    }
}
