package com.x2tora.torabox.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.domain.WaybillDetector
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.ui.components.CarrierStageStepper
import com.x2tora.torabox.ui.components.EmptyDataBanner
import com.x2tora.torabox.ui.components.MiuixGlassIcon
import com.x2tora.torabox.ui.components.SquirclePillBadge
import com.x2tora.torabox.ui.theme.LocalWideDualLayoutEnabled
import com.x2tora.torabox.ui.theme.ToraBoxIconCatalog
import com.x2tora.torabox.ui.theme.ToraBoxMotion
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Clear
import top.yukonga.miuix.kmp.icon.extended.Paste
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartDetectScreen(
    isProbing: Boolean,
    scrollBehavior: ScrollBehavior,
    topInset: Dp,
    bottomBarExtraPadding: Dp,
    onProbeAndAdd: (waybill: String, carrierId: String?, itemHint: String, senderHint: String) -> Unit,
    onNotify: (String) -> Unit
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    var rawText by remember { mutableStateOf("") }

    val extracted = remember(rawText) { WaybillDetector.parseNotificationText(rawText) }
    val recommendations = remember(extracted.waybillNumber, extracted.carrierHintId) {
        if (extracted.waybillNumber.length >= 6) {
            WaybillDetector.recommendCarriers(extracted.waybillNumber, extracted.carrierHintId)
        } else emptyList()
    }
    val hintCarrier = remember(extracted.carrierHintId) {
        extracted.carrierHintId?.let { KoreanCarrierCatalog.findById(it) }
    }

    val wideDualEnabled = LocalWideDualLayoutEnabled.current
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad = maxOf(bottomBarExtraPadding + 20.dp, 116.dp + navBottomInset)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDualColumn = wideDualEnabled && maxWidth >= 600.dp
        val singleSidePad = ((maxWidth - 600.dp) / 2f).coerceAtLeast(20.dp)

        val inputContent: LazyListScope.() -> Unit = {
            item(key = "analyzer_input") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = ToraBoxMotion.LayoutSizeSpring),
                    cornerRadius = 20.dp,
                    insideMargin = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "송장번호 · 알림톡 스마트 분석",
                                    style = MiuixTheme.textStyles.title4.copy(fontWeight = FontWeight.Bold),
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "문자·카톡 배송 안내 통째로 붙여넣으면 번호·택배사·물품명을 추출합니다",
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                            SquirclePillBadge(
                                text = "붙여넣기",
                                backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.14f),
                                contentColor = MiuixTheme.colorScheme.primary,
                                leadingIcon = MiuixIcons.Paste,
                                onClick = {
                                    val clip = clipboardManager.getText()?.text.orEmpty()
                                    if (clip.isBlank()) onNotify("클립보드가 비어 있습니다")
                                    else rawText = clip
                                }
                            )
                        }

                        TextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            label = "송장번호 또는 배송 안내 메시지 입력",
                            singleLine = false,
                            trailingIcon = {
                                if (rawText.isNotEmpty()) {
                                    IconButton(onClick = { rawText = "" }) {
                                        Icon(
                                            imageVector = MiuixIcons.Clear,
                                            contentDescription = "지우기",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedVisibility(
                            visible = extracted.waybillNumber.isNotBlank(),
                            enter = ToraBoxMotion.expandEnter(),
                            exit = ToraBoxMotion.shrinkExit()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "추출된 송장번호",
                                        style = MiuixTheme.textStyles.footnote1,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                    Text(
                                        text = extracted.waybillNumber,
                                        style = MiuixTheme.textStyles.headline2.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = MiuixTheme.colorScheme.primary
                                    )
                                }
                                if (hintCarrier != null) {
                                    InfoLine("감지된 택배사 키워드", hintCarrier.nameKo)
                                }
                                if (extracted.itemNameHint.isNotBlank()) {
                                    InfoLine("메시지 내 상품명", extracted.itemNameHint)
                                }
                                if (extracted.senderNameHint.isNotBlank()) {
                                    InfoLine("메시지 내 보낸이", extracted.senderNameHint)
                                }
                                TextButton(
                                    text = if (isProbing) "후보 택배사 동시 탐색 중..." else "후보 택배사 동시 탐색 및 등록",
                                    enabled = !isProbing,
                                    onClick = {
                                        focusManager.clearFocus()
                                        onProbeAndAdd(
                                            extracted.waybillNumber,
                                            null,
                                            extracted.itemNameHint,
                                            extracted.senderNameHint
                                        )
                                    },
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        val recommendationsContent: LazyListScope.() -> Unit = {
            if (recommendations.isNotEmpty()) {
                item(key = "rec_header") {
                    SmallTitle(text = "자릿수 · 체크섬 · 대역 기반 추천 (${recommendations.size}개 후보)")
                }

                items(recommendations, key = { it.carrier.id }) { rec ->
                    val carrier = rec.carrier
                    val accent = Color(carrier.accentColorHex)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus()
                                onProbeAndAdd(
                                    extracted.waybillNumber,
                                    carrier.id,
                                    extracted.itemNameHint,
                                    extracted.senderNameHint
                                )
                            },
                        cornerRadius = 18.dp,
                        insideMargin = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MiuixGlassIcon(
                                    icon = ToraBoxIconCatalog.resolveIcon(carrier.iconKey),
                                    contentDescription = carrier.nameKo,
                                    accentColor = accent,
                                    size = 42.dp,
                                    iconSize = 20.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = carrier.nameKo,
                                        style = MiuixTheme.textStyles.headline2.copy(fontWeight = FontWeight.Bold),
                                        color = MiuixTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = carrier.waybillPatternHint,
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                }
                                SquirclePillBadge(
                                    text = "일치도 ${rec.confidencePercent}%",
                                    backgroundColor = accent.copy(alpha = 0.14f),
                                    contentColor = accent
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rec.reasonBadges.forEach { reason ->
                                    SquirclePillBadge(
                                        text = reason,
                                        backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                }
                                if (carrier.supportsDirectItemName) {
                                    SquirclePillBadge(
                                        text = "물품명 추출 지원",
                                        backgroundColor = Color(0xFF10B981).copy(alpha = 0.13f),
                                        contentColor = Color(0xFF10B981)
                                    )
                                }
                                if (carrier.supportsDirectSender) {
                                    SquirclePillBadge(
                                        text = "보낸이 추출 지원",
                                        backgroundColor = Color(0xFF3482FF).copy(alpha = 0.13f),
                                        contentColor = Color(0xFF3482FF)
                                    )
                                }
                            }

                            CarrierStageStepper(
                                stages = carrier.stagePipeline,
                                currentStageIndex = 0,
                                currentPhase = StandardDeliveryPhase.PREPARING,
                                accentColor = accent,
                                compact = true
                            )
                        }
                    }
                }
            } else {
                item(key = "rec_empty") {
                    EmptyDataBanner(
                        icon = MiuixIcons.Scan,
                        title = "분석할 송장번호를 입력해 보세요",
                        subtitle = "12자리(CJ·롯데·한진 Mod-7 체크섬), 13자리(우체국·대신·경동·UPU S10), 11자리(로젠), 14자리(쿠팡 CLS), 직구 송장(LP/1Z)을 자동으로 분석해 추천합니다."
                    )
                }
            }
        }

        if (isDualColumn) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(top = topInset + 8.dp, bottom = bottomPad),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = inputContent
                )
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(top = topInset + 8.dp, bottom = bottomPad),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = recommendationsContent
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = singleSidePad,
                    end = singleSidePad,
                    top = topInset + 8.dp,
                    bottom = bottomPad
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                inputContent()
                recommendationsContent()
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.SemiBold),
            color = MiuixTheme.colorScheme.onSurface
        )
    }
}
