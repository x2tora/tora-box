package com.x2tora.torabox

import com.x2tora.torabox.domain.NotificationMessageParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationMessageParserTest {

    @Test
    fun `parses kakao cj tracking notification with item and carrier`() {
        val res = NotificationMessageParser.parse(
            packageName = "com.kakao.talk",
            title = "CJ대한통운 알림톡",
            text = "[CJ대한통운] 고객님의 상품이 발송되었습니다. 상품명: 무선키보드 운송장번호: 5668-2086-4724"
        )
        assertNotNull(res)
        assertEquals("566820864724", res!!.notice.waybillNumber)
        assertEquals("kr.cjlogistics", res.notice.carrierHintId)
        assertEquals("카카오톡", res.sourceAppLabel)
    }

    @Test
    fun `parses coupang shipping push notification`() {
        val res = NotificationMessageParser.parse(
            packageName = "com.coupang.mobile",
            title = "쿠팡 로켓배송 출발",
            text = "주문하신 상품(로켓배송)이 오늘 도착 예정입니다. 송장번호 123456789012"
        )
        assertNotNull(res)
        assertEquals("123456789012", res!!.notice.waybillNumber)
        assertEquals("kr.coupangls", res.notice.carrierHintId)
        assertEquals("쿠팡", res.sourceAppLabel)
    }

    @Test
    fun `rejects otp and card payment notifications`() {
        val otp = NotificationMessageParser.parse(
            packageName = "com.google.android.apps.messaging",
            title = "본인확인",
            text = "[Web발신] 인증번호 [123456]을 입력해 주세요. 본인확인 보안코드 1234567890"
        )
        assertNull(otp)

        val payment = NotificationMessageParser.parse(
            packageName = "com.kakao.talk",
            title = "카드승인",
            text = "카드승인 45,000원 일시불 잔액 1234567890"
        )
        assertNull(payment)
    }
}
