package com.x2tora.torabox

import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.domain.ParcelTrackingEngine
import com.x2tora.torabox.domain.WaybillDetector
import com.x2tora.torabox.model.StandardDeliveryPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToraBoxDomainTest {

    @Test
    fun `korean carrier catalog covers 75 plus carriers with custom stage pipelines`() {
        val carriers = KoreanCarrierCatalog.allCarriers
        assertTrue("Expected >= 75 carriers, got ${carriers.size}", carriers.size >= 75)
        carriers.forEach { c ->
            assertTrue("${c.id} missing stagePipeline", c.stagePipeline.isNotEmpty())
        }
        val cj = KoreanCarrierCatalog.findById("kr.cjlogistics")!!
        val epost = KoreanCarrierCatalog.findById("kr.epost")!!
        val gs = KoreanCarrierCatalog.findById("kr.cvsnet")!!
        assertEquals("간선상·하차", cj.stagePipeline[2].stageName)
        assertEquals("집중국이동", epost.stagePipeline[2].stageName)
        assertEquals("점포접수", gs.stagePipeline[0].stageName)
        assertEquals(4, KoreanCarrierCatalog.resolveStageForStatus(cj, "배달출발", StandardDeliveryPhase.OUT_FOR_DELIVERY))
    }

    @Test
    fun `waybill detector verifies mod7 checksum and upu s10 check digit`() {
        // Lotte/Hanjin Mod-7: 24086466241 -> 24086466241 % 7 = 3 -> 240864662413
        val base11 = 24086466241L
        val check = (base11 % 7L).toInt()
        val valid12 = "$base11$check"
        assertTrue(WaybillDetector.isMod7Valid12Digits(valid12))
        assertFalse(WaybillDetector.isMod7Valid12Digits("${base11}${(check + 1) % 7}"))

        val mod7Recs = WaybillDetector.recommendCarriers(valid12)
        assertTrue(mod7Recs.any { it.carrier.id == "kr.lotte" && it.isChecksumVerified })
        assertTrue(mod7Recs.any { it.carrier.id == "kr.hanjin" && it.isChecksumVerified })

        // UPU S10: EA000000008KR -> 8*7=56 -> 56%11=1 -> 11-1=10 -> check digit 0? Wait:
        // weights [8,6,4,2,3,5,9,7]: serial "12345678" -> 1*8+2*6+3*4+4*2+5*3+6*5+7*9+8*7 = 8+12+12+8+15+30+63+56 = 204
        // 204 % 11 = 6 -> 11 - 6 = 5 -> "EM123456785KR"
        assertTrue(WaybillDetector.isUpuS10Valid("EM123456785KR"))
        assertFalse(WaybillDetector.isUpuS10Valid("EM123456789KR"))
        val emsRecs = WaybillDetector.recommendCarriers("EM123456785KR")
        assertEquals("kr.epost.ems", emsRecs.first().carrier.id)
        assertTrue(emsRecs.first().isChecksumVerified)
    }

    @Test
    fun `waybill detector parses korean sms and recommends carrier accurately`() {
        val sms = """
            [CJ대한통운] 고객님 상품이 발송되었습니다.
            운송장번호: 5668-2086-4724
            상품명: 루헨스 정수기 필터 2개입
            보낸분: 루헨스공식몰
            문의: 1588-1255
        """.trimIndent()
        val extracted = WaybillDetector.parseNotificationText(sms)
        assertEquals("566820864724", extracted.waybillNumber)
        assertEquals("kr.cjlogistics", extracted.carrierHintId)
        assertEquals("루헨스 정수기 필터 2개입", extracted.itemNameHint)
        assertEquals("루헨스공식몰", extracted.senderNameHint)

        val recs = WaybillDetector.recommendCarriers(extracted.waybillNumber, extracted.carrierHintId)
        assertEquals("kr.cjlogistics", recs.first().carrier.id)

        // 11-digit Logen
        val logenRecs = WaybillDetector.recommendCarriers("90123456789")
        assertEquals("kr.logen", logenRecs.first().carrier.id)

        // 13-digit Korea Post
        val epostRecs = WaybillDetector.recommendCarriers("6091234567890")
        assertEquals("kr.epost", epostRecs.first().carrier.id)

        // 14-digit Coupang CLS
        val clsRecs = WaybillDetector.recommendCarriers("10123456789012")
        assertEquals("kr.coupangls", clsRecs.first().carrier.id)
    }

    @Test
    fun `cj logistics next rest json parser extracts item sender and events`() {
        val cj = KoreanCarrierCatalog.findById("kr.cjlogistics")!!
        val waybillJson = """
            {
              "resultCode": 200,
              "data": {
                "wblNo": "566820864724",
                "sndrNm": "이*호",
                "sndrClphno": "010-****-1111",
                "sndrAddr": "경기도 용인시",
                "rcvrNm": "최*",
                "repGoodsNm": "루헨스 정수기 필터",
                "goodsDtlNm": "2개입",
                "qty": "1",
                "acprNm": "본인",
                "acprRlpDnm": "문앞"
              }
            }
        """.trimIndent()
        val detailJson = """
            {
              "resultCode": 200,
              "data": {
                "svcOutList": [
                  {
                    "branNm": "용인집배점",
                    "procBranTelNo": "031-111-2222",
                    "workDt": "2026-11-16",
                    "workHms": "17:40:00",
                    "crgStDnm": "집화처리",
                    "crgStDcdVal": "상품을 인수하였습니다.",
                    "patnBranNm": ""
                  },
                  {
                    "branNm": "강남대리점",
                    "procBranTelNo": "02-555-6666",
                    "workDt": "2026-11-17",
                    "workHms": "08:20:00",
                    "crgStDnm": "배달출발",
                    "crgStDcdVal": "배송기사 홍길동 010-1234-5678 출발",
                    "patnBranNm": ""
                  }
                ]
              }
            }
        """.trimIndent()

        val result = ParcelTrackingEngine.parseCjNextRestJson(cj, "566820864724", waybillJson, detailJson)
        assertNotNull(result)
        assertEquals("루헨스 정수기 필터 2개입", result!!.itemName)
        assertEquals("이*호", result.senderName)
        assertEquals("최*", result.recipientName)
        assertEquals("본인 · 문앞", result.accepterName)
        assertEquals("배달출발", result.currentStatusLabel)
        assertEquals(StandardDeliveryPhase.OUT_FOR_DELIVERY, result.currentPhase)
        assertEquals(2, result.events.size)
        assertEquals("홍길동", result.events.first().driverName)
        assertEquals("010-1234-5678", result.events.first().driverPhone)
    }

    @Test
    fun `korea post and cvsnet and logen and lotte parsers extract metadata cleanly`() {
        val epost = KoreanCarrierCatalog.findById("kr.epost")!!
        val epostHtml = """
            <table class="table_col"><tbody><tr>
              <td>김*수<br>2026.11.16</td>
              <td>이*영<br>2026.11.17</td>
              <td>우체국소포</td>
              <td>등기소포</td>
            </tr></tbody></table>
            <table id="processTable"><tbody>
              <tr><td>2026.11.16</td><td>15:20</td><td>성남우체국</td><td>접수 소포 물품 사진 <![CDATA[//]]></td></tr>
              <tr><td>2026.11.17</td><td>08:10</td><td>강남우체국</td><td>배달준비</td></tr>
            </tbody></table>
        """.trimIndent()
        val epostRes = ParcelTrackingEngine.parseKoreaPostHtml(epost, "6091234567890", epostHtml)!!
        assertEquals("김*수", epostRes.senderName)
        assertEquals("이*영", epostRes.recipientName)
        assertEquals("우체국소포", epostRes.serviceType)
        assertEquals("배달준비", epostRes.currentStatusLabel)
        assertEquals(2, epostRes.events.size)

        val gs = KoreanCarrierCatalog.findById("kr.cvsnet")!!
        val cvsHtml = """
            <script>
            var trackingInfo = {"code":200,"goodsName":"잡화/서적","serviceName":"반값택배","sender":{"name":"박*민","tel":"010-11**-22**","baseAddress":"서울 강남구","detailAddress":""},"receiver":{"name":"최*우","tel":"010-33**-44**","baseAddress":"서울 서초구","detailAddress":""},"trackingDetails":[{"transTime":"2026-11-16T10:15:00","level":1,"transWhere":"GS25역삼점","transCode":"C01","transKind":"점포접수"},{"transTime":"2026-11-17T09:00:00","level":5,"transWhere":"GS25서초점","transCode":"C10","transKind":"점포도착"}]};
            </script>
        """.trimIndent()
        val cvsRes = ParcelTrackingEngine.parseCvsnetHtml(gs, "390481920144", cvsHtml)!!
        assertEquals("잡화/서적", cvsRes.itemName)
        assertEquals("박*민", cvsRes.senderName)
        assertEquals("반값택배", cvsRes.serviceType)
        assertEquals("점포도착", cvsRes.currentStatusLabel)

        val logen = KoreanCarrierCatalog.findById("kr.logen")!!
        val logenHtml = """
            <table class="horizon pdInfo"><tbody>
              <tr><th>물품명</th><td>컴퓨터 부품</td><th>수량</th><td>1</td></tr>
              <tr><th>보내시는분</th><td>토라테크</td><th>받으시는분</th><td>홍*동</td></tr>
            </tbody></table>
            <table class="data tkInfo"><tbody>
              <tr><td>2026.11.17 08:30</td><td>강남지점</td><td>배송출고</td><td>배송출고 되었습니다</td><td></td><td>박기사</td><td>010-9999-8888</td></tr>
            </tbody></table>
        """.trimIndent()
        val logenRes = ParcelTrackingEngine.parseLogenHtml(logen, "90123456789", logenHtml)!!
        assertEquals("컴퓨터 부품", logenRes.itemName)
        assertEquals("토라테크", logenRes.senderName)
        assertEquals("배송출고", logenRes.currentStatusLabel)
        assertEquals("박기사", logenRes.events.first().driverName)

        val lotte = KoreanCarrierCatalog.findById("kr.lotte")!!
        val lotteJson = """
            {
              "errorCd": "0",
              "errorNm": "",
              "tracking": [
                {
                  "BRNSHP_CD": "100",
                  "BRNSHP_NM": "동서울터미널",
                  "BRNSHP_TEL": "02-111-2222",
                  "EMP_NM": "이기사",
                  "EMP_NO": "123",
                  "EMP_TEL": "010-2222-3333",
                  "GODS_STAT_CD": "40",
                  "GODS_STAT_NM": "배달전",
                  "PTN_BRNSHP_CD": "",
                  "PTN_BRNSHP_NM": "강남대리점",
                  "PTN_BRNSHP_TEL": "",
                  "SCAN_TME": "083500",
                  "SCAN_YMD": "20261117",
                  "STATUS": "배달 예정입니다"
                }
              ]
            }
        """.trimIndent()
        val lotteRes = ParcelTrackingEngine.parseLotteOpenApiJson(lotte, "240864662413", lotteJson)!!
        assertEquals("배달전", lotteRes.currentStatusLabel)
        assertEquals(StandardDeliveryPhase.OUT_FOR_DELIVERY, lotteRes.currentPhase)
        assertEquals("이기사", lotteRes.events.first().driverName)
    }
}
