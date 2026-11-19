package com.x2tora.torabox

import com.x2tora.torabox.domain.UniPassCustomsClient
import com.x2tora.torabox.model.StandardDeliveryPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class UniPassCustomsClientTest {

    @Test
    fun `parses unipass customs xml with item and events`() {
        val sampleXml = """
            <cargCsclPrgsInfoQryRtnVo>
              <cargCsclPrgsInfoQryVo>
                <prnm>MECHANICAL KEYBOARD</prnm>
                <ttwg>1.25</ttwg>
                <prgsStts>반출신고</prgsStts>
                <frwrEntsConm>CAINIAO</frwrEntsConm>
              </cargCsclPrgsInfoQryVo>
              <cargCsclPrgsInfoDtlQryVo>
                <cargTrcnRelaBsopTpcd>반출신고</cargTrcnRelaBsopTpcd>
                <prcsDttm>20261119103000</prcsDttm>
                <shedNm>인천특송물류센터</shedNm>
                <rlbrCn>CJ대한통운 운송장번호 566820864724 인계</rlbrCn>
              </cargCsclPrgsInfoDtlQryVo>
              <cargCsclPrgsInfoDtlQryVo>
                <cargTrcnRelaBsopTpcd>통관목록접수</cargTrcnRelaBsopTpcd>
                <prcsDttm>20261119081500</prcsDttm>
                <shedNm>인천공항</shedNm>
                <rlbrCn>목록통관 접수</rlbrCn>
              </cargCsclPrgsInfoDtlQryVo>
            </cargCsclPrgsInfoQryRtnVo>
        """.trimIndent()

        val result = UniPassCustomsClient.parseCustomsXml(sampleXml, "LP123456789012", "global.cainiao")
        assertNotNull(result)
        assertEquals("MECHANICAL KEYBOARD", result!!.itemName)
        assertEquals("1.25kg", result.itemQuantity)
        assertEquals(2, result.events.size)
        assertEquals("반출신고", result.currentStatusLabel)
        assertEquals(StandardDeliveryPhase.ARRIVED_AT_BRANCH, result.currentPhase)
        assertEquals("566820864724", result.linkedDomesticWaybill)
        assertEquals("kr.cjlogistics", result.linkedDomesticCarrierId)
    }
}
