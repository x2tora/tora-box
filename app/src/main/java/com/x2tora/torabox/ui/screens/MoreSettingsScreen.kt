package com.x2tora.torabox.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x2tora.torabox.BuildConfig
import com.x2tora.torabox.model.NavBarStyle
import com.x2tora.torabox.model.ThemeColorStyle
import com.x2tora.torabox.model.ThemeOption
import com.x2tora.torabox.model.ToraBoxDataState
import com.x2tora.torabox.ui.components.AppUpdateUiState
import com.x2tora.torabox.ui.components.ConfirmActionDialog
import com.x2tora.torabox.ui.components.MiuixColorPickerDialog
import com.x2tora.torabox.ui.components.MiuixGlassIcon
import com.x2tora.torabox.ui.components.formatRgbHex
import com.x2tora.torabox.ui.theme.ToraBoxMotion
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Import
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Paste
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.ScreenMirroring
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreSettingsScreen(
    state: ToraBoxDataState,
    scrollBehavior: ScrollBehavior,
    updateUiState: AppUpdateUiState,
    onUpdateTheme: (ThemeOption) -> Unit,
    onUpdateThemeColorStyle: (ThemeColorStyle) -> Unit,
    onUpdateCustomKeyColor: (Long?) -> Unit,
    onUpdateNavBarStyle: (NavBarStyle) -> Unit,
    onUpdateWideDualLayout: (Boolean) -> Unit,
    onUpdateAutoRefreshOnLaunch: (Boolean) -> Unit,
    onUpdateAutoDetectClipboard: (Boolean) -> Unit,
    onUpdateSweetTrackerApiKey: (String) -> Unit,
    onUpdateCustomTrackerEndpoint: (String) -> Unit,
    onLoadSampleShipments: () -> Unit,
    onExportJson: () -> String,
    onPreviewBackupJson: (String) -> Result<ToraBoxDataState>,
    onImportJson: (String) -> Result<Unit>,
    onResetAllData: () -> Unit,
    onCheckForUpdate: () -> Unit,
    onNotify: (String) -> Unit,
    topInset: Dp = 0.dp
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    var showAppColorDialog by remember { mutableStateOf(false) }
    var showSweetTrackerDialog by remember { mutableStateOf(false) }
    var showEndpointDialog by remember { mutableStateOf(false) }
    var showPasteRestoreDialog by remember { mutableStateOf(false) }
    var pendingImportBackup by remember { mutableStateOf<Pair<String, ToraBoxDataState>?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(onExportJson().toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                onNotify("JSON 백업 파일을 저장했습니다")
            }.onFailure {
                onNotify("백업 파일 저장에 실패했습니다")
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: error("파일을 읽을 수 없습니다")
            }.fold(
                onSuccess = { jsonText ->
                    onPreviewBackupJson(jsonText).onSuccess { parsed ->
                        pendingImportBackup = jsonText to parsed
                    }.onFailure {
                        onNotify("올바른 ToraBox 백업 파일이 아닙니다")
                    }
                },
                onFailure = { onNotify("백업 파일 읽기에 실패했습니다") }
            )
        }
    }

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
                top = topInset + 8.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SmallTitle(text = "화면 · 테마 설정")
                Card(modifier = Modifier.fillMaxWidth()) {
                    val options = ThemeOption.entries
                    OverlayDropdownPreference(
                        title = "테마 모드",
                        summary = state.settings.themeOption.label,
                        items = options.map { it.label },
                        selectedIndex = options.indexOf(state.settings.themeOption),
                        onSelectedIndexChange = { onUpdateTheme(options[it]) },
                        startAction = { PreferenceStartIcon(MiuixIcons.Theme, MiuixTheme.colorScheme.primary) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    val colorStyles = ThemeColorStyle.entries
                    val colorStyle = state.settings.resolvedColorStyle()
                    OverlayDropdownPreference(
                        title = "테마 컬러 스타일",
                        summary = colorStyle.label,
                        items = colorStyles.map { it.label },
                        selectedIndex = colorStyles.indexOf(colorStyle),
                        onSelectedIndexChange = { index ->
                            val chosen = colorStyles[index]
                            onUpdateThemeColorStyle(chosen)
                            onNotify("테마 컬러가 '${chosen.label}'으로 적용되었습니다")
                        },
                        startAction = { PreferenceStartIcon(MiuixIcons.Favorites, Color(0xFF7C5CFF)) }
                    )
                    AnimatedVisibility(
                        visible = colorStyle == ThemeColorStyle.CUSTOM,
                        enter = ToraBoxMotion.expandEnter(),
                        exit = ToraBoxMotion.shrinkExit()
                    ) {
                        val previewHex = state.settings.customKeyColorHex ?: 0xFF3482FF
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ArrowPreference(
                                title = "커스텀 포인트 색상 지정",
                                summary = "현재 ${formatRgbHex(previewHex)} 적용 중",
                                onClick = { showAppColorDialog = true },
                                startAction = {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(previewHex))
                                            .border(1.dp, MiuixTheme.colorScheme.onSurface.copy(alpha = 0.16f), CircleShape)
                                    )
                                }
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    val navStyles = NavBarStyle.entries
                    OverlayDropdownPreference(
                        title = "인터페이스 스타일",
                        summary = state.settings.navBarStyle.label,
                        items = navStyles.map { it.label },
                        selectedIndex = navStyles.indexOf(state.settings.navBarStyle),
                        onSelectedIndexChange = { index ->
                            val chosen = navStyles[index]
                            onUpdateNavBarStyle(chosen)
                            onNotify("인터페이스 스타일이 '${chosen.label}'으로 적용되었습니다")
                        },
                        startAction = { PreferenceStartIcon(MiuixIcons.Tune, Color(0xFF14B8A6)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchPreference(
                        title = "대화면 듀얼 칼럼 보기",
                        summary = "태블릿·폴더블에서 좌우 2단 분할 표시",
                        checked = state.settings.wideDualLayoutEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateWideDualLayout(enabled)
                            onNotify(
                                if (enabled) "넓은 화면에서 듀얼 칼럼 레이아웃이 활성화되었습니다"
                                else "대화면에서도 중앙 단일 보기로 표시됩니다"
                            )
                        },
                        startAction = { PreferenceStartIcon(MiuixIcons.ScreenMirroring, Color(0xFF3B82F6)) }
                    )
                }
            }

            item {
                SmallTitle(text = "배송 조회 · 확장 API")
                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "앱 실행 시 진행중 배송 자동 새로고침",
                        checked = state.settings.autoRefreshOnLaunch,
                        onCheckedChange = onUpdateAutoRefreshOnLaunch,
                        startAction = { PreferenceStartIcon(MiuixIcons.Refresh, Color(0xFF10B981)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchPreference(
                        title = "클립보드 송장번호 자동 감지",
                        checked = state.settings.autoDetectClipboardWaybill,
                        onCheckedChange = onUpdateAutoDetectClipboard,
                        startAction = { PreferenceStartIcon(MiuixIcons.Paste, Color(0xFF0EA5E9)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ArrowPreference(
                        title = "스마트택배 API 키 (선택)",
                        summary = if (state.settings.sweetTrackerApiKey.isBlank()) {
                            "미설정 · 주요 택배사는 키 없이도 직접 조회 가능"
                        } else "설정됨 · 73개 국내 택배사 확장 조회 사용",
                        onClick = { showSweetTrackerDialog = true },
                        startAction = { PreferenceStartIcon(MiuixIcons.Lock, Color(0xFFF59E0B)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ArrowPreference(
                        title = "Delivery Tracker 통합 엔드포인트",
                        summary = state.settings.customTrackerEndpoint,
                        onClick = { showEndpointDialog = true },
                        startAction = { PreferenceStartIcon(MiuixIcons.Link, Color(0xFF6366F1)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ArrowPreference(
                        title = "샘플 배송 데이터 추가",
                        summary = "CJ대한통운 · 우체국 · GS반값택배 예시 확인",
                        onClick = {
                            onLoadSampleShipments()
                            onNotify("샘플 배송 3건을 추가했습니다")
                        },
                        startAction = { PreferenceStartIcon(MiuixIcons.Layers, Color(0xFF8B5CF6)) }
                    )
                }
            }

            item {
                SmallTitle(text = "백업 및 데이터")
                Card(modifier = Modifier.fillMaxWidth()) {
                    ArrowPreference(
                        title = "JSON 백업 내보내기",
                        onClick = {
                            val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                            exportFileLauncher.launch("torabox-backup-$today.json")
                        },
                        startAction = { PreferenceStartIcon(MiuixIcons.Backup, Color(0xFF3482FF)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ArrowPreference(
                        title = "JSON 백업 가져오기",
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        startAction = { PreferenceStartIcon(MiuixIcons.Import, Color(0xFF10B981)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ArrowPreference(
                        title = "클립보드 백업 · 복원",
                        onClick = { showPasteRestoreDialog = true },
                        startAction = { PreferenceStartIcon(MiuixIcons.Copy, Color(0xFF06B6D4)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ArrowPreference(
                        title = "모든 배송 데이터 초기화",
                        summary = "테마·기본 설정은 유지됩니다",
                        onClick = { showResetConfirm = true },
                        startAction = { PreferenceStartIcon(MiuixIcons.Reset, MiuixTheme.colorScheme.error) }
                    )
                }
            }

            item {
                SmallTitle(text = "앱 정보 및 업데이트")
                Card(modifier = Modifier.fillMaxWidth()) {
                    ArrowPreference(
                        title = "앱 업데이트 확인",
                        summary = when {
                            updateUiState.isChecking -> "확인 중"
                            !updateUiState.lastStatusNote.isNullOrBlank() -> "v${BuildConfig.VERSION_NAME} · ${updateUiState.lastStatusNote}"
                            else -> "v${BuildConfig.VERSION_NAME}"
                        },
                        onClick = onCheckForUpdate,
                        startAction = { PreferenceStartIcon(MiuixIcons.Store, Color(0xFF3B82F6)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    BasicComponent(
                        title = "ToraBox (x2tora/tora-box)",
                        summary = "v${BuildConfig.VERSION_NAME}",
                        startAction = { PreferenceStartIcon(MiuixIcons.Info, Color(0xFF64748B)) }
                    )
                }
            }
        }
    }

    MiuixColorPickerDialog(
        show = showAppColorDialog,
        title = "커스텀 테마 포인트 색상 선택",
        initialColorHex = state.settings.customKeyColorHex ?: 0xFF3482FF,
        onDismiss = { showAppColorDialog = false },
        onConfirm = { chosenHex ->
            onUpdateCustomKeyColor(chosenHex)
            onNotify("커스텀 포인트 색상이 ${formatRgbHex(chosenHex)}로 적용되었습니다")
        }
    )

    if (showSweetTrackerDialog) {
        var keyText by remember(state.settings.sweetTrackerApiKey) { mutableStateOf(state.settings.sweetTrackerApiKey) }
        OverlayDialog(
            show = true,
            title = "스마트택배 API 키 설정",
            summary = "주요 택배사는 키 없이도 직접 조회되며, 중소 화물·B2B 택배사까지 모두 API로 조회하려면 키를 입력해 주세요.",
            onDismissRequest = { showSweetTrackerDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = "t_key 입력 (비워두면 직접 조회만 사용)",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(text = "취소", onClick = { showSweetTrackerDialog = false }, modifier = Modifier.weight(1f))
                    TextButton(
                        text = "저장",
                        onClick = {
                            onUpdateSweetTrackerApiKey(keyText)
                            showSweetTrackerDialog = false
                            onNotify(if (keyText.isBlank()) "스마트택배 API 키를 비웠습니다" else "스마트택배 API 키를 저장했습니다")
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showEndpointDialog) {
        var epText by remember(state.settings.customTrackerEndpoint) { mutableStateOf(state.settings.customTrackerEndpoint) }
        OverlayDialog(
            show = true,
            title = "Delivery Tracker 엔드포인트",
            summary = "직접 조회가 실패할 때 사용할 보조 REST API 주소를 설정합니다.",
            onDismissRequest = { showEndpointDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = epText,
                    onValueChange = { epText = it },
                    label = "HTTPS 기본 URL",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(text = "기본값", onClick = { epText = "https://apis.tracker.delivery" }, modifier = Modifier.weight(0.9f))
                    TextButton(
                        text = "저장",
                        onClick = {
                            onUpdateCustomTrackerEndpoint(epText)
                            showEndpointDialog = false
                            onNotify("보조 조회 엔드포인트를 저장했습니다")
                        },
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }
        }
    }

    var pasteJson by remember(showPasteRestoreDialog) { mutableStateOf("") }
    OverlayDialog(
        show = showPasteRestoreDialog,
        title = "클립보드 JSON 백업 / 복원",
        onDismissRequest = { showPasteRestoreDialog = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(
                text = "현재 데이터 JSON 클립보드로 복사",
                onClick = {
                    clipboardManager.setText(AnnotatedString(onExportJson()))
                    onNotify("백업 JSON을 클립보드에 복사했습니다")
                },
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider()
            TextField(
                value = pasteJson,
                onValueChange = { pasteJson = it },
                label = "복원할 JSON 텍스트 붙여넣기",
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(text = "취소", onClick = { showPasteRestoreDialog = false }, modifier = Modifier.weight(1f))
                TextButton(
                    text = "복원 실행",
                    onClick = {
                        onPreviewBackupJson(pasteJson).onSuccess { parsed ->
                            showPasteRestoreDialog = false
                            pendingImportBackup = pasteJson to parsed
                        }.onFailure {
                            onNotify("유효한 JSON 형식이 아닙니다")
                        }
                    },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    val importPair = pendingImportBackup
    if (importPair != null) {
        val (rawJsonToRestore, preview) = importPair
        ConfirmActionDialog(
            show = true,
            title = "백업 데이터 복원 확인",
            summary = "현재 저장된 데이터가 아래 백업 내용으로 덮어씌워집니다.\n\n" +
                "• 등록된 송장 ${preview.shipments.size}건\n\n" +
                "복원을 계속할까요?",
            confirmText = "덮어쓰고 복원",
            isDestructive = true,
            onDismiss = { pendingImportBackup = null },
            onConfirm = {
                pendingImportBackup = null
                onImportJson(rawJsonToRestore).onSuccess {
                    onNotify("백업 데이터를 복원했습니다")
                }.onFailure {
                    onNotify("백업 복원 중 오류가 발생했습니다")
                }
            }
        )
    }

    ConfirmActionDialog(
        show = showResetConfirm,
        title = "전체 배송 데이터 초기화",
        summary = "저장된 모든 송장 번호와 배송 이력이 삭제됩니다. 계속할까요?",
        confirmText = "초기화",
        isDestructive = true,
        onDismiss = { showResetConfirm = false },
        onConfirm = {
            onResetAllData()
            showResetConfirm = false
            onNotify("모든 배송 데이터가 초기화되었습니다")
        }
    )
}

@Composable
private fun PreferenceStartIcon(
    icon: ImageVector,
    accentColor: Color
) {
    MiuixGlassIcon(
        icon = icon,
        contentDescription = null,
        accentColor = accentColor,
        modifier = Modifier.padding(end = 6.dp),
        size = 38.dp,
        iconSize = 18.dp
    )
}
