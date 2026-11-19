package com.x2tora.torabox.ui.screens

import android.app.Activity
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.domain.WaybillDetector
import com.x2tora.torabox.model.NavBarStyle
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.ThemeColorStyle
import com.x2tora.torabox.ui.components.AppUpdateDialog
import com.x2tora.torabox.ui.components.ConfirmActionDialog
import com.x2tora.torabox.ui.components.FloatingLiquidGlassNavBar
import com.x2tora.torabox.ui.components.MiuixBottomEdgeBlur
import com.x2tora.torabox.ui.components.MiuixFloatingScrollTopBar
import com.x2tora.torabox.ui.components.StackedToastHost
import com.x2tora.torabox.ui.components.StackedToastState
import com.x2tora.torabox.ui.components.rememberStackedToastState
import com.x2tora.torabox.ui.theme.LocalAppBackdrop
import com.x2tora.torabox.ui.theme.LocalIsDarkTheme
import com.x2tora.torabox.ui.theme.LocalLiquidGlassEnabled
import com.x2tora.torabox.ui.theme.LocalWideDualLayoutEnabled
import com.x2tora.torabox.ui.theme.ToraBoxMotion
import com.x2tora.torabox.viewmodel.ToraBoxViewModel
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Paste
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Main : AppRoute

    @Serializable
    data class ShipmentDetail(val shipmentId: String) : AppRoute

    @Serializable
    data class AddEditShipment(
        val prefillWaybill: String? = null,
        val prefillCarrierId: String? = null,
        val prefillItemName: String? = null,
        val prefillSenderName: String? = null
    ) : AppRoute

    @Serializable
    data class BarcodeScanner(val fromAddEdit: Boolean = false) : AppRoute

    @Serializable
    data object Settings : AppRoute
}

@Composable
fun MainScaffoldScreen(
    viewModel: ToraBoxViewModel,
    pendingSharedText: String? = null,
    onConsumeSharedText: () -> Unit = {},
    pendingShipmentId: String? = null,
    onConsumeShipmentId: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshingIds by viewModel.refreshingIds.collectAsStateWithLifecycle()
    val isProbing by viewModel.isProbingWaybill.collectAsStateWithLifecycle()
    val updateUiState by viewModel.updateUiState.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack<AppRoute>(AppRoute.Main)
    val toastState = rememberStackedToastState()
    val surfaceBg = MiuixTheme.colorScheme.surface
    val notify: (String) -> Unit = { toastState.show(it) }
    var scannedWaybillForAddEdit by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingShipmentId) {
        if (!pendingShipmentId.isNullOrBlank()) {
            backStack.add(AppRoute.ShipmentDetail(pendingShipmentId))
            onConsumeShipmentId()
        }
    }

    LaunchedEffect(pendingSharedText) {
        if (!pendingSharedText.isNullOrBlank()) {
            val parsed = WaybillDetector.parseNotificationText(pendingSharedText)
            if (parsed.waybillNumber.length >= 6) {
                backStack.add(
                    AppRoute.AddEditShipment(
                        prefillWaybill = parsed.waybillNumber,
                        prefillCarrierId = parsed.carrierHintId,
                        prefillItemName = parsed.itemNameHint.ifBlank { null },
                        prefillSenderName = parsed.senderNameHint.ifBlank { null }
                    )
                )
                notify("공유된 메시지에서 송장번호를 불러왔습니다")
            } else {
                notify("공유된 내용에서 송장번호를 찾지 못했습니다")
            }
            onConsumeSharedText()
        }
    }

    val ambientBackdrop = rememberLayerBackdrop {
        drawRect(surfaceBg)
        drawContent()
    }
    val barBackdrop = rememberLayerBackdrop {
        drawRect(surfaceBg)
        drawContent()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(ambientBackdrop)
                .background(surfaceBg)
        )

        CompositionLocalProvider(
            LocalAppBackdrop provides ambientBackdrop,
            LocalLiquidGlassEnabled provides (state.settings.navBarStyle == NavBarStyle.LIQUID_GLASS),
            LocalWideDualLayoutEnabled provides state.settings.wideDualLayoutEnabled
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                },
                transition = NavTransitions.MiuixDefault
            ) {
                entry<AppRoute.Main> {
                    MainTabsHost(
                        viewModel = viewModel,
                        barBackdrop = barBackdrop,
                        toastState = toastState,
                        onOpenDetail = { id -> backStack.add(AppRoute.ShipmentDetail(id)) },
                        onOpenAdd = { waybill, carrierId ->
                            backStack.add(AppRoute.AddEditShipment(waybill, carrierId))
                        },
                        onOpenScanner = {
                            backStack.add(AppRoute.BarcodeScanner(fromAddEdit = false))
                        },
                        onOpenSettings = { backStack.add(AppRoute.Settings) }
                    )
                }

                entry<AppRoute.ShipmentDetail> { route ->
                    val shipment = state.shipments.find { it.id == route.shipmentId }
                    ShipmentDetailScreen(
                        shipment = shipment,
                        isRefreshing = route.shipmentId in refreshingIds,
                        toastState = toastState,
                        onBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        },
                        onRefresh = { id ->
                            viewModel.refreshShipment(id) { _, msg -> notify(msg) }
                        },
                        onUpdateRecord = viewModel::upsertShipment,
                        onTogglePin = viewModel::togglePin,
                        onToggleArchive = viewModel::toggleArchive,
                        onDelete = { id ->
                            viewModel.deleteShipment(id)
                            notify("송장을 삭제했습니다")
                        },
                        onNotify = notify
                    )
                }

                entry<AppRoute.AddEditShipment> { route ->
                    AddEditShipmentScreen(
                        prefillWaybill = route.prefillWaybill,
                        prefillCarrierId = route.prefillCarrierId,
                        prefillItemName = route.prefillItemName,
                        prefillSenderName = route.prefillSenderName,
                        scannedWaybill = scannedWaybillForAddEdit,
                        onConsumeScannedWaybill = { scannedWaybillForAddEdit = null },
                        onOpenScanner = {
                            backStack.add(AppRoute.BarcodeScanner(fromAddEdit = true))
                        },
                        isProbing = isProbing,
                        toastState = toastState,
                        onProbePreview = viewModel::probeMetadataPreview,
                        onSave = { record ->
                            viewModel.upsertShipment(record)
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                            backStack.add(AppRoute.ShipmentDetail(record.id))
                            notify("새 송장을 등록했습니다")
                        },
                        onBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        },
                        onNotify = notify
                    )
                }

                entry<AppRoute.BarcodeScanner> { route ->
                    BarcodeScannerScreen(
                        onBarcodeDetected = { waybill ->
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                            if (route.fromAddEdit) {
                                scannedWaybillForAddEdit = waybill
                            } else {
                                val guessedCarrier = WaybillDetector.recommendCarriers(waybill)
                                    .firstOrNull()?.carrier?.id
                                backStack.add(
                                    AppRoute.AddEditShipment(
                                        prefillWaybill = waybill,
                                        prefillCarrierId = guessedCarrier
                                    )
                                )
                            }
                            notify("스캔된 송장: $waybill")
                        },
                        onBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        }
                    )
                }

                entry<AppRoute.Settings> {
                    SettingsScreenHost(
                        viewModel = viewModel,
                        toastState = toastState,
                        onBack = {
                            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainTabsHost(
    viewModel: ToraBoxViewModel,
    barBackdrop: LayerBackdrop,
    toastState: StackedToastState,
    onOpenDetail: (String) -> Unit,
    onOpenAdd: (String?, String?) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshingIds by viewModel.refreshingIds.collectAsStateWithLifecycle()
    val isProbing by viewModel.isProbingWaybill.collectAsStateWithLifecycle()
    val updateUiState by viewModel.updateUiState.collectAsStateWithLifecycle()
    val surfaceBg = MiuixTheme.colorScheme.surface
    val hostContext = LocalContext.current
    val focusManager = LocalFocusManager.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0=배송, 1=추측, 2=택배사, 3=보관함
    var showQuickMenu by remember { mutableStateOf(false) }
    var lastBackPressedAtMs by remember { mutableLongStateOf(0L) }
    var measuredTopBarHeightDp by remember { mutableStateOf(0.dp) }
    var clipboardCandidate by remember { mutableStateOf<String?>(null) }
    var dismissedClipWaybill by remember { mutableStateOf<String?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var pendingSingleDelete by remember { mutableStateOf<com.x2tora.torabox.model.ShipmentRecord?>(null) }

    val clearSelection: () -> Unit = {
        isSelectionMode = false
        selectedIds = emptySet()
    }
    val enterSelectionMode: (String) -> Unit = { id ->
        isSelectionMode = true
        selectedIds = setOf(id)
    }
    val toggleSelection: (String) -> Unit = { id ->
        val updated = if (id in selectedIds) selectedIds - id else selectedIds + id
        selectedIds = updated
        if (updated.isEmpty()) {
            isSelectionMode = false
        }
    }

    val homeListState = rememberLazyListState()
    val notify: (String) -> Unit = { toastState.show(it) }

    // Detect unregistered waybill on clipboard on resume
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, state.settings.autoDetectClipboardWaybill, state.shipments) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && state.settings.autoDetectClipboardWaybill) {
                val clip = clipboardManager.getText()?.text.orEmpty()
                val parsed = WaybillDetector.parseNotificationText(clip)
                val wb = parsed.waybillNumber
                if (wb.length in 10..20 &&
                    wb != dismissedClipWaybill &&
                    state.shipments.none { it.waybillNumber == wb }
                ) {
                    clipboardCandidate = wb
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        when {
            showQuickMenu -> showQuickMenu = false
            isSelectionMode -> clearSelection()
            selectedTab != 0 -> {
                selectedTab = 0
                lastBackPressedAtMs = 0L
            }
            else -> {
                val now = SystemClock.elapsedRealtime()
                if (now - lastBackPressedAtMs <= 2_000L) {
                    (hostContext as? Activity)?.finish()
                } else {
                    lastBackPressedAtMs = now
                    notify("뒤로가기를 한 번 더 누르면 앱이 종료됩니다")
                }
            }
        }
    }

    val activeCount = remember(state.shipments) {
        state.shipments.count { !it.isArchived && it.currentPhase != StandardDeliveryPhase.DELIVERED }
    }
    val outForDeliveryCount = remember(state.shipments) {
        state.shipments.count { !it.isArchived && it.currentPhase == StandardDeliveryPhase.OUT_FOR_DELIVERY }
    }

    val tabScrollBehaviors = List(4) { MiuixScrollBehavior(state = rememberTopAppBarState()) }
    val activeScrollBehavior = tabScrollBehaviors[selectedTab]
    val defaultTopBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 66.dp
    val resolvedTopBarInset = if (measuredTopBarHeightDp > 0.dp) measuredTopBarHeightDp else defaultTopBarHeight

    val isLiquidGlassNav = state.settings.navBarStyle == NavBarStyle.LIQUID_GLASS
    val bottomBarExtraPadding = if (isLiquidGlassNav) 104.dp else 90.dp

    val quickMenuMorphProgress = remember { Animatable(0f) }
    var isQuickMenuMounted by remember { mutableStateOf(false) }
    LaunchedEffect(showQuickMenu) {
        if (showQuickMenu) {
            isQuickMenuMounted = true
            quickMenuMorphProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 185f)
            )
        } else {
            quickMenuMorphProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.84f, stiffness = 210f)
            )
            isQuickMenuMounted = false
        }
    }

    val tabTitles = listOf("토라박스", "스마트 추측", "택배사 도감", "보관함 · 통계")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        ) {
            // ① Capture layer ONLY
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(barBackdrop)
                    .background(surfaceBg)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        ToraBoxMotion.tabTransition(forward = targetState >= initialState, slideDivisor = 12)
                    },
                    label = "mainTabTransition"
                ) { tab ->
                    when (tab) {
                        0 -> HomeScreen(
                            listState = homeListState,
                            state = state,
                            refreshingIds = refreshingIds,
                            isProbing = isProbing,
                            scrollBehavior = tabScrollBehaviors[0],
                            topInset = resolvedTopBarInset,
                            bottomBarExtraPadding = bottomBarExtraPadding,
                            clipboardCandidateWaybill = clipboardCandidate,
                            onDismissClipboardBanner = {
                                dismissedClipWaybill = clipboardCandidate
                                clipboardCandidate = null
                            },
                            onQuickTrackAndAdd = { waybill, carrierId ->
                                viewModel.trackAndAddShipment(waybill, carrierId) { res ->
                                    res.onSuccess { added ->
                                        val cName = KoreanCarrierCatalog.findById(added.carrierId)?.shortName ?: added.carrierId
                                        notify("$cName 배송을 추가했습니다 (${added.currentStatusLabel})")
                                        onOpenDetail(added.id)
                                    }.onFailure { err ->
                                        notify(err.message ?: "조회에 실패했습니다. 직접 등록해 보세요")
                                    }
                                }
                            },
                            onOpenAddWithPrefill = onOpenAdd,
                            onOpenShipmentDetail = onOpenDetail,
                            onRefreshShipment = { id ->
                                viewModel.refreshShipment(id) { _, msg -> notify(msg) }
                            },
                            onRefreshAllActive = {
                                viewModel.refreshAllActiveShipments { ok, total ->
                                    notify(if (total == 0) "진행중인 배송이 없습니다" else "${total}건 중 ${ok}건 새로고침 완료")
                                }
                            },
                            onLoadSampleShipments = {
                                viewModel.loadSampleShipments()
                                notify("샘플 배송 3건을 불러왔습니다")
                            },
                            onNavigateToSmartDetect = { selectedTab = 1 },
                            onNotify = notify,
                            isSelectionMode = isSelectionMode,
                            selectedIds = selectedIds,
                            onEnterSelectionMode = enterSelectionMode,
                            onToggleSelect = toggleSelection,
                            onSelectAll = { ids ->
                                isSelectionMode = ids.isNotEmpty()
                                selectedIds = ids.toSet()
                            },
                            onClearSelection = clearSelection,
                            onArchiveSelected = {
                                val count = selectedIds.size
                                viewModel.archiveShipments(selectedIds, archived = true)
                                clearSelection()
                                notify("${count}건을 보관함으로 이동했어요")
                            },
                            onRequestDeleteSelected = {
                                if (selectedIds.isNotEmpty()) showBatchDeleteConfirm = true
                            },
                            onOpenScanner = onOpenScanner,
                            onArchiveAllDelivered = {
                                val count = viewModel.archiveAllDelivered()
                                if (count > 0) notify("배송완료 ${count}건을 보관함으로 이동했어요")
                            },
                            onSwipeArchive = { id ->
                                viewModel.archiveShipments(setOf(id), archived = true)
                                notify("보관함으로 이동했어요")
                            },
                            onRequestDeleteSingle = { record ->
                                pendingSingleDelete = record
                            }
                        )

                        1 -> SmartDetectScreen(
                            isProbing = isProbing,
                            scrollBehavior = tabScrollBehaviors[1],
                            topInset = resolvedTopBarInset,
                            bottomBarExtraPadding = bottomBarExtraPadding,
                            onProbeAndAdd = { waybill, carrierId, itemHint, senderHint ->
                                viewModel.trackAndAddShipment(
                                    rawWaybill = waybill,
                                    preferredCarrierId = carrierId,
                                    itemNameHint = itemHint,
                                    senderNameHint = senderHint
                                ) { res ->
                                    res.onSuccess { added ->
                                        val cName = KoreanCarrierCatalog.findById(added.carrierId)?.shortName ?: added.carrierId
                                        notify("$cName 송장 확인 완료 · 등록됨")
                                        onOpenDetail(added.id)
                                    }.onFailure { err ->
                                        notify(err.message ?: "해당 택배사에서 아직 조회되지 않습니다")
                                    }
                                }
                            },
                            onNotify = notify
                        )

                        2 -> CarrierDirectoryScreen(
                            scrollBehavior = tabScrollBehaviors[2],
                            topInset = resolvedTopBarInset,
                            bottomBarExtraPadding = bottomBarExtraPadding,
                            onAddWithCarrier = { carrierId -> onOpenAdd(null, carrierId) }
                        )

                        else -> ArchiveStatsScreen(
                            state = state,
                            refreshingIds = refreshingIds,
                            scrollBehavior = tabScrollBehaviors[3],
                            topInset = resolvedTopBarInset,
                            bottomBarExtraPadding = bottomBarExtraPadding,
                            onOpenShipmentDetail = onOpenDetail,
                            onRefreshShipment = { id ->
                                viewModel.refreshShipment(id) { _, msg -> notify(msg) }
                            },
                            isSelectionMode = isSelectionMode,
                            selectedIds = selectedIds,
                            onEnterSelectionMode = enterSelectionMode,
                            onToggleSelect = toggleSelection,
                            onSelectAll = { ids ->
                                isSelectionMode = ids.isNotEmpty()
                                selectedIds = ids.toSet()
                            },
                            onClearSelection = clearSelection,
                            onRestoreSelected = {
                                val count = selectedIds.size
                                viewModel.archiveShipments(selectedIds, archived = false)
                                clearSelection()
                                notify("${count}건을 배송 목록으로 복원했어요")
                            },
                            onRequestDeleteSelected = {
                                if (selectedIds.isNotEmpty()) showBatchDeleteConfirm = true
                            }
                        )
                    }
                }
            }

            // ② Floating Island Top Bar
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged {
                        if (it.height > 0) {
                            val measured = with(density) { it.height.toDp() }
                            if (measured > 0.dp) measuredTopBarHeightDp = measured
                        }
                    }
            ) {
                MiuixFloatingScrollTopBar(
                    title = tabTitles[selectedTab],
                    scrollBehavior = activeScrollBehavior,
                    backdrop = barBackdrop,
                    maxContentWidth = if (state.settings.wideDualLayoutEnabled) Dp.Unspecified else 620.dp,
                    actions = {
                        IconButton(
                            onClick = {
                                showQuickMenu = false
                                onOpenSettings()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Settings,
                                contentDescription = "설정 및 백업",
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }

            // ③ Quick-add dim scrim below nav bar
            if (isQuickMenuMounted) {
                val scrimAlpha = 0.44f * quickMenuMorphProgress.value.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = scrimAlpha))
                        .pointerInput(showQuickMenu) {
                            if (showQuickMenu) {
                                detectTapGestures { showQuickMenu = false }
                            }
                        }
                )
            }

            // ④ Bottom edge blur + Nav Bar (outside barBackdrop!)
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                MiuixBottomEdgeBlur(
                    backdrop = barBackdrop,
                    extraHeight = 68.dp,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                if (isLiquidGlassNav) {
                    FloatingLiquidGlassNavBar(
                        backdrop = barBackdrop,
                        selectedTab = selectedTab,
                        isCenterAddSelected = showQuickMenu,
                        activeDeliveryCount = activeCount,
                        outForDeliveryCount = outForDeliveryCount,
                        onSelectTab = { tab ->
                            showQuickMenu = false
                            if (selectedTab != tab) clearSelection()
                            selectedTab = tab
                        },
                        onCenterAddClick = { showQuickMenu = !showQuickMenu },
                        useLensTint = state.settings.resolvedColorStyle() != ThemeColorStyle.DEFAULT,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .widthIn(max = 384.dp)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MiuixTheme.colorScheme.surface)
                    ) {
                        HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                        NavigationBar(
                            showDivider = false,
                            defaultWindowInsetsPadding = true,
                            mode = NavigationBarDisplayMode.IconAndText,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .widthIn(max = 560.dp)
                                .fillMaxWidth()
                        ) {
                            NavigationBarItem(
                                selected = !showQuickMenu && selectedTab == 0,
                                onClick = {
                                    showQuickMenu = false
                                    if (selectedTab != 0) clearSelection()
                                    selectedTab = 0
                                },
                                icon = MiuixIcons.Regular.Home,
                                label = "배송",
                                badge = if (activeCount > 0) {
                                    {
                                        if (outForDeliveryCount > 0) {
                                            Box(modifier = Modifier.offset(x = 6.dp, y = (-3).dp)) {
                                                Badge { Text(outForDeliveryCount.toString()) }
                                            }
                                        } else {
                                            Box(modifier = Modifier.offset(x = 6.dp, y = (-4).dp)) {
                                                Badge(modifier = Modifier.size(7.dp))
                                            }
                                        }
                                    }
                                } else null
                            )
                            NavigationBarItem(
                                selected = !showQuickMenu && selectedTab == 1,
                                onClick = {
                                    showQuickMenu = false
                                    if (selectedTab != 1) clearSelection()
                                    selectedTab = 1
                                },
                                icon = MiuixIcons.Scan,
                                label = "추측"
                            )

                            val addInteraction = remember { MutableInteractionSource() }
                            val addPressed by addInteraction.collectIsPressedAsState()
                            val addRotation by animateFloatAsState(
                                targetValue = if (showQuickMenu) 45f else 0f,
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 210f),
                                label = "miuixCenterAddRot"
                            )
                            val addContentColor = MiuixTheme.colorScheme.onSurface.copy(
                                alpha = when {
                                    showQuickMenu && addPressed -> NavigationBarDefaults.SelectedPressedAlpha
                                    showQuickMenu -> 1f
                                    addPressed -> NavigationBarDefaults.UnselectedPressedAlpha
                                    else -> NavigationBarDefaults.UnselectedAlpha
                                }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(NavigationBarDefaults.ItemHeight)
                                    .clickable(
                                        interactionSource = addInteraction,
                                        indication = null,
                                        role = Role.Tab,
                                        onClick = { showQuickMenu = !showQuickMenu }
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Add,
                                    contentDescription = "송장 등록 메뉴",
                                    tint = addContentColor,
                                    modifier = Modifier
                                        .padding(top = NavigationBarDefaults.IconTopPadding)
                                        .size(NavigationBarDefaults.IconSize)
                                        .graphicsLayer { rotationZ = addRotation }
                                )
                                Text(
                                    text = "등록",
                                    color = addContentColor,
                                    fontSize = NavigationBarDefaults.LabelFontSize,
                                    fontWeight = if (showQuickMenu) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(bottom = NavigationBarDefaults.BottomPadding)
                                )
                            }

                            NavigationBarItem(
                                selected = !showQuickMenu && selectedTab == 2,
                                onClick = {
                                    showQuickMenu = false
                                    if (selectedTab != 2) clearSelection()
                                    selectedTab = 2
                                },
                                icon = MiuixIcons.ListView,
                                label = "택배사"
                            )
                            NavigationBarItem(
                                selected = !showQuickMenu && selectedTab == 3,
                                onClick = {
                                    showQuickMenu = false
                                    if (selectedTab != 3) clearSelection()
                                    selectedTab = 3
                                },
                                icon = MiuixIcons.Folder,
                                label = "보관함"
                            )
                        }
                    }
                }
            }

            // ⑤ Morphing Quick-Add menu
            if (isQuickMenuMounted) {
                ExpandableQuickAddMenuOverlay(
                    morphProgress = quickMenuMorphProgress.value,
                    interactive = showQuickMenu,
                    isLiquidGlass = isLiquidGlassNav,
                    onClipboardTrack = {
                        showQuickMenu = false
                        val clip = clipboardManager.getText()?.text.orEmpty()
                        val parsed = WaybillDetector.parseNotificationText(clip)
                        if (parsed.waybillNumber.length < 6) {
                            notify("클립보드에서 유효한 송장번호를 찾지 못했습니다")
                        } else {
                            viewModel.trackAndAddShipment(
                                rawWaybill = parsed.waybillNumber,
                                preferredCarrierId = parsed.carrierHintId,
                                itemNameHint = parsed.itemNameHint,
                                senderNameHint = parsed.senderNameHint
                            ) { res ->
                                res.onSuccess { added ->
                                    notify("클립보드 송장을 조회·등록했습니다")
                                    onOpenDetail(added.id)
                                }.onFailure {
                                    onOpenAdd(parsed.waybillNumber, parsed.carrierHintId)
                                }
                            }
                        }
                    },
                    onManualAdd = {
                        showQuickMenu = false
                        onOpenAdd(null, null)
                    },
                    onBarcodeScan = {
                        showQuickMenu = false
                        onOpenScanner()
                    },
                    onRefreshAll = {
                        showQuickMenu = false
                        viewModel.refreshAllActiveShipments { ok, total ->
                            notify(if (total == 0) "새로고침할 배송이 없습니다" else "${total}건 중 ${ok}건 업데이트 완료")
                        }
                    }
                )
            }

            // ⑥ Cascading StackedToastHost above quick-add & nav bar
            val toastBottomPadding by animateDpAsState(
                targetValue = if (isLiquidGlassNav) 104.dp else 94.dp,
                animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMediumLow),
                label = "toastBottomOffset"
            )
            StackedToastHost(
                state = toastState,
                backdrop = barBackdrop,
                isLiquidGlass = isLiquidGlassNav,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = toastBottomPadding)
            )

            AppUpdateDialog(
                uiState = updateUiState,
                onDismiss = viewModel::dismissUpdateDialog,
                onStartDownload = viewModel::startUpdateDownloadAndInstall,
                onRetryInstall = viewModel::retryApkInstall,
                onOpenPermissionSettings = viewModel::openInstallUnknownSourcesSettings
            )

            ConfirmActionDialog(
                show = showBatchDeleteConfirm,
                title = "선택한 송장 삭제",
                summary = "선택한 ${selectedIds.size}건의 배송 기록과 추적 이력을 완전히 삭제할까요? 삭제 후에는 되돌릴 수 없습니다.",
                confirmText = "${selectedIds.size}건 삭제",
                isDestructive = true,
                onDismiss = { showBatchDeleteConfirm = false },
                onConfirm = {
                    val targets = selectedIds
                    val count = targets.size
                    showBatchDeleteConfirm = false
                    clearSelection()
                    viewModel.deleteShipments(targets)
                    notify("${count}건의 배송 기록을 삭제했어요")
                }
            )

            ConfirmActionDialog(
                show = pendingSingleDelete != null,
                title = "송장 삭제",
                summary = pendingSingleDelete?.let { record ->
                    val shortName = com.x2tora.torabox.domain.KoreanCarrierCatalog.findById(record.carrierId)?.shortName
                        ?: record.carrierId
                    "'${record.displayTitle(shortName)}'(${record.waybillNumber}) 배송 기록을 삭제할까요?"
                }.orEmpty(),
                confirmText = "삭제",
                isDestructive = true,
                onDismiss = { pendingSingleDelete = null },
                onConfirm = {
                    val target = pendingSingleDelete ?: return@ConfirmActionDialog
                    pendingSingleDelete = null
                    viewModel.deleteShipment(target.id)
                    notify("송장을 삭제했어요")
                }
            )
        }
    }
}

@Composable
private fun SettingsScreenHost(
    viewModel: ToraBoxViewModel,
    toastState: StackedToastState,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val updateUiState by viewModel.updateUiState.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior(state = rememberTopAppBarState())
    val settingsBackdrop = rememberLayerBackdrop()
    val surfaceBg = MiuixTheme.colorScheme.surface
    val notify: (String) -> Unit = { toastState.show(it) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            MiuixFloatingScrollTopBar(
                title = "설정 · 백업",
                scrollBehavior = scrollBehavior,
                backdrop = settingsBackdrop,
                maxContentWidth = 620.dp,
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(settingsBackdrop)
                    .background(surfaceBg)
            ) {
                MoreSettingsScreen(
                    state = state,
                    scrollBehavior = scrollBehavior,
                    topInset = paddingValues.calculateTopPadding(),
                    updateUiState = updateUiState,
                    onUpdateTheme = viewModel::updateTheme,
                    onUpdateThemeColorStyle = viewModel::updateThemeColorStyle,
                    onUpdateCustomKeyColor = viewModel::updateCustomKeyColor,
                    onUpdateNavBarStyle = viewModel::updateNavBarStyle,
                    onUpdateWideDualLayout = viewModel::updateWideDualLayout,
                    onUpdateAutoRefreshOnLaunch = viewModel::updateAutoRefreshOnLaunch,
                    onUpdateAutoDetectClipboard = viewModel::updateAutoDetectClipboard,
                    onUpdateAutoArchiveDays = viewModel::updateAutoArchiveDeliveredDays,
                    onUpdateBackgroundSync = viewModel::updateBackgroundSync,
                    onUpdateSweetTrackerApiKey = viewModel::updateSweetTrackerApiKey,
                    onUpdateCustomTrackerEndpoint = viewModel::updateCustomTrackerEndpoint,
                    onLoadSampleShipments = viewModel::loadSampleShipments,
                    onExportJson = viewModel::exportBackupJson,
                    onPreviewBackupJson = viewModel::previewBackupJson,
                    onImportJson = viewModel::importBackupJson,
                    onResetAllData = viewModel::resetAllData,
                    onCheckForUpdate = { viewModel.checkForUpdate(manual = true, onFeedback = notify) },
                    onNotify = notify
                )
            }

            MiuixBottomEdgeBlur(
                backdrop = settingsBackdrop,
                extraHeight = 36.dp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            StackedToastHost(
                state = toastState,
                backdrop = settingsBackdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            )

            AppUpdateDialog(
                uiState = updateUiState,
                onDismiss = viewModel::dismissUpdateDialog,
                onStartDownload = viewModel::startUpdateDownloadAndInstall,
                onRetryInstall = viewModel::retryApkInstall,
                onOpenPermissionSettings = viewModel::openInstallUnknownSourcesSettings
            )
        }
    }
}

@Composable
private fun ExpandableQuickAddMenuOverlay(
    morphProgress: Float,
    interactive: Boolean,
    isLiquidGlass: Boolean,
    onClipboardTrack: () -> Unit,
    onManualAdd: () -> Unit,
    onBarcodeScan: () -> Unit,
    onRefreshAll: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val density = LocalDensity.current
    val raw = morphProgress.coerceAtLeast(0f)
    val clamped = raw.coerceIn(0f, 1f)
    val liftProgress = smoothStep(0f, 0.72f, clamped) + (raw - clamped) * 0.35f
    val sizeProgress = smoothStep(0.02f, 0.85f, clamped) + (raw - clamped) * 0.18f
    val colorBlend = smoothStep(0.06f, 0.58f, clamped)
    val contentFade = smoothStep(0.32f, 0.84f, clamped)

    val surfaceCardColor = if (isDark) MiuixTheme.colorScheme.surfaceContainerHigh else Color.White
    val cardColor = if (isLiquidGlass) {
        androidx.compose.ui.graphics.lerp(MiuixTheme.colorScheme.primary, surfaceCardColor, colorBlend)
    } else {
        surfaceCardColor.copy(alpha = smoothStep(0f, 0.30f, clamped))
    }
    val borderColor = MiuixTheme.colorScheme.onSurface.copy(
        alpha = lerpFloat(0f, if (isDark) 0.12f else 0.08f, clamped)
    )
    val bottomOffset = androidx.compose.ui.unit.lerp(
        if (isLiquidGlass) 22.dp else 36.dp,
        if (isLiquidGlass) 88.dp else 80.dp,
        liftProgress.coerceAtLeast(0f)
    )
    val cornerRadius = androidx.compose.ui.unit.lerp(
        if (isLiquidGlass) 22.dp else 18.dp,
        26.dp,
        clamped
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val targetWidthDp = (maxWidth - 40.dp).coerceIn(264.dp, 340.dp)
        val targetWidthPx = with(density) { targetWidthDp.roundToPx() }
        val minWidthPx = with(density) { (if (isLiquidGlass) 50.dp else 84.dp).roundToPx() }
        val minHeightPx = with(density) { (if (isLiquidGlass) 44.dp else 36.dp).roundToPx() }
        val cardShape = RoundedCornerShape(cornerRadius)

        Box(
            modifier = Modifier
                .padding(bottom = bottomOffset)
                .shadow(
                    elevation = lerpFloat(2f, 22f, clamped).dp,
                    shape = cardShape,
                    ambientColor = Color.Black.copy(alpha = lerpFloat(0.08f, 0.28f, clamped)),
                    spotColor = Color.Black.copy(alpha = lerpFloat(0.10f, 0.32f, clamped))
                )
                .clip(cardShape)
                .background(cardColor)
                .squircleBorder(1.dp, borderColor, cornerRadius)
                .pointerInput(interactive) {
                    if (interactive) detectTapGestures { }
                }
                .layout { measurable, constraints ->
                    val childConstraints = constraints.copy(
                        minWidth = targetWidthPx,
                        maxWidth = targetWidthPx,
                        minHeight = 0
                    )
                    val placeable = measurable.measure(childConstraints)
                    val curW = lerpFloat(minWidthPx.toFloat(), targetWidthPx.toFloat(), sizeProgress)
                        .roundToInt()
                        .coerceAtLeast(1)
                    val curH = lerpFloat(minHeightPx.toFloat(), placeable.height.toFloat(), sizeProgress)
                        .roundToInt()
                        .coerceAtLeast(1)
                    layout(curW, curH) {
                        placeable.placeRelative(
                            x = (curW - placeable.width) / 2,
                            y = (curH - placeable.height) / 2
                        )
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .width(targetWidthDp)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .graphicsLayer { alpha = contentFade }
            ) {
                Text(
                    text = "빠른 등록 · 조회",
                    style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.SemiBold),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 6.dp)
                )
                QuickAddMorphActionRow(
                    icon = MiuixIcons.Paste,
                    iconTint = MiuixTheme.colorScheme.primary,
                    title = "클립보드 송장 즉시 조회",
                    subtitle = "복사한 번호·문자에서 택배사 자동 탐지",
                    revealProgress = smoothStep(0.28f, 0.78f, clamped),
                    enabled = interactive,
                    onClick = onClipboardTrack
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                QuickAddMorphActionRow(
                    icon = MiuixIcons.Scan,
                    iconTint = Color(0xFF10B981),
                    title = "운송장 직접 등록",
                    subtitle = "택배사 선택 · 물품명 조회 및 저장",
                    revealProgress = smoothStep(0.36f, 0.86f, clamped),
                    enabled = interactive,
                    onClick = onManualAdd
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                QuickAddMorphActionRow(
                    icon = MiuixIcons.Scan,
                    iconTint = Color(0xFF8B5CF6),
                    title = "바코드 · QR 스캔",
                    subtitle = "택배 상자·영수증 바코드로 즉시 입력",
                    revealProgress = smoothStep(0.40f, 0.90f, clamped),
                    enabled = interactive,
                    onClick = onBarcodeScan
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                QuickAddMorphActionRow(
                    icon = MiuixIcons.Refresh,
                    iconTint = Color(0xFFF97316),
                    title = "진행중인 배송 전체 새로고침",
                    subtitle = "모든 택배 최신 단계 일괄 동기화",
                    revealProgress = smoothStep(0.44f, 0.94f, clamped),
                    enabled = interactive,
                    onClick = onRefreshAll
                )
            }
        }
    }
}

@Composable
private fun QuickAddMorphActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    revealProgress: Float,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer {
                alpha = revealProgress
                translationY = (1f - revealProgress) * with(density) { 12.dp.toPx() }
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.headline2.copy(fontWeight = FontWeight.SemiBold),
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = MiuixIcons.ChevronForward,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun lerpFloat(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction

private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
    if (edge1 <= edge0) return if (x >= edge1) 1f else 0f
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
