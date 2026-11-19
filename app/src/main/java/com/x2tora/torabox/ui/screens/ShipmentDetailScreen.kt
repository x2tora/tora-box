package com.x2tora.torabox.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.notification.LiveUpdateNotificationManager
import com.x2tora.torabox.ui.components.CarrierStageStepper
import com.x2tora.torabox.ui.components.ConfirmActionDialog
import com.x2tora.torabox.ui.components.EmptyDataBanner
import com.x2tora.torabox.ui.components.MiuixBottomEdgeBlur
import com.x2tora.torabox.ui.components.MiuixFloatingScrollTopBar
import com.x2tora.torabox.ui.components.MiuixGlassIcon
import com.x2tora.torabox.ui.components.MiuixTopBarActionDivider
import com.x2tora.torabox.ui.components.SquirclePillBadge
import com.x2tora.torabox.ui.components.StackedToastHost
import com.x2tora.torabox.ui.components.StackedToastState
import com.x2tora.torabox.ui.components.phaseSemanticColor
import com.x2tora.torabox.ui.theme.ToraBoxIconCatalog
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
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.icon.extended.Pin
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Unpin
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShipmentDetailScreen(
    shipment: ShipmentRecord?,
    isRefreshing: Boolean,
    toastState: StackedToastState,
    onBack: () -> Unit,
    onRefresh: (String) -> Unit,
    onUpdateRecord: (ShipmentRecord) -> Unit,
    onTogglePin: (String) -> Unit,
    onToggleArchive: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNotify: (String) -> Unit,
    doorLockHint: String = "",
    onUpdateGroupTag: (String, String) -> Unit = { _, _ -> },
    onAddPhotoUri: (String, String) -> Unit = { _, _ -> },
    onRemovePhotoUri: (String, String) -> Unit = { _, _ -> },
    onToggleLiveUpdate: (String) -> Unit = {}
) {
    if (shipment == null) {
        LaunchedEffectOnMissing(onBack)
        return
    }

    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val carrier = remember(shipment.carrierId) { KoreanCarrierCatalog.findById(shipment.carrierId) }
    val carrierShort = carrier?.shortName ?: shipment.carrierId
    val semanticColor = phaseSemanticColor(shipment.currentPhase)
    val accentColor = Color(shipment.colorHex)
    val pipeline = carrier?.stagePipeline.orEmpty()
    val scrollBehavior = MiuixScrollBehavior(state = rememberTopAppBarState())
    val detailBackdrop = rememberLayerBackdrop()
    val surfaceBg = MiuixTheme.colorScheme.surface

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var didNavigateBack by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onAddPhotoUri(shipment.id, uri.toString())
            onNotify("수령·영수증 사진을 첨부했습니다")
        }
    }
    val safeBack: () -> Unit = {
        if (!didNavigateBack) {
            didNavigateBack = true
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            MiuixFloatingScrollTopBar(
                title = shipment.displayTitle(carrierShort),
                scrollBehavior = scrollBehavior,
                backdrop = detailBackdrop,
                maxContentWidth = 620.dp,
                navigationIcon = {
                    IconButton(onClick = safeBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "뒤로가기",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onRefresh(shipment.id) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Refresh,
                            contentDescription = "배송 새로고침",
                            tint = if (isRefreshing) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                        )
                    }
                    MiuixTopBarActionDivider()
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = "배송 삭제",
                            tint = MiuixTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(detailBackdrop)
                    .background(surfaceBg)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val sidePad = ((maxWidth - 620.dp) / 2f).coerceAtLeast(16.dp)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
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
                        // Hero Card + Carrier-Specific Progress Stepper
                        item(key = "hero_detail") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 20.dp,
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MiuixGlassIcon(
                                            icon = ToraBoxIconCatalog.resolveIcon(shipment.iconKey),
                                            contentDescription = null,
                                            accentColor = accentColor,
                                            size = 48.dp,
                                            iconSize = 24.dp
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = shipment.displayTitle(carrierShort),
                                                style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.Bold),
                                                color = MiuixTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        clipboardManager.setText(AnnotatedString(shipment.waybillNumber))
                                                        onNotify("송장번호를 복사했습니다")
                                                    }
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${carrier?.nameKo ?: shipment.carrierId} · ${shipment.waybillNumber}",
                                                    style = MiuixTheme.textStyles.footnote1.copy(fontFeatureSettings = "tnum"),
                                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                                )
                                                Icon(
                                                    imageVector = MiuixIcons.Copy,
                                                    contentDescription = "복사",
                                                    tint = MiuixTheme.colorScheme.primary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                        SquirclePillBadge(
                                            text = shipment.currentStatusLabel.ifBlank { shipment.currentPhase.label },
                                            backgroundColor = semanticColor.copy(alpha = 0.14f),
                                            contentColor = semanticColor
                                        )
                                    }

                                    val anomaly = shipment.anomalyType
                                    if (anomaly != null || shipment.estimatedDeliveryLabel.isNotBlank() || shipment.lastSyncSource.isNotBlank()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (anomaly != null) {
                                                SquirclePillBadge(
                                                    text = "${anomaly.badgeText} · ${anomaly.label}",
                                                    backgroundColor = Color(0xFFDC2626).copy(alpha = 0.14f),
                                                    contentColor = Color(0xFFDC2626)
                                                )
                                            }
                                            if (shipment.estimatedDeliveryLabel.isNotBlank()) {
                                                SquirclePillBadge(
                                                    text = shipment.estimatedDeliveryLabel,
                                                    backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    contentColor = MiuixTheme.colorScheme.primary
                                                )
                                            }
                                            if (shipment.lastSyncSource.isNotBlank()) {
                                                val diag = if (shipment.lastLatencyMs > 0L) {
                                                    "${shipment.lastSyncSource} · ${shipment.lastLatencyMs}ms"
                                                } else shipment.lastSyncSource
                                                SquirclePillBadge(
                                                    text = diag,
                                                    backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                                    contentColor = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                                )
                                            }
                                        }
                                    }

                                    // Group tag quick selector
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("개인", "가족", "직구", "반품").forEach { tag ->
                                            val activeTag = shipment.groupTag == tag
                                            SquirclePillBadge(
                                                text = "#$tag",
                                                backgroundColor = if (activeTag) {
                                                    MiuixTheme.colorScheme.primary
                                                } else MiuixTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = if (activeTag) {
                                                    MiuixTheme.colorScheme.onPrimary
                                                } else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                onClick = {
                                                    onUpdateGroupTag(shipment.id, if (activeTag) "" else tag)
                                                }
                                            )
                                        }
                                    }

                                    if (pipeline.isNotEmpty()) {
                                        HorizontalDivider()
                                        val activeStep = pipeline.getOrNull(shipment.currentCarrierStageIndex.coerceIn(0, pipeline.lastIndex))
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${carrierShort} 배송 진행단계",
                                                    style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MiuixTheme.colorScheme.onSurface
                                                )
                                                if (activeStep != null) {
                                                    Text(
                                                        text = "${activeStep.index + 1}/${pipeline.size} · ${activeStep.description}",
                                                        style = MiuixTheme.textStyles.footnote2.copy(fontFeatureSettings = "tnum"),
                                                        color = semanticColor
                                                    )
                                                }
                                            }

                                            CarrierStageStepper(
                                                stages = pipeline,
                                                currentStageIndex = shipment.currentCarrierStageIndex,
                                                currentPhase = shipment.currentPhase,
                                                accentColor = semanticColor,
                                                compact = false
                                            )
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SquirclePillBadge(
                                            text = if (isRefreshing) "조회 중..." else "지금 새로고침",
                                            backgroundColor = MiuixTheme.colorScheme.primary,
                                            contentColor = MiuixTheme.colorScheme.onPrimary,
                                            leadingIcon = MiuixIcons.Refresh,
                                            onClick = { onRefresh(shipment.id) }
                                        )
                                        SquirclePillBadge(
                                            text = if (shipment.isPinned) "상단 고정 해제" else "상단 고정",
                                            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MiuixTheme.colorScheme.onSurface,
                                            leadingIcon = if (shipment.isPinned) MiuixIcons.Unpin else MiuixIcons.Pin,
                                            onClick = {
                                                onTogglePin(shipment.id)
                                                onNotify(if (shipment.isPinned) "고정을 해제했습니다" else "홈 상단에 고정했습니다")
                                            }
                                        )
                                        if (LiveUpdateNotificationManager.isSupported() &&
                                            !shipment.isArchived &&
                                            shipment.currentPhase != StandardDeliveryPhase.DELIVERED
                                        ) {
                                            val liveActive = shipment.isLiveUpdatePinned ||
                                                shipment.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY
                                            SquirclePillBadge(
                                                text = if (shipment.isLiveUpdatePinned) "실시간 알림 중" else "실시간 알림 고정",
                                                backgroundColor = if (liveActive) {
                                                    MiuixTheme.colorScheme.primary.copy(alpha = 0.14f)
                                                } else MiuixTheme.colorScheme.surfaceContainerHigh,
                                                contentColor = if (liveActive) {
                                                    MiuixTheme.colorScheme.primary
                                                } else MiuixTheme.colorScheme.onSurface,
                                                onClick = {
                                                    onToggleLiveUpdate(shipment.id)
                                                    onNotify(
                                                        if (shipment.isLiveUpdatePinned) "실시간 알림 고정을 해제했습니다"
                                                        else "상태바·잠금화면 Live Update로 고정했습니다"
                                                    )
                                                }
                                            )
                                        }
                                        SquirclePillBadge(
                                            text = if (shipment.isArchived) "보관 해제" else "보관함 이동",
                                            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MiuixTheme.colorScheme.onSurface,
                                            leadingIcon = MiuixIcons.Folder,
                                            onClick = {
                                                onToggleArchive(shipment.id)
                                                onNotify(if (shipment.isArchived) "활성 목록으로 되돌렸습니다" else "보관함으로 이동했습니다")
                                            }
                                        )
                                        SquirclePillBadge(
                                            text = "이름·메모 편집",
                                            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MiuixTheme.colorScheme.onSurface,
                                            leadingIcon = MiuixIcons.Edit,
                                            onClick = { showEditDialog = true }
                                        )
                                        SquirclePillBadge(
                                            text = "부재 메시지 복사",
                                            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MiuixTheme.colorScheme.onSurface,
                                            leadingIcon = MiuixIcons.Copy,
                                            onClick = {
                                                val msg = buildString {
                                                    append("안녕하세요, 택배 기사님. 부재 중이니 문 앞에 안전하게 놓아주세요. 감사합니다.")
                                                    if (doorLockHint.isNotBlank()) {
                                                        append(" (출입 안내: ").append(doorLockHint).append(")")
                                                    }
                                                }
                                                @Suppress("DEPRECATION")
                                                clipboardManager.setText(AnnotatedString(msg))
                                                onNotify("부재 요청 문구를 복사했습니다")
                                            }
                                        )
                                        SquirclePillBadge(
                                            text = if (shipment.photoUris.isEmpty()) "사진 첨부" else "사진 ${shipment.photoUris.size}장",
                                            backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MiuixTheme.colorScheme.onSurface,
                                            onClick = { photoPickerLauncher.launch(arrayOf("image/*")) }
                                        )
                                        if (carrier != null) {
                                            SquirclePillBadge(
                                                text = "공식 웹 조회",
                                                backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
                                                contentColor = MiuixTheme.colorScheme.onSecondaryContainer,
                                                leadingIcon = MiuixIcons.Link,
                                                onClick = {
                                                    val url = KoreanCarrierCatalog.buildTrackingUrl(carrier, shipment.waybillNumber)
                                                    runCatching {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                    }.onFailure {
                                                        onNotify("브라우저를 열 수 없습니다")
                                                    }
                                                }
                                            )
                                        }
                                        SquirclePillBadge(
                                            text = "삭제",
                                            backgroundColor = MiuixTheme.colorScheme.error.copy(alpha = 0.14f),
                                            contentColor = MiuixTheme.colorScheme.error,
                                            leadingIcon = MiuixIcons.Delete,
                                            onClick = { showDeleteConfirm = true }
                                        )
                                    }
                                }
                            }
                        }

                        // Extracted Waybill Metadata Card (물품명, 보낸이, 받는이, 수령정보)
                        item(key = "metadata_card") {
                            SmallTitle(text = "송장 추출 상세 정보")
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 18.dp,
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    MetadataRow("물품명", shipment.itemName.ifBlank { "택배사 미제공 · 직접 입력 가능" })
                                    if (shipment.itemQuantity.isNotBlank()) {
                                        MetadataRow("수량", shipment.itemQuantity)
                                    }
                                    HorizontalDivider()
                                    MetadataRow("보낸이", shipment.senderName.ifBlank { "정보 없음" })
                                    if (shipment.senderPhone.isNotBlank()) {
                                        MetadataRow("보낸이 연락처", shipment.senderPhone)
                                    }
                                    if (shipment.senderAddress.isNotBlank()) {
                                        MetadataRow("보낸곳", shipment.senderAddress)
                                    }
                                    HorizontalDivider()
                                    MetadataRow("받는이", shipment.recipientName.ifBlank { "정보 없음" })
                                    if (shipment.recipientAddress.isNotBlank()) {
                                        MetadataRow("받는곳", shipment.recipientAddress)
                                    }
                                    if (shipment.accepterName.isNotBlank()) {
                                        MetadataRow("인수자·수령", shipment.accepterName)
                                    }
                                    if (shipment.serviceType.isNotBlank()) {
                                        MetadataRow("서비스 구분", shipment.serviceType)
                                    }
                                    if (shipment.originWaybill.isNotBlank()) {
                                        MetadataRow("원운송장", shipment.originWaybill)
                                    }
                                    if (shipment.returnWaybill.isNotBlank()) {
                                        MetadataRow("반품운송장", shipment.returnWaybill)
                                    }
                                    if (shipment.memo.isNotBlank()) {
                                        HorizontalDivider()
                                        MetadataRow("내 메모", shipment.memo)
                                    }
                                    if (shipment.photoUris.isNotEmpty()) {
                                        HorizontalDivider()
                                        Text(
                                            text = "첨부 사진 (${shipment.photoUris.size})",
                                            style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.SemiBold),
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            shipment.photoUris.forEachIndexed { idx, uriStr ->
                                                SquirclePillBadge(
                                                    text = "사진 ${idx + 1} 보기",
                                                    backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                                    contentColor = MiuixTheme.colorScheme.onSurface,
                                                    onClick = {
                                                        runCatching {
                                                            context.startActivity(
                                                                Intent(Intent.ACTION_VIEW).apply {
                                                                    setDataAndType(Uri.parse(uriStr), "image/*")
                                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                }
                                                            )
                                                        }.onFailure {
                                                            onNotify("사진을 열 수 없어 목록에서 제거했습니다")
                                                            onRemovePhotoUri(shipment.id, uriStr)
                                                        }
                                                    }
                                                )
                                                SquirclePillBadge(
                                                    text = "제거",
                                                    backgroundColor = Color(0xFFDC2626).copy(alpha = 0.12f),
                                                    contentColor = Color(0xFFDC2626),
                                                    onClick = {
                                                        onRemovePhotoUri(shipment.id, uriStr)
                                                        onNotify("첨부 사진을 제거했습니다")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    if (shipment.lastUpdatedEpochMs > 0L) {
                                        val syncTime = remember(shipment.lastUpdatedEpochMs) {
                                            SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
                                                .format(Date(shipment.lastUpdatedEpochMs))
                                        }
                                        val sourceText = listOf(shipment.lastSyncSource, syncTime)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" · ")
                                        MetadataRow("최근 동기화", sourceText)
                                    }
                                }
                            }
                        }

                        // Events Timeline
                        item(key = "timeline_header") {
                            SmallTitle(text = "배송 이동 이력 (${shipment.events.size}건)")
                        }

                        if (shipment.events.isEmpty()) {
                            item(key = "timeline_empty") {
                                EmptyDataBanner(
                                    icon = MiuixIcons.Send,
                                    title = "아직 등록된 이동 이력이 없습니다",
                                    subtitle = "상단 '지금 새로고침'을 눌러 택배사에서 최신 배송 단계를 불러오세요.",
                                    actionLabel = if (isRefreshing) "조회 중..." else "지금 배송 조회하기",
                                    onAction = { onRefresh(shipment.id) }
                                )
                            }
                        } else {
                            itemsIndexed(shipment.events, key = { idx, ev -> "${idx}_${ev.displayTime}_${ev.statusName}" }) { idx, event ->
                                val isLatest = idx == 0
                                val evColor = phaseSemanticColor(event.phase)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 18.dp,
                                    insideMargin = PaddingValues(15.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(if (isLatest) 12.dp else 8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isLatest) evColor else evColor.copy(alpha = 0.50f))
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SquirclePillBadge(
                                                    text = event.statusName.ifBlank { event.phase.label },
                                                    backgroundColor = evColor.copy(alpha = 0.14f),
                                                    contentColor = evColor
                                                )
                                                if (event.displayTime.isNotBlank()) {
                                                    Text(
                                                        text = event.displayTime,
                                                        style = MiuixTheme.textStyles.footnote2.copy(fontFeatureSettings = "tnum"),
                                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                                    )
                                                }
                                            }

                                            if (event.location.isNotBlank()) {
                                                Text(
                                                    text = event.location,
                                                    style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MiuixTheme.colorScheme.onSurface
                                                )
                                            }
                                            if (event.description.isNotBlank()) {
                                                Text(
                                                    text = event.description,
                                                    style = MiuixTheme.textStyles.body2,
                                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                                )
                                            }

                                            val contacts = listOfNotNull(
                                                event.branchPhone.takeIf { it.isNotBlank() }?.let { "지점 $it" to it },
                                                event.driverPhone.takeIf { it.isNotBlank() }?.let {
                                                    val label = if (event.driverName.isNotBlank()) "${event.driverName} 기사님 $it" else "기사님 $it"
                                                    label to it
                                                }
                                            )
                                            if (contacts.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    contacts.forEach { (label, tel) ->
                                                        SquirclePillBadge(
                                                            text = label,
                                                            backgroundColor = MiuixTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                            contentColor = MiuixTheme.colorScheme.primary,
                                                            leadingIcon = MiuixIcons.Phone,
                                                            onClick = {
                                                                runCatching {
                                                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            MiuixBottomEdgeBlur(
                backdrop = detailBackdrop,
                extraHeight = 36.dp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            StackedToastHost(
                state = toastState,
                backdrop = detailBackdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            )

            ConfirmActionDialog(
                show = showDeleteConfirm,
                title = "송장 삭제",
                summary = "'${shipment.displayTitle(carrierShort)}' (${shipment.waybillNumber}) 기록을 삭제할까요?",
                confirmText = "삭제",
                isDestructive = true,
                onDismiss = { showDeleteConfirm = false },
                onConfirm = {
                    onDelete(shipment.id)
                    safeBack()
                }
            )

            if (showEditDialog) {
                var aliasText by remember(shipment.id) { mutableStateOf(shipment.customTitle) }
                var itemText by remember(shipment.id) { mutableStateOf(shipment.itemName) }
                var senderText by remember(shipment.id) { mutableStateOf(shipment.senderName) }
                var memoText by remember(shipment.id) { mutableStateOf(shipment.memo) }

                OverlayDialog(
                    show = true,
                    title = "배송 정보 편집",
                    onDismissRequest = { showEditDialog = false }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextField(
                            value = aliasText,
                            onValueChange = { aliasText = it },
                            label = "사용자 별칭 (비워두면 물품명 사용)",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = itemText,
                            onValueChange = { itemText = it },
                            label = "물품명",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = senderText,
                            onValueChange = { senderText = it },
                            label = "보낸이",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = memoText,
                            onValueChange = { memoText = it },
                            label = "메모",
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(text = "취소", onClick = { showEditDialog = false }, modifier = Modifier.weight(1f))
                            TextButton(
                                text = "저장",
                                onClick = {
                                    onUpdateRecord(
                                        shipment.copy(
                                            customTitle = aliasText.trim(),
                                            itemName = itemText.trim(),
                                            senderName = senderText.trim(),
                                            memo = memoText.trim()
                                        )
                                    )
                                    showEditDialog = false
                                    onNotify("배송 정보를 수정했습니다")
                                },
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchedEffectOnMissing(onBack: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onBack()
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2.copy(
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            ),
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
