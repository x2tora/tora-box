package com.x2tora.torabox.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.domain.WaybillDetector
import com.x2tora.torabox.model.HomeSortOption
import com.x2tora.torabox.model.PendingCaptureItem
import com.x2tora.torabox.model.ShipmentFilter
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.SwipeActionOption
import com.x2tora.torabox.model.ToraBoxDataState
import com.x2tora.torabox.ui.components.EmptyDataBanner
import com.x2tora.torabox.ui.components.MiuixGlassIcon
import com.x2tora.torabox.ui.components.SelectionActionBar
import com.x2tora.torabox.ui.components.ShipmentSummaryCard
import com.x2tora.torabox.ui.components.SquirclePillBadge
import com.x2tora.torabox.ui.components.SwipeableShipmentCard
import com.x2tora.torabox.ui.theme.LocalWideDualLayoutEnabled
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
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Paste
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun HomeScreen(
    listState: LazyListState,
    state: ToraBoxDataState,
    refreshingIds: Set<String>,
    isProbing: Boolean,
    scrollBehavior: ScrollBehavior,
    topInset: Dp,
    bottomBarExtraPadding: Dp,
    clipboardCandidateWaybill: String?,
    onDismissClipboardBanner: () -> Unit,
    onQuickTrackAndAdd: (waybill: String, carrierId: String?) -> Unit,
    onOpenAddWithPrefill: (waybill: String?, carrierId: String?) -> Unit,
    onOpenShipmentDetail: (String) -> Unit,
    onRefreshShipment: (String) -> Unit,
    onRefreshAllActive: () -> Unit,
    onLoadSampleShipments: () -> Unit,
    onNavigateToSmartDetect: () -> Unit,
    onNotify: (String) -> Unit,
    isSelectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onEnterSelectionMode: (String) -> Unit = {},
    onToggleSelect: (String) -> Unit = {},
    onSelectAll: (List<String>) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onArchiveSelected: () -> Unit = {},
    onRequestDeleteSelected: () -> Unit = {},
    onOpenScanner: () -> Unit = {},
    onArchiveAllDelivered: () -> Unit = {},
    onSwipeArchive: (String) -> Unit = {},
    onTogglePin: (String) -> Unit = {},
    onRequestDeleteSingle: (ShipmentRecord) -> Unit = {},
    onAcceptPendingCapture: (PendingCaptureItem) -> Unit = {},
    onDismissPendingCapture: (String) -> Unit = {},
    onChangeSortOption: (HomeSortOption) -> Unit = {}
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val wideDualEnabled = LocalWideDualLayoutEnabled.current

    var quickInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var activeFilter by remember { mutableStateOf<ShipmentFilter>(ShipmentFilter.ALL) }
    var pinnedOnly by remember { mutableStateOf(false) }

    val parsedInput = remember(quickInput) { WaybillDetector.parseNotificationText(quickInput) }
    val normalizedWaybill = parsedInput.waybillNumber
    val recommendations = remember(normalizedWaybill, parsedInput.carrierHintId) {
        if (normalizedWaybill.length >= 6) {
            WaybillDetector.recommendCarriers(normalizedWaybill, parsedInput.carrierHintId)
        } else emptyList()
    }
    var selectedCarrierId by remember(normalizedWaybill, recommendations) {
        mutableStateOf<String?>(null)
    }

    val activeShipments = remember(state.shipments) {
        state.shipments.filter { !it.isArchived }
    }
    val inTransitCount = remember(activeShipments) {
        activeShipments.count {
            it.currentPhase in setOf(
                StandardDeliveryPhase.PREPARING,
                StandardDeliveryPhase.PICKED_UP,
                StandardDeliveryPhase.IN_TRANSIT,
                StandardDeliveryPhase.ARRIVED_AT_BRANCH
            )
        }
    }
    val outForDeliveryCount = remember(activeShipments) {
        activeShipments.count { it.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY }
    }
    val deliveredCount = remember(activeShipments) {
        activeShipments.count { it.currentPhase == StandardDeliveryPhase.DELIVERED }
    }
    val pinnedCount = remember(activeShipments) {
        activeShipments.count { it.isPinned }
    }
    val availableTags = remember(activeShipments) {
        activeShipments.map { it.groupTag.trim() }.filter { it.isNotEmpty() }.distinct()
    }
    val sortOption = state.settings.homeSortOption

    val filteredShipments = remember(activeShipments, activeFilter, pinnedOnly, searchQuery, selectedTag, sortOption) {
        val q = searchQuery.trim()
        val baseList = activeShipments.filter { item ->
            val pinOk = !pinnedOnly || item.isPinned
            val tagOk = selectedTag == null || item.groupTag.trim() == selectedTag
            val queryOk = if (q.isEmpty()) true else {
                val carrierName = KoreanCarrierCatalog.findById(item.carrierId)?.nameKo.orEmpty()
                item.waybillNumber.contains(q, ignoreCase = true) ||
                    item.itemName.contains(q, ignoreCase = true) ||
                    item.customTitle.contains(q, ignoreCase = true) ||
                    item.senderName.contains(q, ignoreCase = true) ||
                    item.memo.contains(q, ignoreCase = true) ||
                    item.groupTag.contains(q, ignoreCase = true) ||
                    carrierName.contains(q, ignoreCase = true)
            }
            pinOk && tagOk && queryOk
        }
        val list = when (activeFilter) {
            ShipmentFilter.ALL -> baseList
            ShipmentFilter.IN_TRANSIT -> baseList.filter {
                it.currentPhase in setOf(
                    StandardDeliveryPhase.PREPARING,
                    StandardDeliveryPhase.PICKED_UP,
                    StandardDeliveryPhase.IN_TRANSIT,
                    StandardDeliveryPhase.ARRIVED_AT_BRANCH
                )
            }
            ShipmentFilter.OUT_FOR_DELIVERY -> baseList.filter {
                it.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY
            }
            ShipmentFilter.DELIVERED -> baseList.filter {
                it.currentPhase == StandardDeliveryPhase.DELIVERED
            }
        }
        val baseComparator = compareByDescending<ShipmentRecord> { it.isPinned }
        when (sortOption) {
            HomeSortOption.LATEST -> list.sortedWith(
                baseComparator
                    .thenBy { it.currentPhase == StandardDeliveryPhase.DELIVERED }
                    .thenByDescending { maxOf(it.lastUpdatedEpochMs, it.createdAtEpochMs) }
            )
            HomeSortOption.OUT_FOR_DELIVERY_FIRST -> list.sortedWith(
                baseComparator
                    .thenByDescending { it.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY }
                    .thenBy { it.currentPhase == StandardDeliveryPhase.DELIVERED }
                    .thenByDescending { maxOf(it.lastUpdatedEpochMs, it.createdAtEpochMs) }
            )
            HomeSortOption.ETA_SOONEST -> list.sortedWith(
                baseComparator
                    .thenByDescending { it.currentPhase.stepIndex.takeIf { idx -> idx <= 5 } ?: -1 }
                    .thenByDescending { maxOf(it.lastUpdatedEpochMs, it.createdAtEpochMs) }
            )
            HomeSortOption.NAME -> list.sortedWith(
                baseComparator.thenBy {
                    val carrierShort = KoreanCarrierCatalog.findById(it.carrierId)?.shortName ?: it.carrierId
                    it.displayTitle(carrierShort)
                }
            )
        }
    }

    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad = maxOf(bottomBarExtraPadding + 20.dp, 116.dp + navBottomInset)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDualColumn = wideDualEnabled && maxWidth >= 600.dp
        val sidePad = if (isDualColumn) 20.dp else ((maxWidth - 600.dp) / 2f).coerceAtLeast(20.dp)

        LazyColumn(
            state = listState,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!clipboardCandidateWaybill.isNullOrBlank()) {
                item(key = "clipboard_banner") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        insideMargin = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiuixGlassIcon(
                                icon = MiuixIcons.Scan,
                                contentDescription = null,
                                accentColor = MiuixTheme.colorScheme.primary,
                                size = 38.dp,
                                iconSize = 18.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "클립보드 송장 감지됨",
                                    style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.SemiBold),
                                    color = MiuixTheme.colorScheme.primary
                                )
                                Text(
                                    text = clipboardCandidateWaybill,
                                    style = MiuixTheme.textStyles.body2.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                            }
                            SquirclePillBadge(
                                text = "바로 조회",
                                backgroundColor = MiuixTheme.colorScheme.primary,
                                contentColor = MiuixTheme.colorScheme.onPrimary,
                                onClick = {
                                    onDismissClipboardBanner()
                                    onQuickTrackAndAdd(clipboardCandidateWaybill, null)
                                }
                            )
                            IconButton(
                                onClick = onDismissClipboardBanner,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Clear,
                                    contentDescription = "닫기",
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            val latestPending = state.pendingCaptures.firstOrNull()
            if (latestPending != null) {
                item(key = "pending_capture_banner") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        insideMargin = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiuixGlassIcon(
                                icon = MiuixIcons.Scan,
                                contentDescription = null,
                                accentColor = Color(0xFF10B981),
                                size = 38.dp,
                                iconSize = 18.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${latestPending.sourceAppLabel} 알림 송장 (${state.pendingCaptures.size}건)",
                                    style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = listOf(latestPending.itemNameHint, latestPending.waybillNumber)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" · "),
                                    style = MiuixTheme.textStyles.body2.copy(fontWeight = FontWeight.Bold),
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                            SquirclePillBadge(
                                text = "등록",
                                backgroundColor = Color(0xFF10B981),
                                contentColor = Color.White,
                                onClick = { onAcceptPendingCapture(latestPending) }
                            )
                            IconButton(
                                onClick = { onDismissPendingCapture(latestPending.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Clear,
                                    contentDescription = "무시",
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item(key = "quick_track_hero") {
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
                                    text = "빠른 송장 조회",
                                    style = MiuixTheme.textStyles.title4.copy(fontWeight = FontWeight.Bold),
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "송장번호나 택배 문자 붙여넣기 시 택배사 자동 추천",
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                            SquirclePillBadge(
                                text = "문자 분석기",
                                backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
                                contentColor = MiuixTheme.colorScheme.onSecondaryContainer,
                                leadingIcon = MiuixIcons.Scan,
                                onClick = onNavigateToSmartDetect
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = quickInput,
                                onValueChange = { quickInput = it },
                                label = "운송장 번호 입력 (하이픈·공백 자동 정리)",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        focusManager.clearFocus()
                                        if (normalizedWaybill.isNotBlank()) {
                                            onQuickTrackAndAdd(normalizedWaybill, selectedCarrierId)
                                        }
                                    }
                                ),
                                trailingIcon = {
                                    if (quickInput.isNotEmpty()) {
                                        IconButton(onClick = { quickInput = "" }) {
                                            Icon(
                                                imageVector = MiuixIcons.Clear,
                                                contentDescription = "지우기",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .squircleBackground(MiuixTheme.colorScheme.surfaceContainerHigh, 14.dp)
                                    .squircleClip(14.dp)
                                    .clickable {
                                        focusManager.clearFocus()
                                        onOpenScanner()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Scan,
                                    contentDescription = "바코드 스캔",
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .squircleBackground(MiuixTheme.colorScheme.surfaceContainerHigh, 14.dp)
                                    .squircleClip(14.dp)
                                    .clickable {
                                        val clipText = clipboardManager.getText()?.text.orEmpty()
                                        if (clipText.isBlank()) {
                                            onNotify("클립보드가 비어 있습니다")
                                        } else {
                                            quickInput = clipText
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Paste,
                                    contentDescription = "붙여넣기",
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = recommendations.isNotEmpty(),
                            enter = ToraBoxMotion.expandEnter(),
                            exit = ToraBoxMotion.shrinkExit()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "추천 택배사 (${normalizedWaybill})",
                                    style = MiuixTheme.textStyles.footnote1.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        val autoSelected = selectedCarrierId == null
                                        SquirclePillBadge(
                                            text = "자동 탐지",
                                            backgroundColor = if (autoSelected) {
                                                MiuixTheme.colorScheme.primary
                                            } else MiuixTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = if (autoSelected) {
                                                MiuixTheme.colorScheme.onPrimary
                                            } else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                            onClick = { selectedCarrierId = null }
                                        )
                                    }
                                    items(recommendations, key = { it.carrier.id }) { rec ->
                                        val active = selectedCarrierId == rec.carrier.id
                                        val label = if (rec.isChecksumVerified) {
                                            "${rec.carrier.shortName} ${rec.confidencePercent}% · 검증됨"
                                        } else {
                                            "${rec.carrier.shortName} ${rec.confidencePercent}%"
                                        }
                                        SquirclePillBadge(
                                            text = label,
                                            backgroundColor = if (active) {
                                                Color(rec.carrier.accentColorHex)
                                            } else Color(rec.carrier.accentColorHex).copy(alpha = 0.13f),
                                            contentColor = if (active) Color.White else Color(rec.carrier.accentColorHex),
                                            onClick = { selectedCarrierId = rec.carrier.id }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                text = "직접 등록",
                                onClick = {
                                    focusManager.clearFocus()
                                    onOpenAddWithPrefill(
                                        normalizedWaybill.ifBlank { null },
                                        selectedCarrierId ?: recommendations.firstOrNull()?.carrier?.id
                                    )
                                },
                                modifier = Modifier.weight(0.9f)
                            )
                            TextButton(
                                text = if (isProbing) "조회 중..." else "조회 및 추가",
                                enabled = !isProbing && normalizedWaybill.length >= 6,
                                onClick = {
                                    focusManager.clearFocus()
                                    onQuickTrackAndAdd(normalizedWaybill, selectedCarrierId)
                                },
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                                modifier = Modifier.weight(1.4f)
                            )
                        }
                    }
                }
            }

            // Status Summary Strip
            item(key = "summary_strip") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusBentoChip(
                        label = "전체",
                        count = activeShipments.size,
                        selected = activeFilter == ShipmentFilter.ALL,
                        accentColor = MiuixTheme.colorScheme.primary,
                        onClick = { activeFilter = ShipmentFilter.ALL },
                        modifier = Modifier.weight(1f)
                    )
                    StatusBentoChip(
                        label = "이동중",
                        count = inTransitCount,
                        selected = activeFilter == ShipmentFilter.IN_TRANSIT,
                        accentColor = Color(0xFF0EA5E9),
                        onClick = { activeFilter = ShipmentFilter.IN_TRANSIT },
                        modifier = Modifier.weight(1f)
                    )
                    StatusBentoChip(
                        label = "배송출발",
                        count = outForDeliveryCount,
                        selected = activeFilter == ShipmentFilter.OUT_FOR_DELIVERY,
                        accentColor = Color(0xFFF97316),
                        onClick = { activeFilter = ShipmentFilter.OUT_FOR_DELIVERY },
                        modifier = Modifier.weight(1f)
                    )
                    StatusBentoChip(
                        label = "완료",
                        count = deliveredCount,
                        selected = activeFilter == ShipmentFilter.DELIVERED,
                        accentColor = Color(0xFF16A34A),
                        onClick = { activeFilter = ShipmentFilter.DELIVERED },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (activeShipments.isNotEmpty() && (pinnedCount > 0 || deliveredCount > 0 || availableTags.isNotEmpty() || activeShipments.size >= 2) && !isSelectionMode) {
                item(key = "home_search_and_tags") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (activeShipments.size >= 2 || searchQuery.isNotEmpty()) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = "내 배송 검색 (물품명·송장·보낸이·태그)",
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = MiuixIcons.Search,
                                        contentDescription = null,
                                        modifier = Modifier.padding(start = 12.dp).size(18.dp),
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(imageVector = MiuixIcons.Clear, contentDescription = "지우기", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item(key = "sort_cycle") {
                                val nextSort = HomeSortOption.entries[(sortOption.ordinal + 1) % HomeSortOption.entries.size]
                                SquirclePillBadge(
                                    text = "정렬: ${sortOption.label}",
                                    backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MiuixTheme.colorScheme.onSurface,
                                    onClick = { onChangeSortOption(nextSort) }
                                )
                            }
                            if (pinnedCount > 0) {
                                item(key = "filter_pinned") {
                                    SquirclePillBadge(
                                        text = if (pinnedOnly) "고정됨 ${pinnedCount} · 해제" else "고정됨 ${pinnedCount}",
                                        backgroundColor = if (pinnedOnly) {
                                            MiuixTheme.colorScheme.primary
                                        } else MiuixTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = if (pinnedOnly) {
                                            MiuixTheme.colorScheme.onPrimary
                                        } else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        onClick = { pinnedOnly = !pinnedOnly }
                                    )
                                }
                            }
                            items(availableTags, key = { "tag_$it" }) { tag ->
                                val tagSelected = selectedTag == tag
                                SquirclePillBadge(
                                    text = "#$tag",
                                    backgroundColor = if (tagSelected) {
                                        MiuixTheme.colorScheme.primary
                                    } else MiuixTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (tagSelected) {
                                        MiuixTheme.colorScheme.onPrimary
                                    } else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    onClick = { selectedTag = if (tagSelected) null else tag }
                                )
                            }
                            if (deliveredCount > 0) {
                                item(key = "archive_all_delivered") {
                                    SquirclePillBadge(
                                        text = "배송완료 ${deliveredCount}건 한 번에 보관",
                                        backgroundColor = Color(0xFF16A34A).copy(alpha = 0.14f),
                                        contentColor = Color(0xFF16A34A),
                                        onClick = onArchiveAllDelivered
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (activeShipments.isNotEmpty()) {
                if (isSelectionMode) {
                    item(key = "selection_bar") {
                        val visibleIds = remember(filteredShipments) { filteredShipments.map { it.id } }
                        val allSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selectedIds }
                        SelectionActionBar(
                            selectedCount = selectedIds.size,
                            allSelected = allSelected,
                            archiveLabel = "보관",
                            onToggleSelectAll = {
                                if (allSelected) onSelectAll(emptyList()) else onSelectAll(visibleIds)
                            },
                            onArchiveSelected = onArchiveSelected,
                            onDeleteSelected = onRequestDeleteSelected,
                            onExitSelection = onClearSelection
                        )
                    }
                }
                item(key = "active_section_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallTitle(text = "진행중인 배송 (${filteredShipments.size})")
                        if (!isSelectionMode) {
                            SquirclePillBadge(
                                text = if (refreshingIds.isNotEmpty()) "새로고침 중..." else "전체 새로고침",
                                backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MiuixTheme.colorScheme.primary,
                                leadingIcon = MiuixIcons.Refresh,
                                onClick = onRefreshAllActive
                            )
                        }
                    }
                }
            }

            if (activeShipments.isEmpty()) {
                item(key = "empty_home") {
                    EmptyDataBanner(
                        icon = MiuixIcons.Layers,
                        title = "등록된 배송이 없습니다",
                        subtitle = "위 입력창에 송장번호를 넣으면 택배사를 자동 추측해 물품명과 보낸이 정보까지 함께 불러옵니다.",
                        accentColor = MiuixTheme.colorScheme.primary,
                        actionLabel = "송장 직접 등록하기",
                        onAction = { onOpenAddWithPrefill(null, null) },
                        secondaryActionLabel = "샘플 배송 3건 체험하기",
                        onSecondaryAction = onLoadSampleShipments
                    )
                }
            } else if (filteredShipments.isEmpty()) {
                item(key = "empty_filter") {
                    EmptyDataBanner(
                        icon = MiuixIcons.Layers,
                        title = "해당 단계의 배송이 없습니다",
                        subtitle = "다른 배송 단계 필터를 선택해 확인해 보세요.",
                        accentColor = MiuixTheme.colorScheme.primary,
                        actionLabel = "전체 보기",
                        onAction = { activeFilter = ShipmentFilter.ALL }
                    )
                }
            } else if (isDualColumn) {
                val pairs = filteredShipments.chunked(2)
                items(pairs, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        pair.forEach { item ->
                            val rightOpt = state.settings.swipeRightAction
                            val leftOpt = state.settings.swipeLeftAction
                            SwipeableShipmentCard(
                                enabled = !isSelectionMode,
                                rightActionLabel = rightOpt.label,
                                leftActionLabel = leftOpt.label,
                                onSwipeRight = {
                                    when (rightOpt) {
                                        SwipeActionOption.ARCHIVE -> onSwipeArchive(item.id)
                                        SwipeActionOption.PIN -> onTogglePin(item.id)
                                        SwipeActionOption.DELETE -> onRequestDeleteSingle(item)
                                    }
                                },
                                onSwipeLeft = {
                                    when (leftOpt) {
                                        SwipeActionOption.ARCHIVE -> onSwipeArchive(item.id)
                                        SwipeActionOption.PIN -> onTogglePin(item.id)
                                        SwipeActionOption.DELETE -> onRequestDeleteSingle(item)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                ShipmentSummaryCard(
                                    shipment = item,
                                    isRefreshing = item.id in refreshingIds,
                                    selectionMode = isSelectionMode,
                                    isSelected = item.id in selectedIds,
                                    onClick = {
                                        if (isSelectionMode) onToggleSelect(item.id)
                                        else onOpenShipmentDetail(item.id)
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) onEnterSelectionMode(item.id)
                                        else onToggleSelect(item.id)
                                    },
                                    onRefresh = { onRefreshShipment(item.id) }
                                )
                            }
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            } else {
                items(filteredShipments, key = { it.id }) { shipment ->
                    val rightOpt = state.settings.swipeRightAction
                    val leftOpt = state.settings.swipeLeftAction
                    SwipeableShipmentCard(
                        enabled = !isSelectionMode,
                        rightActionLabel = rightOpt.label,
                        leftActionLabel = leftOpt.label,
                        onSwipeRight = {
                            when (rightOpt) {
                                SwipeActionOption.ARCHIVE -> onSwipeArchive(shipment.id)
                                SwipeActionOption.PIN -> onTogglePin(shipment.id)
                                SwipeActionOption.DELETE -> onRequestDeleteSingle(shipment)
                            }
                        },
                        onSwipeLeft = {
                            when (leftOpt) {
                                SwipeActionOption.ARCHIVE -> onSwipeArchive(shipment.id)
                                SwipeActionOption.PIN -> onTogglePin(shipment.id)
                                SwipeActionOption.DELETE -> onRequestDeleteSingle(shipment)
                            }
                        },
                        modifier = Modifier.animateItem()
                    ) {
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
                            onRefresh = { onRefreshShipment(shipment.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBentoChip(
    label: String,
    count: Int,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = count.toString(),
                style = MiuixTheme.textStyles.title3.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = if (selected) accentColor else MiuixTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MiuixTheme.textStyles.footnote2.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) accentColor else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
