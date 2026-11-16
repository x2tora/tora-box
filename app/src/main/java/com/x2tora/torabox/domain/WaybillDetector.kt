package com.x2tora.torabox.domain

import com.x2tora.torabox.model.CarrierInfo
import com.x2tora.torabox.model.CarrierRecommendation
import com.x2tora.torabox.model.ExtractedWaybillNotice

object WaybillDetector {

    private val upuWeights = intArrayOf(8, 6, 4, 2, 3, 5, 9, 7)

    fun normalizeWaybill(raw: String): String =
        raw.trim().uppercase().filter { it.isLetterOrDigit() }

    fun isMod7Valid12Digits(digits: String): Boolean {
        if (digits.length != 12 || !digits.all(Char::isDigit)) return false
        val body = digits.substring(0, 11).toLongOrNull() ?: return false
        val check = digits[11].digitToInt()
        return (body % 7L).toInt() == check
    }

    fun isUpuS10Valid(code: String): Boolean {
        val clean = normalizeWaybill(code)
        if (!clean.matches(Regex("^[A-Z]{2}\\d{9}[A-Z]{2}$"))) return false
        val serial = clean.substring(2, 10)
        val checkDigit = clean[10].digitToInt()
        var sum = 0
        for (i in 0 until 8) {
            sum += serial[i].digitToInt() * upuWeights[i]
        }
        val rem = sum % 11
        val expected = when (val diff = 11 - rem) {
            10 -> 0
            11 -> 5
            else -> diff
        }
        return checkDigit == expected
    }

    fun parseNotificationText(rawText: String): ExtractedWaybillNotice {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return ExtractedWaybillNotice("")

        val carrierHintId = detectCarrierHint(trimmed)
        val itemHint = extractFieldHint(
            trimmed,
            listOf("상품명", "물품명", "품목명", "품목", "주문상품", "배송상품")
        )
        val senderHint = extractFieldHint(
            trimmed,
            listOf("보낸분", "보내는분", "보낸이", "송화인", "발송처", "발송인", "쇼핑몰")
        )

        // Prioritize explicit waybill tokens near tracking keywords
        val explicitRegex = Regex(
            "(?:운송장\\s*번호|운송장|송장\\s*번호|송장|등기\\s*번호|Tracking\\s*(?:No|Number)?)\\s*[:：\\-]?\\s*([A-Za-z0-9][A-Za-z0-9\\-\\s]{7,24}[A-Za-z0-9])",
            RegexOption.IGNORE_CASE
        )
        val explicitMatch = explicitRegex.find(trimmed)?.groupValues?.getOrNull(1)
        val explicitClean = explicitMatch?.let(::normalizeWaybill)?.takeIf { isPlausibleWaybill(it) }
        if (explicitClean != null) {
            return ExtractedWaybillNotice(
                waybillNumber = explicitClean,
                carrierHintId = carrierHintId,
                itemNameHint = itemHint,
                senderNameHint = senderHint
            )
        }

        // Fall back to scanning candidate tokens while excluding Korean phone numbers
        val tokenRegex = Regex("\\b([A-Za-z]{2}\\d{9}[A-Za-z]{2}|1Z[A-Za-z0-9]{16}|(?:LP|YT|CN|SF)[A-Za-z0-9]{10,20}|\\d[\\d\\-]{8,22}\\d)\\b", RegexOption.IGNORE_CASE)
        val candidates = tokenRegex.findAll(trimmed)
            .map { it.groupValues[1] }
            .filterNot { looksLikePhoneOrDate(it) }
            .map(::normalizeWaybill)
            .filter(::isPlausibleWaybill)
            .toList()

        val bestWaybill = candidates.firstOrNull() ?: normalizeWaybill(trimmed).takeIf(::isPlausibleWaybill).orEmpty()
        return ExtractedWaybillNotice(
            waybillNumber = bestWaybill,
            carrierHintId = carrierHintId,
            itemNameHint = itemHint,
            senderNameHint = senderHint
        )
    }

    private fun looksLikePhoneOrDate(rawToken: String): Boolean {
        val digits = rawToken.filter(Char::isDigit)
        if (rawToken.startsWith("010") || rawToken.startsWith("011") ||
            rawToken.startsWith("1588") || rawToken.startsWith("1577") ||
            rawToken.startsWith("1544") || rawToken.startsWith("1644") ||
            rawToken.startsWith("1899") || rawToken.startsWith("1800") ||
            rawToken.startsWith("02-") || rawToken.startsWith("031-") || rawToken.startsWith("070")
        ) {
            return digits.length <= 11
        }
        return false
    }

    private fun isPlausibleWaybill(normalized: String): Boolean {
        if (normalized.length !in 9..24) return false
        if (normalized.all(Char::isDigit)) return normalized.length in 10..20
        return normalized.any(Char::isDigit)
    }

    private fun detectCarrierHint(text: String): String? {
        val lower = text.lowercase()
        val keywordPairs = listOf(
            listOf("cj대한통운", "cj 대한통운", "대한통운", "cj택배") to "kr.cjlogistics",
            listOf("우체국ems", "우체국 ems", "국제특급") to "kr.epost.ems",
            listOf("우체국", "epost") to "kr.epost",
            listOf("한진택배", "한진") to "kr.hanjin",
            listOf("롯데택배", "롯데글로벌", "롯데") to "kr.lotte",
            listOf("로젠택배", "로젠") to "kr.logen",
            listOf("쿠팡", "로켓배송", "coupang") to "kr.coupangls",
            listOf("gs25", "gspostbox", "gs택배", "반값택배") to "kr.cvsnet",
            listOf("cu편의점", "cu택배", "알뜰택배", "b2c", "포스트박스 cu") to "kr.cupost",
            listOf("세븐일레븐", "착한택배") to "kr.seveneleven",
            listOf("컬리넥스트마일", "마켓컬리", "컬리", "샛별배송") to "kr.kurly",
            listOf("경동택배", "경동화물", "경동") to "kr.kdexp",
            listOf("대신택배", "대신화물", "대신") to "kr.daesin",
            listOf("천일택배", "천일정기화물") to "kr.chunilps",
            listOf("합동택배") to "kr.hdexp",
            listOf("일양로지스", "일양택배") to "kr.ilyanglogis",
            listOf("건영택배") to "kr.kunyoung",
            listOf("홈픽") to "kr.homepick",
            listOf("오늘의픽업") to "kr.todaypickup",
            listOf("차이냐오", "cainiao", "알리익스프레스") to "cn.cainiao.global",
            listOf("dhl") to "de.dhl",
            listOf("fedex", "페덱스") to "us.fedex",
            listOf("ups") to "us.ups"
        )
        for ((keys, carrierId) in keywordPairs) {
            if (keys.any { lower.contains(it) }) return carrierId
        }
        return null
    }

    private fun extractFieldHint(text: String, labels: List<String>): String {
        val joined = labels.joinToString("|")
        val regex = Regex("(?:$joined)\\s*[:：]\\s*([^\\n\\r]{2,48})")
        val raw = regex.find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        return raw.trimEnd(',', '.', ')', ']')
    }

    fun recommendCarriers(
        rawInput: String,
        carrierHintId: String? = null
    ): List<CarrierRecommendation> {
        val clean = normalizeWaybill(rawInput)
        if (clean.length < 6) return emptyList()

        val scores = linkedMapOf<String, Int>()
        val reasons = linkedMapOf<String, MutableList<String>>()
        val checksumMatched = hashSetOf<String>()

        fun bump(carrierId: String, delta: Int, reason: String? = null, isChecksum: Boolean = false) {
            scores[carrierId] = (scores[carrierId] ?: 0) + delta
            if (reason != null) {
                reasons.getOrPut(carrierId) { mutableListOf() }.add(reason)
            }
            if (isChecksum) checksumMatched.add(carrierId)
        }

        if (carrierHintId != null) {
            bump(carrierHintId, 45, "메시지·키워드 일치")
        }

        val allDigits = clean.all(Char::isDigit)
        val len = clean.length

        // UPU S10 international tracking (e.g. EM123456789KR, LK123456789CN)
        if (clean.matches(Regex("^[A-Z]{2}\\d{9}[A-Z]{2}$"))) {
            val s10Valid = isUpuS10Valid(clean)
            val badge = if (s10Valid) "UPU S10 체크디지트 일치" else "국제등기 13자리 형식"
            val suffix = clean.takeLast(2)
            bump("kr.epost.ems", if (s10Valid) 94 else 78, badge, isChecksum = s10Valid)
            bump("kr.epost", if (s10Valid) 84 else 70, badge, isChecksum = s10Valid)
            if (suffix == "JP") bump("jp.japanpost", 88, "일본우편 국가코드(JP)")
            if (suffix == "CN") bump("cn.cainiao.global", 74, "중국발 국제등기(CN)")
        }

        // UPS 1Z...
        if (clean.startsWith("1Z") && len == 18) {
            bump("us.ups", 96, "UPS 1Z 18자리 규격", isChecksum = true)
        }

        // Cainiao / AliExpress prefixes
        if (clean.matches(Regex("^(LP|YT|CN|SF|SY)[A-Z0-9]{9,20}$"))) {
            bump("cn.cainiao.global", 91, "직구 송장 프리픽스 (${clean.take(2)})")
            bump("kr.actcore.ocean", 52, "해외직구 통관 연계 후보")
        }

        if (allDigits) {
            when (len) {
                10 -> {
                    bump("kr.cjlogistics", 72, "10자리 운송장 규격")
                    bump("kr.hanjin", 66, "10자리 운송장 규격")
                    bump("kr.cvsnet", 58, "GS25 택배 10자리")
                    bump("kr.cupost", 56, "CU 택배 10자리")
                    bump("de.dhl", 54, "DHL 10자리 규격")
                    bump("kr.ilyanglogis", 48, "일양로지스 10자리")
                    bump("kr.kunyoung", 42, "건영택배 10자리")
                }
                11 -> {
                    val p1 = clean[0]
                    bump("kr.logen", if (p1 in setOf('9', '7', '3')) 88 else 78, "로젠택배 11자리 규격")
                    bump("kr.kdexp", 64, "경동택배 11자리 화물 규격")
                    bump("kr.chunilps", 50, "천일택배 11자리")
                    bump("jp.japanpost", 35, "일본우편 11자리")
                }
                12 -> {
                    val mod7 = isMod7Valid12Digits(clean)
                    val prefix2 = clean.substring(0, 2).toIntOrNull() ?: 0
                    val prefix1 = clean[0]

                    // CJ대한통운 12자리 주요 대역 (33~39, 55~59, 60~69, 80~89)
                    val cjPrefix = prefix2 in 33..39 || prefix2 in 55..59 || prefix2 in 60..69 || prefix2 in 80..89
                    bump(
                        "kr.cjlogistics",
                        if (cjPrefix) 86 else 72,
                        if (cjPrefix) "CJ 주요 대역(${clean.take(2)}xx) · 12자리" else "12자리 운송장"
                    )

                    if (mod7) {
                        val lotteBias = if (prefix1 in setOf('2', '3', '4')) 92 else 85
                        val hanjinBias = if (prefix1 in setOf('4', '5', '8')) 89 else 81
                        bump("kr.lotte", lotteBias, "Mod-7 체크섬 일치 · 12자리", isChecksum = true)
                        bump("kr.hanjin", hanjinBias, "Mod-7 체크섬 일치 · 12자리", isChecksum = true)
                    } else {
                        bump("kr.hanjin", 64, "12자리 운송장")
                        bump("kr.lotte", 52, "12자리(체크섬 불일치 · 참고용)")
                    }

                    bump("kr.cvsnet", 62, "GS Postbox 12자리")
                    bump("kr.cupost", 60, "CU 편의점택배 12자리")
                    if (clean.startsWith("10") || clean.startsWith("20")) {
                        bump("kr.coupangls", 74, "쿠팡 CLS 대역 후보")
                    } else {
                        bump("kr.coupangls", 50, "12자리 운송장 후보")
                    }
                    bump("kr.homepick", 44, "홈픽 12자리")
                    bump("kr.ilyanglogis", 42, "일양로지스 12자리")
                    bump("us.fedex", 40, "FedEx 12자리")
                }
                13 -> {
                    val p2 = clean.substring(0, 2)
                    val epostPrefix = p2 in setOf("60", "61", "68", "69", "10", "11", "70", "50", "40")
                    bump(
                        "kr.epost",
                        if (epostPrefix) 92 else 82,
                        if (epostPrefix) "우체국 13자리 대역($p2)" else "우체국 13자리 표준"
                    )
                    bump("kr.daesin", 68, "대신택배 13자리")
                    bump("kr.kdexp", 66, "경동택배 13자리")
                    bump("kr.hdexp", 62, "합동택배 13자리")
                    bump("kr.chunilps", 56, "천일택배 13자리")
                    bump("kr.cvsnet", 54, "GS 반값택배 13자리 후보")
                    bump("kr.cupost", 52, "CU 알뜰택배 13자리 후보")
                }
                14 -> {
                    val isCoupangPrefix = clean.startsWith("10") || clean.startsWith("20")
                    bump(
                        "kr.coupangls",
                        if (isCoupangPrefix) 92 else 80,
                        if (isCoupangPrefix) "쿠팡 CLS 14자리 대역(${clean.take(2)})" else "쿠팡 CLS 14자리"
                    )
                    bump("kr.hanjin", 70, "한진택배 14자리")
                    bump("kr.cvsnet", 64, "GS 반값택배 14자리")
                    bump("kr.kdexp", 60, "경동택배 14자리")
                    bump("kr.hdexp", 56, "합동택배 14자리")
                }
                15 -> {
                    bump("us.fedex", 84, "FedEx 15자리 규격")
                    bump("kr.teamfresh", 62, "팀프레시 15자리 후보")
                }
                20, 22 -> {
                    bump("us.usps", 86, "USPS ${len}자리 규격")
                    bump("us.fedex", 74, "FedEx ${len}자리 규격")
                }
            }
        }

        // Map scores into sorted CarrierRecommendation list
        return scores.entries
            .mapNotNull { (carrierId, score) ->
                val carrier = KoreanCarrierCatalog.findById(carrierId) ?: return@mapNotNull null
                CarrierRecommendation(
                    carrier = carrier,
                    confidencePercent = score.coerceIn(15, 99),
                    reasonBadges = reasons[carrierId].orEmpty().distinct(),
                    isChecksumVerified = carrierId in checksumMatched
                )
            }
            .sortedByDescending { it.confidencePercent }
            .take(8)
    }
}
