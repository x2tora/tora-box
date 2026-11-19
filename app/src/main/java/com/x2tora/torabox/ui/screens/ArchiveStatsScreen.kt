package com.x2tora.torabox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.x2tora.torabox.domain.DeliveryIntelligence
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.ToraBoxDataState
import com.x2tora.torabox.ui.components.EmptyDataBanner
import com.x2tora.torabox.ui.components.SelectionActionBar
import com.x2tora.torabox.ui.components.ShipmentSummaryCard
import com.x2tora.torabox.ui.components.SquirclePillBadge
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Clear
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun ArchiveStatsScreen(
    state: ToraBoxDataState,
    refreshingIds: Set<String>,
    scrollBehavior: ScrollBehavior,
    topInset: Dp,
    bottomBarExtraPadding: Dp,
    onOpenShipmentDetail: (String) -> Unit,
    onRefreshShipment: (String) -> Unit,
    isSelectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onEnterSelectionMode: (String) -> Unit = {},
    onToggleSelect: (String) -> Unit = {},
    onSelectAll: (List<String>) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onRestoreSelected: () -> Unit = {},
    onRequestDeleteSelected: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var scopeTab by remember { mutableIntStateOf(0) } // 0=전체, 1=보관함, 2=배송완료

    val allShipments = state.shipments
    val totalCount = allShipments.size
    val deliveredCount = remember(allShipments) {
        allShipments.count { it.currentPhase == StandardDeliveryPhase.DELIVERED }
    }
    val archivedCount = remember(allShipments) {
        allShipments.count { it.isArchived }
    }
    val topCarrierSummary = remember(allShipments) {
        allShipments.groupingBy { it.carrierId }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .mapNotNull { (id, cnt) ->
                val name = KoreanCarrierCatalog.findById(id)?.shortName ?: id
                "$name ${cnt}건"
            }
            .joinToString(" · ")
            .ifBlank { "기록 없음" }
    }

    val filtered = remember(allShipments, query, scopeTab) {
        val q = query.trim()
        allShipments.filter { s ->
            val scopeOk = when (scopeTab) {
                1 -> s.isArchived
                2 -> s.currentPhase == StandardDeliveryPhase.DELIVERED
                else -> true
            }
            val carrierName = KoreanCarrierCatalog.findById(s.carrierId)?.nameKo.orEmpty()
            val qOk = q.isEmpty() ||
                s.itemName.contains(q, ignoreCase = true) ||
                s.customTitle.contains(q, ignoreCase = true) ||
                s.senderName.contains(q, ignoreCase = true) ||
                s.recipientName.contains(q, ignoreCase = true) ||
                s.waybillNumber.contains(q, ignoreCase = true) ||
                carrierName.contains(q, ignoreCase = true)
            scopeOk && qOk
        }.sortedByDescending { maxOf(it.lastUpdatedEpochMs, it.createdAtEpochMs) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sidePad = ((maxWidth - 600.dp) / 2f).coerceAtLeast(16.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = sidePad,
                end = sidePad,
                top = topInset + 8.dp,
                bottom = bottomBarExtraPadding + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "stats_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    insideMargin = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "누적 배송 요약",
                            style = MiuixTheme.textStyles.title4.copy(fontWeight = FontWeight.Bold),
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatBox("전체 송장", "${totalCount}건", MiuixTheme.colorScheme.primary, Modifier.weight(1f))
                            StatBox("배송 완료", "${deliveredCount}건", Color(0xFF16A34A), Modifier.weight(1f))
                            StatBox("보관함", "${archivedCount}건", Color(0xFF64748B), Modifier.weight(1f))
                        }
                        Text(
                            text = "주요 이용 택배사: $topCarrierSummary",
                            style = MiuixTheme.textStyles.footnote2.copy(fontFeatureSettings = "tnum"),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                        )
                    }
                }
            }

            if (state.shipments.isNotEmpty()) {
                item(key = "insights_card") {
                    val topCarriers = remember(state.shipments) {
                        state.shipments
                            .groupBy { it.carrierId }
                            .entries
                            .sortedByDescending { it.value.size }
                            .take(4)
                    }
                    val maxCarrierCount = (topCarriers.maxOfOrNull { it.value.size } ?: 1).coerceAtLeast(1)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        insideMargin = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "택배사별 이용 · 평균 배송 속도",
                                style = MiuixTheme.textStyles.headline2.copy(fontWeight = FontWeight.Bold),
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            topCarriers.forEach { (carrierId, records) ->
                                val carrier = KoreanCarrierCatalog.findById(carrierId)
                                val name = carrier?.shortName ?: carrierId
                                val avgHours = DeliveryIntelligence.computeAverageDeliveryHours(state.shipments, carrierId)
                                val ratio = (records.size.toFloat() / maxCarrierCount.toFloat()).coerceIn(0.12f, 1f)
                                val barColor = Color(carrier?.accentColorHex ?: 0xFF3482FF)

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = name,
                                            style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.SemiBold),
                                            color = MiuixTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (avgHours != null) {
                                                "${records.size}건 · 평균 ${avgHours.toInt()}시간"
                                            } else {
                                                "${records.size}건"
                                            },
                                            style = MiuixTheme.textStyles.footnote2.copy(fontFeatureSettings = "tnum"),
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(ratio)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(barColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "archive_search") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        label = "물품명 · 보낸이 · 송장번호 검색",
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = MiuixIcons.Search,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 8.dp).size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
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
                        itemsIndexed(listOf("전체 기록", "보관함", "배송완료")) { idx, title ->
                            val selected = scopeTab == idx
                            SquirclePillBadge(
                                text = title,
                                backgroundColor = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                onClick = { scopeTab = idx }
                            )
                        }
                    }
                }
            }

            if (isSelectionMode) {
                item(key = "archive_selection_bar") {
                    val visibleIds = remember(filtered) { filtered.map { it.id } }
                    val allSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedIds }
                    SelectionActionBar(
                        selectedCount = selectedIds.size,
                        allSelected = allSelected,
                        archiveLabel = if (scopeTab == 1) "활성 복원" else "보관",
                        onToggleSelectAll = {
                            if (allSelected) onSelectAll(emptyList()) else onSelectAll(visibleIds)
                        },
                        onArchiveSelected = onRestoreSelected,
                        onDeleteSelected = onRequestDeleteSelected,
                        onExitSelection = onClearSelection
                    )
                }
            }

            item(key = "archive_title") {
                SmallTitle(text = "조회 내역 (${filtered.size}건)")
            }

            if (filtered.isEmpty()) {
                item(key = "archive_empty") {
                    EmptyDataBanner(
                        icon = MiuixIcons.Folder,
                        title = "표시할 배송 기록이 없습니다",
                        subtitle = "물품명이나 보낸이, 송장번호로 이전 배송 내역을 검색할 수 있습니다."
                    )
                }
            } else {
                items(filtered, key = { it.id }) { shipment ->
                    ShipmentSummaryCard(
                        shipment = shipment,
                        isRefreshing = shipment.id in refreshingIds,
                        selectionMode = isSelectionMode,
                        isSelected = shipment.id in selectedIds,
                        onClick = {
                            if (isSelectionMode) onToggleSelect(shipment.id)
                            else onOpenShipmentDetail(shipment.id)
                        },
                        onLongClick = {
                            if (!isSelectionMode) onEnterSelectionMode(shipment.id)
                            else onToggleSelect(shipment.id)
                        },
                        onRefresh = { onRefreshShipment(shipment.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MiuixTheme.textStyles.title3.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = color
        )
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
    }
}
