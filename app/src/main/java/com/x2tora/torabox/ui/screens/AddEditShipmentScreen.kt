package com.x2tora.torabox.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.domain.WaybillDetector
import com.x2tora.torabox.model.CarrierCategory
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.TrackingEvent
import com.x2tora.torabox.model.TrackingResult
import com.x2tora.torabox.ui.components.ConfirmActionDialog
import com.x2tora.torabox.ui.components.IconAndColorIdentityRow
import com.x2tora.torabox.ui.components.MiuixBottomEdgeBlur
import com.x2tora.torabox.ui.components.MiuixFloatingScrollTopBar
import com.x2tora.torabox.ui.components.MiuixGlassIcon
import com.x2tora.torabox.ui.components.SquirclePillBadge
import com.x2tora.torabox.ui.components.StackedToastHost
import com.x2tora.torabox.ui.components.StackedToastState
import com.x2tora.torabox.ui.theme.ToraBoxIconCatalog
import com.x2tora.torabox.ui.theme.ToraBoxMotion
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Clear
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Paste
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.util.UUID

@Composable
fun AddEditShipmentScreen(
    prefillWaybill: String?,
    prefillCarrierId: String?,
    prefillItemName: String? = null,
    prefillSenderName: String? = null,
    scannedWaybill: String? = null,
    onConsumeScannedWaybill: () -> Unit = {},
    onOpenScanner: (() -> Unit)? = null,
    isProbing: Boolean,
    toastState: StackedToastState,
    onProbePreview: (waybill: String, carrierId: String?, onResult: (Result<TrackingResult>) -> Unit) -> Unit,
    onSave: (ShipmentRecord) -> Unit,
    onBack: () -> Unit,
    onNotify: (String) -> Unit
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior(state = rememberTopAppBarState())
    val screenBackdrop = rememberLayerBackdrop()
    val surfaceBg = MiuixTheme.colorScheme.surface

    var waybillInput by remember { mutableStateOf(prefillWaybill.orEmpty()) }
    var selectedCarrierId by remember { mutableStateOf(prefillCarrierId) }
    var customTitle by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf(prefillItemName.orEmpty()) }
    var itemQuantity by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf(prefillSenderName.orEmpty()) }

    LaunchedEffect(scannedWaybill) {
        if (!scannedWaybill.isNullOrBlank()) {
            waybillInput = scannedWaybill
            onConsumeScannedWaybill()
        }
    }
    var senderPhone by remember { mutableStateOf("") }
    var senderAddress by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var recipientAddress by remember { mutableStateOf("") }
    var accepterName by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var iconKey by remember {
        mutableStateOf(
            prefillCarrierId?.let { KoreanCarrierCatalog.findById(it)?.iconKey } ?: "box"
        )
    }
    var colorHex by remember {
        mutableLongStateOf(
            prefillCarrierId?.let { KoreanCarrierCatalog.findById(it)?.accentColorHex } ?: 0xFF3482FF
        )
    }
    var previewEvents by remember { mutableStateOf<List<TrackingEvent>>(emptyList()) }
    var previewStatusLabel by remember { mutableStateOf("조회 대기") }
    var previewPhase by remember { mutableStateOf(StandardDeliveryPhase.PREPARING) }
    var previewStageIndex by remember { mutableStateOf(0) }
    var previewSyncSource by remember { mutableStateOf("") }

    var showCarrierPicker by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val parsed = remember(waybillInput) { WaybillDetector.parseNotificationText(waybillInput) }
    val normalizedWaybill = parsed.waybillNumber.ifBlank { WaybillDetector.normalizeWaybill(waybillInput) }
    val recommendations = remember(normalizedWaybill, parsed.carrierHintId) {
        if (normalizedWaybill.length >= 6) {
            WaybillDetector.recommendCarriers(normalizedWaybill, parsed.carrierHintId)
        } else emptyList()
    }
    val chosenCarrier = remember(selectedCarrierId, recommendations) {
        selectedCarrierId?.let { KoreanCarrierCatalog.findById(it) }
            ?: recommendations.firstOrNull()?.carrier
    }

    val isDirty = waybillInput.isNotBlank() || itemName.isNotBlank() || senderName.isNotBlank() || customTitle.isNotBlank()

    BackHandler {
        if (isDirty) showDiscardConfirm = true else onBack()
    }

    fun applyPreviewResult(tr: TrackingResult) {
        selectedCarrierId = tr.carrierId
        KoreanCarrierCatalog.findById(tr.carrierId)?.let { c ->
            iconKey = c.iconKey
            colorHex = c.accentColorHex
        }
        if (tr.itemName.isNotBlank()) itemName = tr.itemName
        if (tr.itemQuantity.isNotBlank()) itemQuantity = tr.itemQuantity
        if (tr.senderName.isNotBlank()) senderName = tr.senderName
        if (tr.senderPhone.isNotBlank()) senderPhone = tr.senderPhone
        if (tr.senderAddress.isNotBlank()) senderAddress = tr.senderAddress
        if (tr.recipientName.isNotBlank()) recipientName = tr.recipientName
        if (tr.recipientPhone.isNotBlank()) recipientPhone = tr.recipientPhone
        if (tr.recipientAddress.isNotBlank()) recipientAddress = tr.recipientAddress
        if (tr.accepterName.isNotBlank()) accepterName = tr.accepterName
        if (tr.serviceType.isNotBlank()) serviceType = tr.serviceType
        previewEvents = tr.events
        previewStatusLabel = tr.currentStatusLabel.ifBlank { tr.currentPhase.label }
        previewPhase = tr.currentPhase
        previewStageIndex = tr.currentCarrierStageIndex
        previewSyncSource = tr.syncSource
    }

    fun saveAction() {
        focusManager.clearFocus()
        if (normalizedWaybill.length < 6) {
            scope.launch { listState.animateScrollToItem(0) }
            onNotify("유효한 운송장 번호를 입력해 주세요")
            return
        }
        val finalCarrier = chosenCarrier
        if (finalCarrier == null) {
            scope.launch { listState.animateScrollToItem(0) }
            onNotify("택배사를 선택해 주세요")
            return
        }
        onSave(
            ShipmentRecord(
                id = UUID.randomUUID().toString(),
                waybillNumber = normalizedWaybill,
                carrierId = finalCarrier.id,
                customTitle = customTitle.trim(),
                itemName = itemName.trim().ifBlank { parsed.itemNameHint },
                itemQuantity = itemQuantity.trim(),
                senderName = senderName.trim().ifBlank { parsed.senderNameHint },
                senderPhone = senderPhone.trim(),
                senderAddress = senderAddress.trim(),
                recipientName = recipientName.trim(),
                recipientPhone = recipientPhone.trim(),
                recipientAddress = recipientAddress.trim(),
                accepterName = accepterName.trim(),
                serviceType = serviceType.trim(),
                currentStatusLabel = previewStatusLabel,
                currentPhase = previewPhase,
                currentCarrierStageIndex = previewStageIndex,
                events = previewEvents,
                iconKey = iconKey,
                colorHex = colorHex,
                memo = memo.trim(),
                createdAtEpochMs = System.currentTimeMillis(),
                lastUpdatedEpochMs = if (previewEvents.isNotEmpty()) System.currentTimeMillis() else 0L,
                lastSyncSource = previewSyncSource
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            MiuixFloatingScrollTopBar(
                title = "새 송장 등록",
                scrollBehavior = scrollBehavior,
                backdrop = screenBackdrop,
                maxContentWidth = 620.dp,
                navigationIcon = {
                    IconButton(
                        onClick = { if (isDirty) showDiscardConfirm = true else onBack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = ::saveAction, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = MiuixIcons.Ok,
                            contentDescription = "저장",
                            tint = MiuixTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(screenBackdrop)
                    .background(surfaceBg)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val sidePad = ((maxWidth - 620.dp) / 2f).coerceAtLeast(16.dp)

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .overScrollVertical()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = sidePad,
                            end = sidePad,
                            top = paddingValues.calculateTopPadding() + 8.dp,
                            bottom = 110.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(key = "waybill_card") {
                            SmallTitle(text = "송장번호 및 택배사")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp,
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextField(
                                            value = waybillInput,
                                            onValueChange = { waybillInput = it },
                                            label = "송장번호 입력 또는 문자 붙여넣기",
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (onOpenScanner != null) {
                                            IconButton(
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    onOpenScanner()
                                                },
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Icon(
                                                    imageVector = MiuixIcons.Scan,
                                                    contentDescription = "바코드 스캔",
                                                    tint = MiuixTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        SquirclePillBadge(
                                            text = "붙여넣기",
                                            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MiuixTheme.colorScheme.primary,
                                            leadingIcon = MiuixIcons.Paste,
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text.orEmpty()
                                                if (clip.isBlank()) onNotify("클립보드가 비어 있습니다")
                                                else waybillInput = clip
                                            }
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = recommendations.isNotEmpty(),
                                        enter = ToraBoxMotion.expandEnter(),
                                        exit = ToraBoxMotion.shrinkExit()
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "추천 택배사 후보",
                                                style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.SemiBold),
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                            )
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(recommendations, key = { it.carrier.id }) { rec ->
                                                    val selected = selectedCarrierId == rec.carrier.id
                                                    SquirclePillBadge(
                                                        text = "${rec.carrier.shortName} ${rec.confidencePercent}%",
                                                        backgroundColor = if (selected) {
                                                            Color(rec.carrier.accentColorHex)
                                                        } else Color(rec.carrier.accentColorHex).copy(alpha = 0.14f),
                                                        contentColor = if (selected) Color.White else Color(rec.carrier.accentColorHex),
                                                        onClick = {
                                                            selectedCarrierId = rec.carrier.id
                                                            iconKey = rec.carrier.iconKey
                                                            colorHex = rec.carrier.accentColorHex
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    ArrowPreference(
                                        title = "택배사 선택",
                                        summary = chosenCarrier?.let { "${it.nameKo} (${it.category.label})" }
                                            ?: "전체 ${KoreanCarrierCatalog.allCarriers.size}개 택배사 중 선택",
                                        onClick = { showCarrierPicker = true }
                                    )

                                    TextButton(
                                        text = if (isProbing) "송장 정보 불러오는 중..." else "송장번호로 물품명·보낸이 자동 조회",
                                        enabled = !isProbing && normalizedWaybill.length >= 6,
                                        onClick = {
                                            focusManager.clearFocus()
                                            onProbePreview(normalizedWaybill, selectedCarrierId) { res ->
                                                res.onSuccess { tr ->
                                                    applyPreviewResult(tr)
                                                    val cName = KoreanCarrierCatalog.findById(tr.carrierId)?.shortName ?: tr.carrierId
                                                    val extras = buildList {
                                                        if (tr.itemName.isNotBlank()) add("물품명")
                                                        if (tr.senderName.isNotBlank()) add("보낸이")
                                                        add("${tr.events.size}건 이력")
                                                    }.joinToString(", ")
                                                    onNotify("$cName 조회 완료 ($extras)")
                                                }.onFailure { err ->
                                                    onNotify(err.message ?: "조회 결과가 없어 직접 입력 후 저장할 수 있습니다")
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.textButtonColorsPrimary(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        item(key = "item_meta_card") {
                            SmallTitle(text = "물품명 · 보낸이 · 메모 (자동 채움 또는 직접 입력)")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp,
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    TextField(
                                        value = itemName,
                                        onValueChange = { itemName = it },
                                        label = "물품명 (예: 무선 이어폰, 정수기 필터)",
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    TextField(
                                        value = senderName,
                                        onValueChange = { senderName = it },
                                        label = "보낸이 / 쇼핑몰",
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    TextField(
                                        value = customTitle,
                                        onValueChange = { customTitle = it },
                                        label = "표시 별칭 (선택)",
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    TextField(
                                        value = memo,
                                        onValueChange = { memo = it },
                                        label = "메모 (선택)",
                                        singleLine = false,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        item(key = "identity_card") {
                            SmallTitle(text = "아이콘 및 색상")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp,
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                IconAndColorIdentityRow(
                                    iconKey = iconKey,
                                    fallbackIcon = MiuixIcons.Scan,
                                    colorHex = colorHex,
                                    onIconChange = { iconKey = it },
                                    onColorChange = { colorHex = it }
                                )
                            }
                        }

                        item(key = "save_btn") {
                            TextButton(
                                text = "배송 저장하기",
                                onClick = ::saveAction,
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            MiuixBottomEdgeBlur(
                backdrop = screenBackdrop,
                extraHeight = 36.dp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            StackedToastHost(
                state = toastState,
                backdrop = screenBackdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            )

            if (showCarrierPicker) {
                CarrierPickerSheet(
                    selectedCarrierId = selectedCarrierId,
                    onDismiss = { showCarrierPicker = false },
                    onSelect = { c ->
                        selectedCarrierId = c.id
                        iconKey = c.iconKey
                        colorHex = c.accentColorHex
                        showCarrierPicker = false
                    }
                )
            }

            ConfirmActionDialog(
                show = showDiscardConfirm,
                title = "작성 취소",
                summary = "입력 중인 송장 정보를 저장하지 않고 나가시겠습니까?",
                confirmText = "나가기",
                isDestructive = true,
                onDismiss = { showDiscardConfirm = false },
                onConfirm = onBack
            )
        }
    }
}

@Composable
private fun CarrierPickerSheet(
    selectedCarrierId: String?,
    onDismiss: () -> Unit,
    onSelect: (com.x2tora.torabox.model.CarrierInfo) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<CarrierCategory?>(null) }
    val filtered = remember(search, category) {
        val q = search.trim()
        KoreanCarrierCatalog.allCarriers.filter { c ->
            (category == null || c.category == category) &&
                (q.isEmpty() || c.nameKo.contains(q, true) || c.shortName.contains(q, true))
        }
    }

    OverlayBottomSheet(
        show = true,
        title = "택배사 선택 (${KoreanCarrierCatalog.allCarriers.size}개)",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextField(
                value = search,
                onValueChange = { search = it },
                label = "택배사 이름 검색",
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = MiuixIcons.Search,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp).size(18.dp)
                    )
                },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(imageVector = MiuixIcons.Clear, contentDescription = "지우기", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SquirclePillBadge(
                        text = "전체",
                        backgroundColor = if (category == null) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (category == null) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        onClick = { category = null }
                    )
                }
                items(CarrierCategory.entries) { cat ->
                    val active = category == cat
                    SquirclePillBadge(
                        text = cat.label,
                        backgroundColor = if (active) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (active) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        onClick = { category = cat }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.id }) { carrier ->
                    val isSelected = carrier.id == selectedCarrierId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(carrier) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiuixGlassIcon(
                            icon = ToraBoxIconCatalog.resolveIcon(carrier.iconKey),
                            contentDescription = null,
                            accentColor = Color(carrier.accentColorHex),
                            size = 34.dp,
                            iconSize = 17.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = carrier.nameKo,
                                style = MiuixTheme.textStyles.body1.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${carrier.category.label} · ${carrier.waybillPatternHint}",
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = MiuixIcons.Ok,
                                contentDescription = "선택됨",
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
