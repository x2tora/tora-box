package com.x2tora.torabox.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.model.CarrierCategory
import com.x2tora.torabox.model.CarrierInfo
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
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarrierDirectoryScreen(
    scrollBehavior: ScrollBehavior,
    topInset: Dp,
    bottomBarExtraPadding: Dp,
    onAddWithCarrier: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CarrierCategory?>(null) }
    var expandedCarrierId by remember { mutableStateOf<String?>("kr.cjlogistics") }

    val filtered = remember(searchQuery, selectedCategory) {
        val q = searchQuery.trim()
        KoreanCarrierCatalog.allCarriers.filter { carrier ->
            val catMatch = selectedCategory == null || carrier.category == selectedCategory
            val textMatch = q.isEmpty() ||
                carrier.nameKo.contains(q, ignoreCase = true) ||
                carrier.shortName.contains(q, ignoreCase = true) ||
                carrier.sweetTrackerCode.contains(q, ignoreCase = true) ||
                carrier.id.contains(q, ignoreCase = true)
            catMatch && textMatch
        }
    }

    val wideDualEnabled = LocalWideDualLayoutEnabled.current
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad = maxOf(bottomBarExtraPadding + 20.dp, 116.dp + navBottomInset)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDualColumn = wideDualEnabled && maxWidth >= 600.dp
        val sidePad = if (isDualColumn) 20.dp else ((maxWidth - 600.dp) / 2f).coerceAtLeast(20.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = sidePad,
                end = sidePad,
                top = topInset + 8.dp,
                bottom = bottomPad
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "directory_search") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "택배사 이름 · 코드 검색 (전체 ${KoreanCarrierCatalog.allCarriers.size}개)",
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = MiuixIcons.Search,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 8.dp).size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
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

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            val active = selectedCategory == null
                            SquirclePillBadge(
                                text = "전체 (${KoreanCarrierCatalog.allCarriers.size})",
                                backgroundColor = if (active) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (active) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                onClick = { selectedCategory = null }
                            )
                        }
                        items(CarrierCategory.entries) { cat ->
                            val count = KoreanCarrierCatalog.allCarriers.count { it.category == cat }
                            val active = selectedCategory == cat
                            SquirclePillBadge(
                                text = "${cat.label} ($count)",
                                backgroundColor = if (active) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (active) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                onClick = { selectedCategory = cat }
                            )
                        }
                    }
                }
            }

            item(key = "directory_count") {
                SmallTitle(text = "지원 택배사 목록 (${filtered.size}개) · 탭하여 배송 단계 확인")
            }

            if (filtered.isEmpty()) {
                item(key = "directory_empty") {
                    EmptyDataBanner(
                        icon = MiuixIcons.ListView,
                        title = "일치하는 택배사가 없습니다",
                        subtitle = "다른 검색어나 분류를 선택해 보세요.",
                        actionLabel = "전체 택배사 보기",
                        onAction = {
                            searchQuery = ""
                            selectedCategory = null
                        }
                    )
                }
            } else if (isDualColumn) {
                val pairs = filtered.chunked(2)
                items(pairs, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        pair.forEach { carrier ->
                            CarrierDirectoryCard(
                                carrier = carrier,
                                expanded = expandedCarrierId == carrier.id,
                                onToggle = {
                                    expandedCarrierId = if (expandedCarrierId == carrier.id) null else carrier.id
                                },
                                onAddWithCarrier = onAddWithCarrier,
                                onDialPhone = { phone ->
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { carrier ->
                    CarrierDirectoryCard(
                        carrier = carrier,
                        expanded = expandedCarrierId == carrier.id,
                        onToggle = {
                            expandedCarrierId = if (expandedCarrierId == carrier.id) null else carrier.id
                        },
                        onAddWithCarrier = onAddWithCarrier,
                        onDialPhone = { phone ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CarrierDirectoryCard(
    carrier: CarrierInfo,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddWithCarrier: (String) -> Unit,
    onDialPhone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(carrier.accentColorHex)

    Card(
        modifier = modifier
            .animateContentSize(animationSpec = ToraBoxMotion.LayoutSizeSpring)
            .clickable(onClick = onToggle),
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
                    size = 40.dp,
                    iconSize = 20.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = carrier.nameKo,
                        style = MiuixTheme.textStyles.headline2.copy(fontWeight = FontWeight.Bold),
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${carrier.category.label} · ${carrier.waybillPatternHint}",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }
                SquirclePillBadge(
                    text = if (carrier.supportsDirectQuery) "직접조회" else "통합지원",
                    backgroundColor = if (carrier.supportsDirectQuery) {
                        Color(0xFF10B981).copy(alpha = 0.14f)
                    } else MiuixTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (carrier.supportsDirectQuery) {
                        Color(0xFF10B981)
                    } else MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = ToraBoxMotion.expandEnter(),
                exit = ToraBoxMotion.shrinkExit()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider()

                    Text(
                        text = "${carrier.shortName} 배송 진행단계 (${carrier.stagePipeline.size}단계)",
                        style = MiuixTheme.textStyles.footnote1.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = accent
                    )

                    CarrierStageStepper(
                        stages = carrier.stagePipeline,
                        currentStageIndex = 2,
                        currentPhase = StandardDeliveryPhase.IN_TRANSIT,
                        accentColor = accent
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        carrier.stagePipeline.forEach { step ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${step.index + 1}. ${step.stageName}",
                                    style = MiuixTheme.textStyles.footnote1.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = step.description,
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SquirclePillBadge(
                            text = "코드 ${carrier.sweetTrackerCode}",
                            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                        if (carrier.supportsDirectItemName) {
                            SquirclePillBadge(
                                text = "물품명 제공",
                                backgroundColor = Color(0xFF10B981).copy(alpha = 0.13f),
                                contentColor = Color(0xFF10B981)
                            )
                        }
                        if (carrier.supportsDirectSender) {
                            SquirclePillBadge(
                                text = "보낸이 제공",
                                backgroundColor = Color(0xFF3482FF).copy(alpha = 0.13f),
                                contentColor = Color(0xFF3482FF)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (carrier.phone.isNotBlank()) {
                            TextButton(
                                text = "전화 ${carrier.phone}",
                                onClick = { onDialPhone(carrier.phone) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        TextButton(
                            text = "이 택배사로 송장 등록",
                            onClick = { onAddWithCarrier(carrier.id) },
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        }
    }
}
