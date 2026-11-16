package com.x2tora.torabox.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeOption(val label: String) {
    SYSTEM("시스템 설정"),
    LIGHT("라이트"),
    DARK("다크")
}

@Serializable
enum class ThemeColorStyle(val label: String) {
    DEFAULT("기본 블루"),
    MATERIAL_YOU("Material You (다이나믹)"),
    CUSTOM("직접 색상 지정")
}

@Serializable
enum class NavBarStyle(val label: String) {
    MIUIX("기본 바 (Miuix)"),
    LIQUID_GLASS("리퀴드 글래스 바")
}

@Serializable
enum class CarrierCategory(val label: String) {
    MAJOR("주요 택배사"),
    CONVENIENCE_TODAY("편의점·당일·새벽"),
    CARGO_B2B("화물·중량·특수"),
    INTERNATIONAL("국제·해외특송")
}

@Serializable
enum class StandardDeliveryPhase(val stepIndex: Int, val label: String) {
    PREPARING(0, "접수·상품준비"),
    PICKED_UP(1, "집화·발송"),
    IN_TRANSIT(2, "이동중"),
    ARRIVED_AT_BRANCH(3, "배달지점도착"),
    OUT_FOR_DELIVERY(4, "배송출발"),
    DELIVERED(5, "배송완료"),
    EXCEPTION(6, "미배달·예외")
}

@Serializable
data class CarrierStageStep(
    val index: Int,
    val stageName: String,
    val description: String,
    val mappedPhase: StandardDeliveryPhase
)

@Serializable
data class CarrierInfo(
    val id: String,
    val sweetTrackerCode: String,
    val trackerDeliveryId: String? = null,
    val nameKo: String,
    val shortName: String,
    val category: CarrierCategory,
    val phone: String = "",
    val trackingUrlTemplate: String = "",
    val accentColorHex: Long = 0xFF3482FF,
    val iconKey: String = "box",
    val waybillLengths: List<Int> = emptyList(),
    val waybillPatternHint: String = "",
    val supportsDirectItemName: Boolean = false,
    val supportsDirectSender: Boolean = false,
    val supportsDirectQuery: Boolean = false,
    val stagePipeline: List<CarrierStageStep> = emptyList()
)

@Serializable
data class TrackingEvent(
    val timeIso: String = "",
    val displayTime: String = "",
    val statusName: String = "",
    val phase: StandardDeliveryPhase = StandardDeliveryPhase.IN_TRANSIT,
    val carrierStageIndex: Int = 0,
    val location: String = "",
    val branchPhone: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val description: String = ""
)

@Serializable
data class ShipmentRecord(
    val id: String,
    val waybillNumber: String,
    val carrierId: String,
    val customTitle: String = "",
    val itemName: String = "",
    val itemQuantity: String = "",
    val senderName: String = "",
    val senderPhone: String = "",
    val senderAddress: String = "",
    val recipientName: String = "",
    val recipientPhone: String = "",
    val recipientAddress: String = "",
    val accepterName: String = "",
    val serviceType: String = "",
    val originWaybill: String = "",
    val returnWaybill: String = "",
    val currentStatusLabel: String = "조회 대기",
    val currentPhase: StandardDeliveryPhase = StandardDeliveryPhase.PREPARING,
    val currentCarrierStageIndex: Int = 0,
    val events: List<TrackingEvent> = emptyList(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val iconKey: String = "box",
    val colorHex: Long = 0xFF3482FF,
    val memo: String = "",
    val createdAtEpochMs: Long = 0L,
    val lastUpdatedEpochMs: Long = 0L,
    val lastSyncSource: String = ""
) {
    fun displayTitle(carrierShortName: String): String =
        customTitle.trim().ifBlank {
            itemName.trim().ifBlank { "$carrierShortName 배송" }
        }
}

@Serializable
data class AppSettings(
    val themeOption: ThemeOption = ThemeOption.SYSTEM,
    val themeColorStyle: ThemeColorStyle = ThemeColorStyle.DEFAULT,
    val customKeyColorHex: Long? = null,
    val navBarStyle: NavBarStyle = NavBarStyle.MIUIX,
    val wideDualLayoutEnabled: Boolean = false,
    val autoRefreshOnLaunch: Boolean = true,
    val autoDetectClipboardWaybill: Boolean = true,
    val sweetTrackerApiKey: String = "",
    val customTrackerEndpoint: String = "https://apis.tracker.delivery"
) {
    fun resolvedColorStyle(): ThemeColorStyle = themeColorStyle
    fun resolvedNavBarStyle(): NavBarStyle = navBarStyle
}

@Serializable
data class ToraBoxDataState(
    val shipments: List<ShipmentRecord> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val settings: AppSettings = AppSettings()
)

data class CarrierRecommendation(
    val carrier: CarrierInfo,
    val confidencePercent: Int,
    val reasonBadges: List<String>,
    val isChecksumVerified: Boolean = false
)

data class ExtractedWaybillNotice(
    val waybillNumber: String,
    val carrierHintId: String? = null,
    val itemNameHint: String = "",
    val senderNameHint: String = ""
)

data class TrackingResult(
    val carrierId: String,
    val waybillNumber: String,
    val itemName: String = "",
    val itemQuantity: String = "",
    val senderName: String = "",
    val senderPhone: String = "",
    val senderAddress: String = "",
    val recipientName: String = "",
    val recipientPhone: String = "",
    val recipientAddress: String = "",
    val accepterName: String = "",
    val serviceType: String = "",
    val originWaybill: String = "",
    val returnWaybill: String = "",
    val currentStatusLabel: String = "",
    val currentPhase: StandardDeliveryPhase = StandardDeliveryPhase.PREPARING,
    val currentCarrierStageIndex: Int = 0,
    val events: List<TrackingEvent> = emptyList(),
    val syncSource: String = ""
)

sealed interface ShipmentFilter {
    data object ALL : ShipmentFilter
    data object IN_TRANSIT : ShipmentFilter
    data object OUT_FOR_DELIVERY : ShipmentFilter
    data object DELIVERED : ShipmentFilter
}
