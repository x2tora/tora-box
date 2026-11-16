package com.x2tora.torabox.domain

import com.x2tora.torabox.model.CarrierCategory
import com.x2tora.torabox.model.CarrierInfo
import com.x2tora.torabox.model.CarrierStageStep
import com.x2tora.torabox.model.StandardDeliveryPhase

object KoreanCarrierCatalog {

    private val cjPipeline = listOf(
        CarrierStageStep(0, "운송장등록", "송장 정보 접수 대기", StandardDeliveryPhase.PREPARING),
        CarrierStageStep(1, "집화처리", "담당 집배점 상품 인수", StandardDeliveryPhase.PICKED_UP),
        CarrierStageStep(2, "간선상·하차", "허브 터미널 간선 이동중", StandardDeliveryPhase.IN_TRANSIT),
        CarrierStageStep(3, "배달지도착", "배달 대리점·캠프 입고", StandardDeliveryPhase.ARRIVED_AT_BRANCH),
        CarrierStageStep(4, "배달출발", "배송기사 배달 출발", StandardDeliveryPhase.OUT_FOR_DELIVERY),
        CarrierStageStep(5, "배달완료", "수령인 전달 및 인수 완료", StandardDeliveryPhase.DELIVERED)
    )

    private val epostPipeline = listOf(
        CarrierStageStep(0, "접수", "우체국 창구·방문 접수", StandardDeliveryPhase.PREPARING),
        CarrierStageStep(1, "인수·발송", "우체국 발송 및 집하", StandardDeliveryPhase.PICKED_UP),
        CarrierStageStep(2, "집중국이동", "우편집중국 경유 발송·도착", StandardDeliveryPhase.IN_TRANSIT),
        CarrierStageStep(3, "배달국도착", "배달 관할 우체국 도착", StandardDeliveryPhase.ARRIVED_AT_BRANCH),
        CarrierStageStep(4, "배달준비", "집배원 배달 준비 및 출발", StandardDeliveryPhase.OUT_FOR_DELIVERY),
        CarrierStageStep(5, "배달완료", "우편물 배달 완료", StandardDeliveryPhase.DELIVERED)
    )

    private val hanjinPipeline = listOf(
        CarrierStageStep(0, "운송장등록", "운송장 정보 등록", StandardDeliveryPhase.PREPARING),
        CarrierStageStep(1, "집하접수", "담당 영업소 상품 인수", StandardDeliveryPhase.PICKED_UP),
        CarrierStageStep(2, "터미널이동", "허브 터미널 상·하차 이동", StandardDeliveryPhase.IN_TRANSIT),
        CarrierStageStep(3, "배송준비중", "도착 대리점 입고 완료", StandardDeliveryPhase.ARRIVED_AT_BRANCH),
        CarrierStageStep(4, "배송출발", "배송기사 고객지 출발", StandardDeliveryPhase.OUT_FOR_DELIVERY),
        CarrierStageStep(5, "배송완료", "수화인 인수 및 배송 완료", StandardDeliveryPhase.DELIVERED)
    )

    private val lottePipeline = listOf(
        CarrierStageStep(0, "운송장등록", "운송장 발행 및 접수", StandardDeliveryPhase.PREPARING),
        CarrierStageStep(1, "상품집하", "발송 대리점 상품 집하", StandardDeliveryPhase.PICKED_UP),
        CarrierStageStep(2, "구간발송·도착", "물류 터미널 적입·해체 이동", StandardDeliveryPhase.IN_TRANSIT),
        CarrierStageStep(3, "대리점도착", "배달 관할 대리점 도착", StandardDeliveryPhase.ARRIVED_AT_BRANCH),
        CarrierStageStep(4, "배달전", "배송기사 배차 및 배달 이동", StandardDeliveryPhase.OUT_FOR_DELIVERY),
        CarrierStageStep(5, "배달완료", "인수자 등록 및 배달 완료", StandardDeliveryPhase.DELIVERED)
    )

    private val logenPipeline = listOf(
        CarrierStageStep(0, "접수대기", "송장 등록 및 발송 준비", StandardDeliveryPhase.PREPARING),
        CarrierStageStep(1, "집하완료", "지점 집하완료 및 집하출고", StandardDeliveryPhase.PICKED_UP),
        CarrierStageStep(2, "터미널입출고", "행낭적입 · 터미널 입고·출고", StandardDeliveryPhase.IN_TRANSIT),
        CarrierStageStep(3, "배송입고", "도착 지점 배송입고", StandardDeliveryPhase.ARRIVED_AT_BRANCH),
        CarrierStageStep(4, "배송출고", "배송기사 출고 및 배달중", StandardDeliveryPhase.OUT_FOR_DELIVERY),
        CarrierStageStep(5, "배송완료", "고객 전달 및 사진확인 완료", StandardDeliveryPhase.DELIVERED)
    )

    private val coupangPipeline = listOf(
        CarrierStageStep(0, "운송장등록", "송장 등록 및 출고 대기", StandardDeliveryPhase.PREPARING),
        CarrierStageStep(1, "집하·센터상차", "물류센터 출고 및 집하", StandardDeliveryPhase.PICKED_UP),
        CarrierStageStep(2, "소터분류", "허브 센터·소터 분류 이동", StandardDeliveryPhase.IN_TRANSIT),
        CarrierStageStep(3, "캠프도착", "배송 캠프 상·하차 도착", StandardDeliveryPhase.ARRIVED_AT_BRANCH),
        CarrierStageStep(4, "배송출발", "배송기사 캠프 출발", StandardDeliveryPhase.OUT_FOR_DELIVERY),
        CarrierStageStep(5, "배송완료", "프레시백·문앞 전달 완료", StandardDeliveryPhase.DELIVERED)
    )

    private val cvsPipeline = listOf(
        CarrierStageStep(0, "점포접수", "편의점 무인택배기 접수", StandardDeliveryPhase.PREPARING),
        CarrierStageStep(1, "기사인수", "점포 수거 및 일배 입고", StandardDeliveryPhase.PICKED_UP),
        CarrierStageStep(2, "허브이동", "물류 허브 출고 및 간선 이동", StandardDeliveryPhase.IN_TRANSIT),
        CarrierStageStep(3, "도착점포이동", "도착 일배센터·점포 출고", StandardDeliveryPhase.ARRIVED_AT_BRANCH),
        CarrierStageStep(4, "점포도착·출발", "수령 점포 도착 또는 배송출발", StandardDeliveryPhase.OUT_FOR_DELIVERY),
        CarrierStageStep(5, "고객전달", "점포 픽업 및 배송 완료", StandardDeliveryPhase.DELIVERED)
    )

    private val cargoPipeline = listOf(
        CarrierStageStep(0, "영업소접수", "화물·정기화물 송장 접수", StandardDeliveryPhase.PREPARING),
        CarrierStageStep(1, "영업소집하", "발송 영업소 화물 인수", StandardDeliveryPhase.PICKED_UP),
        CarrierStageStep(2, "노선상차·중계", "화물 터미널 노선 상·하차", StandardDeliveryPhase.IN_TRANSIT),
        CarrierStageStep(3, "도착영업소", "수하인 관할 영업소 도착", StandardDeliveryPhase.ARRIVED_AT_BRANCH),
        CarrierStageStep(4, "배달차량상차", "지입·배달 차량 상차 출발", StandardDeliveryPhase.OUT_FOR_DELIVERY),
        CarrierStageStep(5, "인수확인", "화물 인수 확인 및 완료", StandardDeliveryPhase.DELIVERED)
    )

    private val sameDayPipeline = listOf(
        CarrierStageStep(0, "픽업접수", "당일·새벽배송 픽업 요청", StandardDeliveryPhase.PREPARING),
        CarrierStageStep(1, "픽업완료", "발송지 물품 수거 완료", StandardDeliveryPhase.PICKED_UP),
        CarrierStageStep(2, "거점분류", "마이크로 물류거점 입고·분류", StandardDeliveryPhase.IN_TRANSIT),
        CarrierStageStep(3, "권역배차", "배송 권역 기사 배차 완료", StandardDeliveryPhase.ARRIVED_AT_BRANCH),
        CarrierStageStep(4, "배송출발", "실시간 당일·새벽 배송중", StandardDeliveryPhase.OUT_FOR_DELIVERY),
        CarrierStageStep(5, "배송완료", "수령지 전달 완료", StandardDeliveryPhase.DELIVERED)
    )

    private val internationalPipeline = listOf(
        CarrierStageStep(0, "발송접수", "해외 발송 정보·운송장 등록", StandardDeliveryPhase.PREPARING),
        CarrierStageStep(1, "현지집하·출고", "발송지 집하 및 국제공항 출고", StandardDeliveryPhase.PICKED_UP),
        CarrierStageStep(2, "항공·해상운송", "국제 운송 및 국내 입항", StandardDeliveryPhase.IN_TRANSIT),
        CarrierStageStep(3, "수입통관", "세관 목록통관·수입신고 처리", StandardDeliveryPhase.ARRIVED_AT_BRANCH),
        CarrierStageStep(4, "국내배송출발", "국내 연계 택배사 인계·출발", StandardDeliveryPhase.OUT_FOR_DELIVERY),
        CarrierStageStep(5, "배달완료", "수취인 최종 전달 완료", StandardDeliveryPhase.DELIVERED)
    )

    private val standardDomesticPipeline = listOf(
        CarrierStageStep(0, "송장접수", "운송장 등록 및 발송 준비", StandardDeliveryPhase.PREPARING),
        CarrierStageStep(1, "집하·발송", "발송 거점 상품 인수", StandardDeliveryPhase.PICKED_UP),
        CarrierStageStep(2, "물류이동중", "허브·중계 터미널 이동", StandardDeliveryPhase.IN_TRANSIT),
        CarrierStageStep(3, "지점도착", "도착지 담당 지점 입고", StandardDeliveryPhase.ARRIVED_AT_BRANCH),
        CarrierStageStep(4, "배송출발", "담당 기사 배달 출발", StandardDeliveryPhase.OUT_FOR_DELIVERY),
        CarrierStageStep(5, "배송완료", "수령인 전달 완료", StandardDeliveryPhase.DELIVERED)
    )

    private fun simpleCarrier(
        id: String,
        sweetCode: String,
        trackerId: String? = null,
        nameKo: String,
        shortName: String = nameKo,
        category: CarrierCategory,
        phone: String = "",
        urlTemplate: String = "",
        accentHex: Long = 0xFF3482FF,
        iconKey: String = "box",
        lengths: List<Int> = emptyList(),
        patternHint: String = "숫자 송장번호",
        directItem: Boolean = false,
        directSender: Boolean = false,
        directQuery: Boolean = false,
        pipeline: List<CarrierStageStep> = when (category) {
            CarrierCategory.MAJOR -> standardDomesticPipeline
            CarrierCategory.CONVENIENCE_TODAY -> sameDayPipeline
            CarrierCategory.CARGO_B2B -> cargoPipeline
            CarrierCategory.INTERNATIONAL -> internationalPipeline
        }
    ) = CarrierInfo(
        id = id,
        sweetTrackerCode = sweetCode,
        trackerDeliveryId = trackerId,
        nameKo = nameKo,
        shortName = shortName,
        category = category,
        phone = phone,
        trackingUrlTemplate = urlTemplate,
        accentColorHex = accentHex,
        iconKey = iconKey,
        waybillLengths = lengths,
        waybillPatternHint = patternHint,
        supportsDirectItemName = directItem,
        supportsDirectSender = directSender,
        supportsDirectQuery = directQuery,
        stagePipeline = pipeline
    )

    val allCarriers: List<CarrierInfo> = listOf(
        // === 주요 택배사 ===
        CarrierInfo(
            id = "kr.cjlogistics",
            sweetTrackerCode = "04",
            trackerDeliveryId = "kr.cjlogistics",
            nameKo = "CJ대한통운",
            shortName = "CJ대한통운",
            category = CarrierCategory.MAJOR,
            phone = "1588-1255",
            trackingUrlTemplate = "https://trace.cjlogistics.com/next/tracking.html?wblNo={waybill}",
            accentColorHex = 0xFFEF4444,
            iconKey = "truck",
            waybillLengths = listOf(10, 12),
            waybillPatternHint = "10자리 또는 12자리 숫자 (예: 566820864724)",
            supportsDirectItemName = true,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = cjPipeline
        ),
        CarrierInfo(
            id = "kr.epost",
            sweetTrackerCode = "01",
            trackerDeliveryId = "kr.epost",
            nameKo = "우체국택배",
            shortName = "우체국",
            category = CarrierCategory.MAJOR,
            phone = "1588-1300",
            trackingUrlTemplate = "https://service.epost.go.kr/trace.RetrieveDomRigiTraceList.comm?displayHeader=N&sid1={waybill}",
            accentColorHex = 0xFFF97316,
            iconKey = "mail",
            waybillLengths = listOf(13),
            waybillPatternHint = "13자리 숫자 (60·61·68·69·10·11 시작) 또는 등기",
            supportsDirectItemName = true,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = epostPipeline
        ),
        CarrierInfo(
            id = "kr.hanjin",
            sweetTrackerCode = "05",
            trackerDeliveryId = "kr.hanjin",
            nameKo = "한진택배",
            shortName = "한진",
            category = CarrierCategory.MAJOR,
            phone = "1588-0011",
            trackingUrlTemplate = "https://www.hanjin.com/kor/CMS/DeliveryMgr/WaybillResult.do?mCode=MN038&schLang=KR&wblnumText2={waybill}",
            accentColorHex = 0xFF0EA5E9,
            iconKey = "plane",
            waybillLengths = listOf(10, 12, 14),
            waybillPatternHint = "12자리(Mod-7) 또는 10·14자리 숫자",
            supportsDirectItemName = true,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = hanjinPipeline
        ),
        CarrierInfo(
            id = "kr.lotte",
            sweetTrackerCode = "08",
            trackerDeliveryId = "kr.lotte",
            nameKo = "롯데택배",
            shortName = "롯데",
            category = CarrierCategory.MAJOR,
            phone = "1588-2121",
            trackingUrlTemplate = "https://www.lotteglogis.com/open/tracking?InvNo={waybill}",
            accentColorHex = 0xFFDC2626,
            iconKey = "box",
            waybillLengths = listOf(12),
            waybillPatternHint = "12자리 숫자 (앞 11자리 % 7 == 마지막 자리)",
            supportsDirectItemName = true,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = lottePipeline
        ),
        CarrierInfo(
            id = "kr.logen",
            sweetTrackerCode = "06",
            trackerDeliveryId = "kr.logen",
            nameKo = "로젠택배",
            shortName = "로젠",
            category = CarrierCategory.MAJOR,
            phone = "1588-9988",
            trackingUrlTemplate = "https://www.ilogen.com/web/personal/trace/{waybill}",
            accentColorHex = 0xFFF59E0B,
            iconKey = "box",
            waybillLengths = listOf(11),
            waybillPatternHint = "11자리 숫자 (예: 90123456789)",
            supportsDirectItemName = true,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = logenPipeline
        ),
        CarrierInfo(
            id = "kr.coupangls",
            sweetTrackerCode = "CLS",
            trackerDeliveryId = "kr.coupangls",
            nameKo = "쿠팡 로지스틱스(CLS)",
            shortName = "쿠팡 CLS",
            category = CarrierCategory.MAJOR,
            phone = "1577-7011",
            trackingUrlTemplate = "https://www.coupangls.com/web/modal/invoice/{waybill}",
            accentColorHex = 0xFF3B82F6,
            iconKey = "bolt",
            waybillLengths = listOf(12, 14),
            waybillPatternHint = "14자리(10 시작) 또는 12자리 숫자",
            supportsDirectItemName = false,
            supportsDirectSender = false,
            supportsDirectQuery = true,
            stagePipeline = coupangPipeline
        ),

        // === 편의점 · 당일 · 새벽배송 ===
        CarrierInfo(
            id = "kr.cvsnet",
            sweetTrackerCode = "24",
            trackerDeliveryId = "kr.cvsnet",
            nameKo = "GS Postbox (반값·국내택배)",
            shortName = "GS25",
            category = CarrierCategory.CONVENIENCE_TODAY,
            phone = "1577-1287",
            trackingUrlTemplate = "https://www.cvsnet.co.kr/invoice/tracking.do?invoice_no={waybill}",
            accentColorHex = 0xFF06B6D4,
            iconKey = "store",
            waybillLengths = listOf(10, 12, 13, 14),
            waybillPatternHint = "10~14자리 숫자 (반값택배 포함)",
            supportsDirectItemName = true,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = cvsPipeline
        ),
        CarrierInfo(
            id = "kr.cupost",
            sweetTrackerCode = "46",
            trackerDeliveryId = "kr.cupost",
            nameKo = "CU 편의점택배 (알뜰택배)",
            shortName = "CU택배",
            category = CarrierCategory.CONVENIENCE_TODAY,
            phone = "1577-1287",
            trackingUrlTemplate = "https://www.cupost.co.kr/postbox/delivery/localResult.cupost?invoice_no={waybill}",
            accentColorHex = 0xFF8B5CF6,
            iconKey = "store",
            waybillLengths = listOf(10, 12, 13),
            waybillPatternHint = "10~13자리 숫자 (CU 알뜰·국내택배)",
            supportsDirectItemName = true,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = cvsPipeline
        ),
        simpleCarrier("kr.seveneleven", "189", null, "세븐일레븐 착한택배", "세븐일레븐", CarrierCategory.CONVENIENCE_TODAY, "1577-0711", "https://www.7-eleven.co.kr/", 0xFF10B981, "store", listOf(12, 13), "12~13자리 숫자", pipeline = cvsPipeline),
        simpleCarrier("kr.homepick", "54", "kr.homepick", "홈픽택배", "홈픽", CarrierCategory.CONVENIENCE_TODAY, "1800-0987", "https://www.homepick.com/", 0xFF14B8A6, "home", listOf(12, 13), "12~13자리 숫자", directQuery = true, pipeline = sameDayPipeline),
        simpleCarrier("kr.homepick.today", "129", null, "홈픽 오늘도착", "홈픽 오늘도착", CarrierCategory.CONVENIENCE_TODAY, "1800-0987", "https://www.homepick.com/", 0xFF0D9488, "home", listOf(12, 13), "12~13자리 숫자", pipeline = sameDayPipeline),
        simpleCarrier("kr.kurly", "82", null, "컬리넥스트마일", "컬리", CarrierCategory.CONVENIENCE_TODAY, "1644-1107", "https://www.kurlynextmile.com/", 0xFF7C3AED, "bolt", listOf(12, 13, 14), "12~14자리 송장·주문번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.todaypickup", "TODAY", "kr.todaypickup", "오늘의픽업", "오늘의픽업", CarrierCategory.CONVENIENCE_TODAY, "1668-3326", "https://mall.todaypickup.com/front/delivery/list/{waybill}", 0xFFEC4899, "bolt", listOf(10, 12, 14), "10~14자리 숫자·영숫자", directQuery = true, pipeline = sameDayPipeline),
        simpleCarrier("kr.teamfresh", "TF", null, "팀프레시 (새벽배송)", "팀프레시", CarrierCategory.CONVENIENCE_TODAY, "1661-9910", "", 0xFF2563EB, "bolt", listOf(12, 14, 15), "영숫자·숫자 송장번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.today", "143", "kr.today", "투데이 (To-Day)", "투데이", CarrierCategory.CONVENIENCE_TODAY, "", "", 0xFF0EA5E9, "bolt", listOf(12, 13), "숫자 송장번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.doobalhero", "89", null, "두발히어로", "두발히어로", CarrierCategory.CONVENIENCE_TODAY, "1522-5984", "", 0xFFF97316, "bolt", listOf(10, 12), "당일배송 송장번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.chainlogis", "92", null, "지니고 당일배송", "지니고", CarrierCategory.CONVENIENCE_TODAY, "", "", 0xFF6366F1, "bolt", listOf(10, 12), "당일배송 송장번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.kakaot", "94", null, "카카오 T 당일배송", "카카오 T", CarrierCategory.CONVENIENCE_TODAY, "1599-9400", "", 0xFFF59E0B, "bolt", listOf(10, 12, 14), "카카오 T 송장번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.wonders", "75", null, "원더스퀵", "원더스퀵", CarrierCategory.CONVENIENCE_TODAY, "", "", 0xFF06B6D4, "bolt", listOf(10, 12), "퀵/당일 송장번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.delirabbit", "131", null, "딜리래빗", "딜리래빗", CarrierCategory.CONVENIENCE_TODAY, "", "", 0xFFEC4899, "bolt", listOf(10, 12, 13), "새벽·당일 송장번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.lastmile", "138", null, "라스트마일", "라스트마일", CarrierCategory.CONVENIENCE_TODAY, "", "", 0xFF10B981, "bolt", listOf(12), "숫자 송장번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.tango", "142", null, "탱고앤고", "탱고앤고", CarrierCategory.CONVENIENCE_TODAY, "", "", 0xFF3B82F6, "bolt", listOf(12), "숫자 송장번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.delibox", "167", null, "딜리박스", "딜리박스", CarrierCategory.CONVENIENCE_TODAY, "", "", 0xFF8B5CF6, "box", listOf(12), "숫자 송장번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.thunder", "113", null, "썬더히어로", "썬더히어로", CarrierCategory.CONVENIENCE_TODAY, "", "", 0xFFF59E0B, "bolt", listOf(10, 12), "당일배송 송장번호", pipeline = sameDayPipeline),
        simpleCarrier("kr.pingpong", "119", null, "핑퐁", "핑퐁", CarrierCategory.CONVENIENCE_TODAY, "", "", 0xFF0EA5E9, "bolt", listOf(10, 12), "당일배송 송장번호", pipeline = sameDayPipeline),

        // === 화물 · 중량 · B2B · 특수택배 ===
        CarrierInfo(
            id = "kr.kdexp",
            sweetTrackerCode = "23",
            trackerDeliveryId = "kr.kdexp",
            nameKo = "경동택배",
            shortName = "경동",
            category = CarrierCategory.CARGO_B2B,
            phone = "1899-5368",
            trackingUrlTemplate = "https://kdexp.com/service/delivery/new/delivery.do?barcode={waybill}",
            accentColorHex = 0xFF1D4ED8,
            iconKey = "cargo",
            waybillLengths = listOf(11, 13, 14),
            waybillPatternHint = "11·13·14자리 숫자 (대형화물·일반)",
            supportsDirectItemName = true,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = cargoPipeline
        ),
        CarrierInfo(
            id = "kr.daesin",
            sweetTrackerCode = "22",
            trackerDeliveryId = "kr.daesin",
            nameKo = "대신택배",
            shortName = "대신",
            category = CarrierCategory.CARGO_B2B,
            phone = "043-222-4582",
            trackingUrlTemplate = "https://www.ds3211.co.kr/freight/internalFreightSearch.ht?billno={waybill}",
            accentColorHex = 0xFF047857,
            iconKey = "cargo",
            waybillLengths = listOf(13),
            waybillPatternHint = "13자리 숫자 (정기화물·택배)",
            supportsDirectItemName = true,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = cargoPipeline
        ),
        CarrierInfo(
            id = "kr.ilyanglogis",
            sweetTrackerCode = "11",
            trackerDeliveryId = "kr.ilyanglogis",
            nameKo = "일양로지스",
            shortName = "일양",
            category = CarrierCategory.CARGO_B2B,
            phone = "1588-0002",
            trackingUrlTemplate = "https://www.ilyanglogis.com/functionality/tracking.asp",
            accentColorHex = 0xFF0284C7,
            iconKey = "truck",
            waybillLengths = listOf(10, 12),
            waybillPatternHint = "10자리 또는 12자리 숫자",
            supportsDirectItemName = false,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = standardDomesticPipeline
        ),
        CarrierInfo(
            id = "kr.chunilps",
            sweetTrackerCode = "17",
            trackerDeliveryId = "kr.chunilps",
            nameKo = "천일택배",
            shortName = "천일",
            category = CarrierCategory.CARGO_B2B,
            phone = "1877-6606",
            trackingUrlTemplate = "https://www.chunil.co.kr/HTrace/HTrace.jsp?transNo={waybill}",
            accentColorHex = 0xFF4F46E5,
            iconKey = "cargo",
            waybillLengths = listOf(11, 13),
            waybillPatternHint = "11자리 또는 13자리 숫자",
            supportsDirectItemName = true,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = cargoPipeline
        ),
        CarrierInfo(
            id = "kr.hdexp",
            sweetTrackerCode = "32",
            trackerDeliveryId = "kr.hdexp",
            nameKo = "합동택배",
            shortName = "합동",
            category = CarrierCategory.CARGO_B2B,
            phone = "1899-3392",
            trackingUrlTemplate = "https://hdexp.co.kr/deliverySearch2.hd?barcode={waybill}",
            accentColorHex = 0xFF0F766E,
            iconKey = "cargo",
            waybillLengths = listOf(13, 14),
            waybillPatternHint = "13~14자리 숫자 (대형화물)",
            supportsDirectItemName = true,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = cargoPipeline
        ),
        CarrierInfo(
            id = "kr.kunyoung",
            sweetTrackerCode = "18",
            trackerDeliveryId = "kr.kunyoung",
            nameKo = "건영택배",
            shortName = "건영",
            category = CarrierCategory.CARGO_B2B,
            phone = "053-354-3001",
            trackingUrlTemplate = "https://www.kunyoung.com/goods/goods_02.php?mulno={waybill}",
            accentColorHex = 0xFF6D28D9,
            iconKey = "cargo",
            waybillLengths = listOf(10, 12),
            waybillPatternHint = "10~12자리 숫자",
            supportsDirectItemName = false,
            supportsDirectSender = false,
            supportsDirectQuery = true,
            stagePipeline = cargoPipeline
        ),
        simpleCarrier("kr.hanips", "16", "kr.hanips", "한의사랑택배", "한의사랑", CarrierCategory.CARGO_B2B, "1600-1055", "", 0xFF059669, "box", listOf(12)),
        simpleCarrier("kr.hanjin.b2b", "20", null, "한진택배 B2B", "한진 B2B", CarrierCategory.CARGO_B2B, "1588-0011", "", 0xFF0284C7, "cargo", listOf(10, 12), pipeline = hanjinPipeline),
        simpleCarrier("kr.slx", "44", "kr.slx", "SLX택배", "SLX", CarrierCategory.CARGO_B2B, "031-637-5400", "https://net.slx.co.kr/info/tracking.jsp?iv_no={waybill}", 0xFF2563EB, "truck", listOf(12), directQuery = true),
        simpleCarrier("kr.cway", "45", "kr.cway", "우리택배(구 호남택배)", "우리택배", CarrierCategory.CARGO_B2B, "1588-4899", "", 0xFF14B8A6, "truck", listOf(10, 12), directQuery = true),
        simpleCarrier("kr.honamlogis", "45H", "kr.honamlogis", "호남택배", "호남택배", CarrierCategory.CARGO_B2B, "1877-0572", "", 0xFF0D9488, "truck", listOf(10, 12), directQuery = true),
        simpleCarrier("kr.nonghyup", "53", null, "농협택배", "농협", CarrierCategory.CARGO_B2B, "1588-2100", "", 0xFF16A34A, "store", listOf(12, 13)),
        simpleCarrier("kr.yongmalogis", "74", "kr.yongmalogis", "용마로지스", "용마로지스", CarrierCategory.CARGO_B2B, "1588-2060", "", 0xFF4F46E5, "cargo", listOf(10, 12), directQuery = true),
        simpleCarrier("kr.goodstoluck", "GTL", "kr.goodstoluck", "굿투럭", "굿투럭", CarrierCategory.CARGO_B2B, "", "", 0xFF0EA5E9, "truck", listOf(12), directQuery = true),
        simpleCarrier("kr.iklogis", "71", null, "IK물류", "IK물류", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.sunghoon", "72", null, "성훈물류", "성훈물류", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.crlogitech", "73", null, "CR로지텍", "CR로지텍", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.fullathome", "85", null, "풀앳홈", "풀앳홈", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.winia", "90", null, "위니아에이드", "위니아", CarrierCategory.CARGO_B2B, "1588-9588"),
        simpleCarrier("kr.trax", "95", null, "트랙스로지스", "트랙스", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.naeun", "100", null, "나은물류", "나은물류", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.hanssem", "101", null, "한샘서비스", "한샘", CarrierCategory.CARGO_B2B, "1588-0900"),
        simpleCarrier("kr.dodoflex", "104", null, "도도플렉스(dodoflex)", "도도플렉스", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.lge", "107", null, "LG전자 물류", "LG전자", CarrierCategory.CARGO_B2B, "1544-7777"),
        simpleCarrier("kr.lottechilsung", "118", null, "롯데칠성", "롯데칠성", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.valex", "120", null, "발렉스 특수물류", "발렉스", CarrierCategory.CARGO_B2B, "1599-5511"),
        simpleCarrier("kr.ntl", "123", null, "엔티엘피스", "엔티엘피스", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.gkglobal", "125", null, "지케이글로벌", "지케이글로벌", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.logispot", "127", null, "로지스팟", "로지스팟", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.logispartner", "130", null, "로지스파트너", "로지스파트너", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.gop", "132", null, "지오피", "지오피", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.hkholdings", "134", null, "에이치케이홀딩스", "HK홀딩스", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.htns", "135", null, "HTNS", "HTNS", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.kjt", "136", null, "케이제이티", "케이제이티", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.theham", "137", null, "더함", "더함", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.argo", "148", null, "ARGO (테크타카)", "ARGO", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.hy", "155", null, "HY (한국야쿠르트)", "HY", CarrierCategory.CARGO_B2B, "1577-3651"),
        simpleCarrier("kr.upjeju", "156", null, "유피로지스(제주)", "유피로지스", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.banyan", "157", null, "반얀로지스틱스", "반얀", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.prism", "160", null, "프리즘코리아", "프리즘", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.winion", "163", null, "위니온", "위니온", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.jaha", "168", null, "자하", "자하", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.allin", "171", null, "올인닷컴", "올인닷컴", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.mulryudaejang", "173", null, "물류대장(택배)", "물류대장", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.pulmuone", "175", null, "풀무원샘물", "풀무원샘물", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.slo", "179", null, "SLO(모든)", "SLO", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.baros", "183", null, "바로스", "바로스", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.letters", "186", null, "레터스", "레터스", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.vendorpia", "187", null, "벤더피아", "벤더피아", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.mulryu.install", "190", null, "물류대장(설치)", "물류대장 설치", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.boxn", "191", null, "BoxN", "BoxN", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.returniz", "192", null, "리터니즈", "리터니즈", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.wemeet", "196", null, "위밋모빌리티", "위밋", CarrierCategory.CARGO_B2B),
        simpleCarrier("kr.ltl", "LTL", "kr.ltl", "LTL", "LTL", CarrierCategory.CARGO_B2B),

        // === 국제 · 해외특송 ===
        CarrierInfo(
            id = "kr.epost.ems",
            sweetTrackerCode = "12",
            trackerDeliveryId = "kr.epost.ems",
            nameKo = "우체국 EMS (국제특송)",
            shortName = "EMS",
            category = CarrierCategory.INTERNATIONAL,
            phone = "1588-1300",
            trackingUrlTemplate = "https://service.epost.go.kr/trace.RetrieveEmsRigiTraceList.comm?POST_CODE={waybill}",
            accentColorHex = 0xFFEA580C,
            iconKey = "globe",
            waybillLengths = listOf(13),
            waybillPatternHint = "UPU S10 규격 13자리 (예: EM123456789KR)",
            supportsDirectItemName = false,
            supportsDirectSender = true,
            supportsDirectQuery = true,
            stagePipeline = internationalPipeline
        ),
        simpleCarrier("de.dhl", "13", "de.dhl", "DHL Express", "DHL", CarrierCategory.INTERNATIONAL, "1588-0001", "https://www.dhl.com/kr-ko/home/tracking.html?tracking-id={waybill}", 0xFFF59E0B, "globe", listOf(10), "10자리 숫자"),
        simpleCarrier("us.ups", "14", "us.ups", "UPS", "UPS", CarrierCategory.INTERNATIONAL, "1588-6886", "https://www.ups.com/track?loc=ko_KR&tracknum={waybill}", 0xFF78350F, "globe", listOf(18), "1Z로 시작하는 18자리 영숫자"),
        simpleCarrier("us.fedex", "21", "us.fedex", "FedEx", "FedEx", CarrierCategory.INTERNATIONAL, "1588-0588", "https://www.fedex.com/fedextrack/?trknbr={waybill}", 0xFF6D28D9, "globe", listOf(12, 15, 20), "12·15자리 숫자"),
        simpleCarrier("nl.tnt", "25", "nl.tnt", "TNT Express", "TNT", CarrierCategory.INTERNATIONAL, "1588-0588", "", 0xFFF97316, "globe", listOf(9, 13)),
        simpleCarrier("kr.epantos", "37", "kr.epantos", "LX판토스", "LX판토스", CarrierCategory.INTERNATIONAL, "02-3771-2114", "https://www.epantos.com/", 0xFFDC2626, "globe", listOf(10, 12, 13), directQuery = true),
        simpleCarrier("kr.cjlogistics.intl", "42", null, "CJ대한통운 국제특송", "CJ 국제특송", CarrierCategory.INTERNATIONAL, "1588-1255", "https://trace.cjlogistics.com/next/tracking.html?wblNo={waybill}", 0xFFEF4444, "globe", listOf(10, 12, 13)),
        simpleCarrier("kr.lotte.global", "LG", "kr.lotte.global", "롯데글로벌 국제특송", "롯데 국제", CarrierCategory.INTERNATIONAL, "1588-2121", "", 0xFFDC2626, "globe", listOf(12, 13)),
        simpleCarrier("cn.cainiao.global", "CN", "cn.cainiao.global", "차이냐오 (알리·직구)", "차이냐오", CarrierCategory.INTERNATIONAL, "", "https://global.cainiao.com/detail.htm?mailNoList={waybill}", 0xFFF97316, "globe", listOf(13, 14, 15, 16, 18), "LP/YT/CN 등 직구 송장"),
        simpleCarrier("kr.ecms", "38", null, "ECMS Express", "ECMS", CarrierCategory.INTERNATIONAL, "", "", 0xFF0EA5E9, "globe", listOf(12, 14, 16)),
        simpleCarrier("kr.aci", "48", null, "ACI Express", "ACI", CarrierCategory.INTERNATIONAL, "", "", 0xFF2563EB, "globe", listOf(12, 13)),
        simpleCarrier("kr.gsmnton", "28", null, "GSMNtoN (국제특송)", "GSMNtoN", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.kgl", "30", null, "KGL네트웍스", "KGL", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.gsi", "41", null, "GSI Express", "GSI", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.ace", "49", null, "A.C.E Express", "ACE", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.gpslogix", "50", null, "GPS Logix", "GPS Logix", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.europarcel", "55", null, "유로택배 (EuroParcel)", "EuroParcel", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.yjs.uk", "60", null, "YJS글로벌 (영국)", "YJS 영국", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.eunha", "63", null, "은하쉬핑", "은하쉬핑", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.yjs.world", "65", null, "YJS글로벌 (월드)", "YJS 월드", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.daerim", "69", null, "대림통운", "대림통운", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.lineexpress", "77", null, "LineExpress", "LineExpress", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.smartlogis", "84", null, "스마트로지스", "스마트로지스", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.actcore.ocean", "ACT", "kr.actcore.ocean-inbound", "ACT&CORE (해상직구)", "ACT&CORE", CarrierCategory.INTERNATIONAL, "", "", 0xFF0284C7, "globe", listOf(12, 13)),
        simpleCarrier("kr.swgexp.epost", "SWGE", "kr.swgexp.epost", "성원글로벌카고 (우체국연계)", "성원 우체국", CarrierCategory.INTERNATIONAL),
        simpleCarrier("kr.swgexp.cj", "SWGC", "kr.swgexp.cjlogistics", "성원글로벌카고 (CJ연계)", "성원 CJ", CarrierCategory.INTERNATIONAL),
        simpleCarrier("us.usps", "USPS", "us.usps", "USPS (미국우정)", "USPS", CarrierCategory.INTERNATIONAL, "", "", 0xFF1D4ED8, "globe", listOf(20, 22)),
        simpleCarrier("jp.japanpost", "JP", "jp.japanpost", "일본우편 (Japan Post)", "일본우편", CarrierCategory.INTERNATIONAL, "", "", 0xFFDC2626, "globe", listOf(11, 12, 13)),
        simpleCarrier("jp.sagawa", "SGW", "jp.sagawa", "사가와 익스프레스", "사가와", CarrierCategory.INTERNATIONAL, "", "", 0xFF2563EB, "globe", listOf(12)),
        simpleCarrier("jp.yamato", "YMT", "jp.yamato", "야마토 운수", "야마토", CarrierCategory.INTERNATIONAL, "", "", 0xFFF59E0B, "globe", listOf(12))
    )

    private val byIdMap: Map<String, CarrierInfo> = allCarriers.associateBy { it.id }
    private val bySweetCodeMap: Map<String, CarrierInfo> = allCarriers.associateBy { it.sweetTrackerCode }

    fun findById(id: String): CarrierInfo? = byIdMap[id]

    fun findBySweetTrackerCode(code: String): CarrierInfo? = bySweetCodeMap[code]

    fun findByNameOrAlias(raw: String): CarrierInfo? {
        val norm = raw.trim().lowercase().replace(" ", "")
        if (norm.isEmpty()) return null
        val aliasMap = mapOf(
            "cj" to "kr.cjlogistics",
            "cj대한통운" to "kr.cjlogistics",
            "대한통운" to "kr.cjlogistics",
            "cj택배" to "kr.cjlogistics",
            "우체국" to "kr.epost",
            "우체국택배" to "kr.epost",
            "우체국소포" to "kr.epost",
            "한진" to "kr.hanjin",
            "한진택배" to "kr.hanjin",
            "롯데" to "kr.lotte",
            "롯데택배" to "kr.lotte",
            "롯데글로벌" to "kr.lotte",
            "로젠" to "kr.logen",
            "로젠택배" to "kr.logen",
            "쿠팡" to "kr.coupangls",
            "쿠팡로지스틱스" to "kr.coupangls",
            "로켓배송" to "kr.coupangls",
            "gs" to "kr.cvsnet",
            "gs25" to "kr.cvsnet",
            "반값택배" to "kr.cvsnet",
            "gspostbox" to "kr.cvsnet",
            "cu" to "kr.cupost",
            "cu택배" to "kr.cupost",
            "알뜰택배" to "kr.cupost",
            "세븐일레븐" to "kr.seveneleven",
            "착한택배" to "kr.seveneleven",
            "경동" to "kr.kdexp",
            "경동택배" to "kr.kdexp",
            "대신" to "kr.daesin",
            "대신택배" to "kr.daesin",
            "일양" to "kr.ilyanglogis",
            "일양로지스" to "kr.ilyanglogis",
            "천일" to "kr.chunilps",
            "천일택배" to "kr.chunilps",
            "합동" to "kr.hdexp",
            "합동택배" to "kr.hdexp",
            "건영" to "kr.kunyoung",
            "건영택배" to "kr.kunyoung",
            "ems" to "kr.epost.ems",
            "우체국ems" to "kr.epost.ems",
            "dhl" to "de.dhl",
            "fedex" to "us.fedex",
            "페덱스" to "us.fedex",
            "ups" to "us.ups",
            "차이냐오" to "cn.cainiao.global",
            "cainiao" to "cn.cainiao.global",
            "컬리" to "kr.kurly",
            "마켓컬리" to "kr.kurly",
            "홈픽" to "kr.homepick",
            "오늘의픽업" to "kr.todaypickup"
        )
        aliasMap[norm]?.let { return byIdMap[it] }
        return allCarriers.firstOrNull {
            val n = it.nameKo.lowercase().replace(" ", "")
            val s = it.shortName.lowercase().replace(" ", "")
            n.contains(norm) || s.contains(norm)
        }
    }

    fun inferPhaseFromText(statusText: String): StandardDeliveryPhase {
        val text = statusText.trim()
        if (text.isEmpty()) return StandardDeliveryPhase.PREPARING
        return when {
            text.contains("완료") || text.contains("전달") || text.contains("수취") || text.contains("인수확인") ->
                StandardDeliveryPhase.DELIVERED
            text.contains("미배달") || text.contains("반송") || text.contains("취소") || text.contains("오류") ->
                StandardDeliveryPhase.EXCEPTION
            text.contains("배달출발") || text.contains("배송출발") || text.contains("배송출고") ||
                text.contains("배달준비") || text.contains("배달전") || text.contains("배달중") ||
                text.contains("배송차량") || text.contains("배송기사 인계") ->
                StandardDeliveryPhase.OUT_FOR_DELIVERY
            text.contains("도착") && (text.contains("지점") || text.contains("영업소") || text.contains("캠프") ||
                text.contains("대리점") || text.contains("점포") || text.contains("배달국") ||
                text.contains("배송입고") || text.contains("배달지") || text.contains("해체")) ->
                StandardDeliveryPhase.ARRIVED_AT_BRANCH
            text.contains("상차") || text.contains("하차") || text.contains("발송") || text.contains("도착") ||
                text.contains("터미널") || text.contains("허브") || text.contains("적입") ||
                text.contains("집중국") || text.contains("이동") || text.contains("통관") ||
                text.contains("공항") || text.contains("항공") || text.contains("분류") ->
                StandardDeliveryPhase.IN_TRANSIT
            text.contains("집하") || text.contains("인수") || text.contains("픽업완료") ->
                StandardDeliveryPhase.PICKED_UP
            text.contains("등록") || text.contains("접수") || text.contains("준비") || text.contains("출력") ->
                StandardDeliveryPhase.PREPARING
            else -> StandardDeliveryPhase.IN_TRANSIT
        }
    }

    fun resolveStageForStatus(
        carrier: CarrierInfo,
        statusText: String,
        phase: StandardDeliveryPhase
    ): Int {
        val pipeline = carrier.stagePipeline.ifEmpty { standardDomesticPipeline }
        if (phase == StandardDeliveryPhase.DELIVERED) return pipeline.lastIndex
        val directMatch = pipeline.indexOfFirst { step ->
            val key = step.stageName.substringBefore("·")
            statusText.contains(key) || step.stageName.split("·").any { part -> statusText.contains(part) }
        }
        if (directMatch >= 0) return directMatch
        val phaseMatch = pipeline.indexOfFirst { it.mappedPhase == phase }
        if (phaseMatch >= 0) return phaseMatch
        return phase.stepIndex.coerceIn(0, pipeline.lastIndex)
    }

    fun buildTrackingUrl(carrier: CarrierInfo, waybill: String): String {
        val clean = waybill.trim()
        val template = carrier.trackingUrlTemplate
        if (template.isBlank()) {
            return "https://search.naver.com/search.naver?query=${carrier.shortName}+${clean}"
        }
        return template.replace("{waybill}", clean)
    }
}
