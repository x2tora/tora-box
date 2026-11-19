package com.x2tora.torabox.domain

import com.x2tora.torabox.model.ExtractedWaybillNotice

data class ParsedNotificationCapture(
    val notice: ExtractedWaybillNotice,
    val sourceAppLabel: String,
    val confidenceScore: Int
)

/**
 * Context-aware parser for incoming notifications from KakaoTalk, SMS, and shopping apps.
 * Filters out OTP/auth codes and marketing noise via parcel-intent keyword scoring.
 */
object NotificationMessageParser {

    private val deliveryKeywords = listOf(
        "운송장", "송장", "등기", "택배", "배송", "발송", "출고", "집하",
        "배달", "통관", "도착예정", "로켓배송", "오늘도착", "수령"
    )

    private val noiseKeywords = listOf(
        "인증번호", "보안코드", "본인확인", "OTP", "결제승인", "카드승인",
        "잔액", "입금", "출금", "이체", "광고", "할인쿠폰"
    )

    private val packageLabels = mapOf(
        "com.kakao.talk" to ("카카오톡" to null),
        "com.google.android.apps.messaging" to ("메시지" to null),
        "com.samsung.android.messaging" to ("삼성 메시지" to null),
        "com.coupang.mobile" to ("쿠팡" to "kr.coupangls"),
        "com.nhn.android.search" to ("네이버" to null),
        "com.musinsa.store" to ("무신사" to null),
        "com.alibaba.aliexpresshd" to ("알리익스프레스" to "global.cainiao"),
        "com.einnovation.temu" to ("테무" to "global.temu"),
        "com.oliveyoung" to ("올리브영" to null),
        "com.kurly.android" to ("컬리" to "kr.kurly")
    )

    fun parse(packageName: String, title: String, text: String): ParsedNotificationCapture? {
        val combined = listOf(title.trim(), text.trim()).filter { it.isNotEmpty() }.joinToString("\n")
        if (combined.length < 8) return null

        val lower = combined.lowercase()
        val hasDeliveryKeyword = deliveryKeywords.any { lower.contains(it) }
        val hasNoise = noiseKeywords.any { lower.contains(it.lowercase()) } && !lower.contains("송장")
        if (!hasDeliveryKeyword || hasNoise) return null

        val parsed = WaybillDetector.parseNotificationText(combined)
        if (parsed.waybillNumber.length < 8) return null

        val (appLabel, defaultCarrierHint) = packageLabels[packageName] ?: (packageName.substringAfterLast('.') to null)
        val effectiveCarrierHint = parsed.carrierHintId ?: defaultCarrierHint
        val effectiveSenderHint = parsed.senderNameHint.ifBlank {
            if (packageName !in setOf("com.kakao.talk", "com.google.android.apps.messaging", "com.samsung.android.messaging")) {
                appLabel
            } else ""
        }

        val score = buildScore(combined, parsed, effectiveCarrierHint)
        if (score < 40) return null

        return ParsedNotificationCapture(
            notice = parsed.copy(
                carrierHintId = effectiveCarrierHint,
                senderNameHint = effectiveSenderHint
            ),
            sourceAppLabel = appLabel,
            confidenceScore = score
        )
    }

    private fun buildScore(
        text: String,
        notice: ExtractedWaybillNotice,
        carrierHintId: String?
    ): Int {
        var score = 25
        val lower = text.lowercase()
        if (lower.contains("송장") || lower.contains("운송장")) score += 30
        if (lower.contains("배송") || lower.contains("출고") || lower.contains("발송")) score += 20
        if (carrierHintId != null) score += 15
        if (notice.itemNameHint.isNotBlank()) score += 10
        return score.coerceAtMost(100)
    }
}
