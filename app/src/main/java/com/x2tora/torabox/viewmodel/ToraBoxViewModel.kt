package com.x2tora.torabox.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.x2tora.torabox.BuildConfig
import com.x2tora.torabox.data.ToraBoxRepository
import com.x2tora.torabox.domain.DeliveryIntelligence
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.domain.ParcelTrackingEngine
import com.x2tora.torabox.domain.ShipmentMaintenance
import com.x2tora.torabox.domain.WaybillDetector
import com.x2tora.torabox.model.AppSettings
import com.x2tora.torabox.model.HomeSortOption
import com.x2tora.torabox.model.NavBarStyle
import com.x2tora.torabox.model.PendingCaptureItem
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.model.SwipeActionOption
import com.x2tora.torabox.model.ThemeColorStyle
import com.x2tora.torabox.model.ThemeOption
import com.x2tora.torabox.model.ToraBoxDataState
import com.x2tora.torabox.model.TrackingResult
import com.x2tora.torabox.ui.components.AppUpdatePhase
import com.x2tora.torabox.ui.components.AppUpdateUiState
import com.x2tora.torabox.update.AppUpdater
import com.x2tora.torabox.update.InstallLaunchOutcome
import com.x2tora.torabox.update.UpdateCheckOutcome
import com.x2tora.torabox.widget.ToraBoxWidgetUpdater
import com.x2tora.torabox.worker.ShipmentSyncScheduler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ToraBoxViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ToraBoxRepository(application)

    private val _state = MutableStateFlow(repo.load())
    val state: StateFlow<ToraBoxDataState> = _state.asStateFlow()

    private val _refreshingIds = MutableStateFlow<Set<String>>(emptySet())
    val refreshingIds: StateFlow<Set<String>> = _refreshingIds.asStateFlow()

    private val _isProbingWaybill = MutableStateFlow(false)
    val isProbingWaybill: StateFlow<Boolean> = _isProbingWaybill.asStateFlow()

    private val _updateUiState = MutableStateFlow(
        AppUpdateUiState(currentVersionName = BuildConfig.VERSION_NAME)
    )
    val updateUiState: StateFlow<AppUpdateUiState> = _updateUiState.asStateFlow()

    private var downloadedApkFile: File? = null

    init {
        applyAutoArchiveIfNeeded()
        val settings = _state.value.settings
        ShipmentSyncScheduler.syncSchedule(
            context = application,
            enabled = settings.backgroundSyncEnabled,
            intervalHours = settings.backgroundSyncIntervalHours
        )
        if (settings.autoRefreshOnLaunch && _state.value.shipments.isNotEmpty()) {
            refreshAllActiveShipments(silent = true)
        }
    }

    private fun updateAndSave(transform: (ToraBoxDataState) -> ToraBoxDataState) {
        _state.update { prev ->
            val next = transform(prev)
            repo.save(next)
            next
        }
        ToraBoxWidgetUpdater.updateAllAsync(getApplication())
    }

    fun upsertShipment(record: ShipmentRecord) {
        val normalizedNumber = WaybillDetector.normalizeWaybill(record.waybillNumber)
        val cleanRecord = record.copy(waybillNumber = normalizedNumber)
        updateAndSave { prev ->
            val exists = prev.shipments.any { it.id == cleanRecord.id }
            val nextList = if (exists) {
                prev.shipments.map { if (it.id == cleanRecord.id) cleanRecord else it }
            } else {
                listOf(cleanRecord) + prev.shipments
            }
            val nextRecent = if (normalizedNumber.isNotBlank()) {
                (listOf(normalizedNumber) + prev.recentSearches).distinct().take(10)
            } else prev.recentSearches
            prev.copy(shipments = nextList, recentSearches = nextRecent)
        }
    }

    fun deleteShipment(id: String) {
        updateAndSave { prev ->
            prev.copy(shipments = prev.shipments.filterNot { it.id == id })
        }
    }

    fun deleteShipments(ids: Set<String>) {
        if (ids.isEmpty()) return
        updateAndSave { prev ->
            prev.copy(shipments = prev.shipments.filterNot { it.id in ids })
        }
    }

    fun archiveShipments(ids: Set<String>, archived: Boolean) {
        if (ids.isEmpty()) return
        updateAndSave { prev ->
            prev.copy(
                shipments = prev.shipments.map {
                    if (it.id in ids) it.copy(isArchived = archived) else it
                }
            )
        }
    }

    fun archiveAllDelivered(): Int {
        var count = 0
        updateAndSave { prev ->
            val nextList = prev.shipments.map {
                if (!it.isArchived && it.currentPhase == StandardDeliveryPhase.DELIVERED) {
                    count++
                    it.copy(isArchived = true)
                } else it
            }
            prev.copy(shipments = nextList)
        }
        return count
    }

    fun applyAutoArchiveIfNeeded(): Int {
        val days = _state.value.settings.autoArchiveDeliveredDays
        if (days <= 0) return 0
        var archivedCount = 0
        updateAndSave { prev ->
            val (updated, count) = ShipmentMaintenance.applyAutoArchive(prev.shipments, days)
            archivedCount = count
            if (count > 0) prev.copy(shipments = updated) else prev
        }
        return archivedCount
    }

    fun togglePin(id: String) {
        updateAndSave { prev ->
            prev.copy(
                shipments = prev.shipments.map {
                    if (it.id == id) it.copy(isPinned = !it.isPinned) else it
                }
            )
        }
    }

    fun toggleArchive(id: String) {
        updateAndSave { prev ->
            prev.copy(
                shipments = prev.shipments.map {
                    if (it.id == id) it.copy(isArchived = !it.isArchived) else it
                }
            )
        }
    }

    fun loadSampleShipments() {
        val samples = repo.createSampleShipments()
        updateAndSave { prev ->
            val existingKeys = prev.shipments.map { "${it.carrierId}:${it.waybillNumber}" }.toSet()
            val toAdd = samples.filter { "${it.carrierId}:${it.waybillNumber}" !in existingKeys }
            prev.copy(shipments = toAdd + prev.shipments)
        }
    }

    fun refreshShipment(
        shipmentId: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val shipment = _state.value.shipments.find { it.id == shipmentId } ?: return
        val carrier = KoreanCarrierCatalog.findById(shipment.carrierId) ?: return
        if (shipmentId in _refreshingIds.value) return

        viewModelScope.launch {
            _refreshingIds.update { it + shipmentId }
            try {
                val settings = _state.value.settings
                val res = ParcelTrackingEngine.track(
                    carrier = carrier,
                    rawWaybill = shipment.waybillNumber,
                    sweetTrackerApiKey = settings.sweetTrackerApiKey,
                    customTrackerEndpoint = settings.customTrackerEndpoint,
                    appContext = getApplication(),
                    personalCustomsCode = settings.personalCustomsCode
                )
                res.onSuccess { tr ->
                    val merged = applyTrackingResult(shipment, tr)
                    upsertShipment(merged)
                    val metaInfo = buildList {
                        if (merged.itemName.isNotBlank()) add("물품명·보낸이 확인")
                        else add(merged.currentStatusLabel)
                    }.joinToString()
                    onComplete(true, "${carrier.shortName} 조회 완료 ($metaInfo)")
                }.onFailure { err ->
                    onComplete(false, err.message ?: "배송 정보를 불러오지 못했습니다")
                }
            } finally {
                _refreshingIds.update { it - shipmentId }
            }
        }
    }

    fun refreshAllActiveShipments(
        silent: Boolean = false,
        onComplete: (Int, Int) -> Unit = { _, _ -> }
    ) {
        val active = _state.value.shipments.filter {
            !it.isArchived && it.currentPhase != StandardDeliveryPhase.DELIVERED
        }
        if (active.isEmpty()) {
            if (!silent) onComplete(0, 0)
            return
        }
        viewModelScope.launch {
            val settings = _state.value.settings
            val results = active.map { item ->
                async {
                    val carrier = KoreanCarrierCatalog.findById(item.carrierId) ?: return@async item.id to null
                    _refreshingIds.update { it + item.id }
                    try {
                        val tr = ParcelTrackingEngine.track(
                            carrier = carrier,
                            rawWaybill = item.waybillNumber,
                            sweetTrackerApiKey = settings.sweetTrackerApiKey,
                            customTrackerEndpoint = settings.customTrackerEndpoint,
                            appContext = getApplication(),
                            personalCustomsCode = settings.personalCustomsCode
                        ).getOrNull()
                        item.id to tr
                    } finally {
                        _refreshingIds.update { it - item.id }
                    }
                }
            }.awaitAll()

            var successCount = 0
            val mapById = results.toMap()
            updateAndSave { prev ->
                prev.copy(
                    shipments = prev.shipments.map { cur ->
                        val tr = mapById[cur.id]
                        if (tr != null) {
                            successCount++
                            applyTrackingResult(cur, tr)
                        } else cur
                    }
                )
            }
            applyAutoArchiveIfNeeded()
            onComplete(successCount, active.size)
        }
    }

    fun trackAndAddShipment(
        rawWaybill: String,
        preferredCarrierId: String? = null,
        customTitle: String = "",
        itemNameHint: String = "",
        senderNameHint: String = "",
        onResult: (Result<ShipmentRecord>) -> Unit
    ) {
        val waybill = WaybillDetector.normalizeWaybill(rawWaybill)
        if (waybill.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("송장번호를 입력해 주세요")))
            return
        }
        if (_isProbingWaybill.value) return

        viewModelScope.launch {
            _isProbingWaybill.value = true
            try {
                val settings = _state.value.settings
                val res = if (preferredCarrierId != null) {
                    val c = KoreanCarrierCatalog.findById(preferredCarrierId)
                    if (c != null) {
                        ParcelTrackingEngine.track(
                            carrier = c,
                            rawWaybill = waybill,
                            sweetTrackerApiKey = settings.sweetTrackerApiKey,
                            customTrackerEndpoint = settings.customTrackerEndpoint,
                            appContext = getApplication(),
                            personalCustomsCode = settings.personalCustomsCode
                        )
                    } else {
                        ParcelTrackingEngine.probeAndTrackWaybill(
                            rawWaybill = waybill,
                            carrierHintId = null,
                            sweetTrackerApiKey = settings.sweetTrackerApiKey,
                            customTrackerEndpoint = settings.customTrackerEndpoint,
                            appContext = getApplication(),
                            personalCustomsCode = settings.personalCustomsCode
                        )
                    }
                } else {
                    ParcelTrackingEngine.probeAndTrackWaybill(
                        rawWaybill = waybill,
                        carrierHintId = null,
                        sweetTrackerApiKey = settings.sweetTrackerApiKey,
                        customTrackerEndpoint = settings.customTrackerEndpoint,
                        appContext = getApplication(),
                        personalCustomsCode = settings.personalCustomsCode
                    )
                }

                res.onSuccess { tr ->
                    val carrier = KoreanCarrierCatalog.findById(tr.carrierId)
                    val existing = _state.value.shipments.find {
                        it.waybillNumber == waybill && it.carrierId == tr.carrierId
                    }
                    val base = existing ?: ShipmentRecord(
                        id = UUID.randomUUID().toString(),
                        waybillNumber = waybill,
                        carrierId = tr.carrierId,
                        customTitle = customTitle.trim(),
                        itemName = itemNameHint.trim(),
                        senderName = senderNameHint.trim(),
                        iconKey = carrier?.iconKey ?: "box",
                        colorHex = carrier?.accentColorHex ?: 0xFF3482FF,
                        createdAtEpochMs = System.currentTimeMillis()
                    )
                    val merged = applyTrackingResult(base, tr)
                    upsertShipment(merged)
                    onResult(Result.success(merged))
                }.onFailure { err ->
                    onResult(Result.failure(err))
                }
            } finally {
                _isProbingWaybill.value = false
            }
        }
    }

    fun probeMetadataPreview(
        rawWaybill: String,
        carrierId: String?,
        onResult: (Result<TrackingResult>) -> Unit
    ) {
        val waybill = WaybillDetector.normalizeWaybill(rawWaybill)
        if (waybill.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("송장번호를 먼저 입력해 주세요")))
            return
        }
        if (_isProbingWaybill.value) return

        viewModelScope.launch {
            _isProbingWaybill.value = true
            try {
                val settings = _state.value.settings
                val res = if (carrierId != null) {
                    val carrier = KoreanCarrierCatalog.findById(carrierId)
                    if (carrier != null) {
                        ParcelTrackingEngine.track(
                            carrier = carrier,
                            rawWaybill = waybill,
                            sweetTrackerApiKey = settings.sweetTrackerApiKey,
                            customTrackerEndpoint = settings.customTrackerEndpoint,
                            appContext = getApplication(),
                            personalCustomsCode = settings.personalCustomsCode
                        )
                    } else {
                        ParcelTrackingEngine.probeAndTrackWaybill(
                            rawWaybill = waybill,
                            carrierHintId = null,
                            sweetTrackerApiKey = settings.sweetTrackerApiKey,
                            customTrackerEndpoint = settings.customTrackerEndpoint,
                            appContext = getApplication(),
                            personalCustomsCode = settings.personalCustomsCode
                        )
                    }
                } else {
                    ParcelTrackingEngine.probeAndTrackWaybill(
                        rawWaybill = waybill,
                        carrierHintId = null,
                        sweetTrackerApiKey = settings.sweetTrackerApiKey,
                        customTrackerEndpoint = settings.customTrackerEndpoint,
                        appContext = getApplication(),
                        personalCustomsCode = settings.personalCustomsCode
                    )
                }
                onResult(res)
            } finally {
                _isProbingWaybill.value = false
            }
        }
    }

    private fun applyTrackingResult(base: ShipmentRecord, tr: TrackingResult): ShipmentRecord {
        val avgHours = DeliveryIntelligence.computeAverageDeliveryHours(_state.value.shipments, tr.carrierId)
        return ShipmentMaintenance.applyTrackingResult(
            base = base,
            tr = tr,
            historicalSameCarrierHours = avgHours
        )
    }

    fun reloadFromStorage() {
        val fresh = repo.load()
        _state.value = fresh
    }

    fun acceptPendingCapture(item: PendingCaptureItem, onDone: (String) -> Unit = {}) {
        updateAndSave { prev ->
            prev.copy(pendingCaptures = prev.pendingCaptures.filterNot { it.id == item.id })
        }
        trackAndAddShipment(
            rawWaybill = item.waybillNumber,
            preferredCarrierId = item.carrierHintId,
            itemNameHint = item.itemNameHint,
            senderNameHint = item.senderNameHint
        ) { res ->
            res.onSuccess { onDone("'${it.displayTitle(it.carrierId)}' 등록 완료") }
                .onFailure {
                    val carrier = item.carrierHintId?.let { KoreanCarrierCatalog.findById(it) }
                    upsertShipment(
                        ShipmentRecord(
                            id = UUID.randomUUID().toString(),
                            waybillNumber = item.waybillNumber,
                            carrierId = item.carrierHintId ?: "kr.cjlogistics",
                            itemName = item.itemNameHint,
                            senderName = item.senderNameHint,
                            groupTag = "자동수집",
                            iconKey = carrier?.iconKey ?: "box",
                            colorHex = carrier?.accentColorHex ?: 0xFF3482FF,
                            createdAtEpochMs = item.capturedAtEpochMs
                        )
                    )
                    onDone("송장을 등록했습니다 (상세 정보는 나중에 다시 조회하세요)")
                }
        }
    }

    fun dismissPendingCapture(id: String) {
        updateAndSave { prev ->
            prev.copy(pendingCaptures = prev.pendingCaptures.filterNot { it.id == id })
        }
    }

    fun updateGroupTag(shipmentId: String, tag: String) {
        updateAndSave { prev ->
            prev.copy(
                shipments = prev.shipments.map {
                    if (it.id == shipmentId) it.copy(groupTag = tag.trim()) else it
                }
            )
        }
    }

    fun addPhotoUri(shipmentId: String, uri: String) {
        updateAndSave { prev ->
            prev.copy(
                shipments = prev.shipments.map {
                    if (it.id == shipmentId && uri !in it.photoUris) {
                        it.copy(photoUris = (it.photoUris + uri).take(5))
                    } else it
                }
            )
        }
    }

    fun removePhotoUri(shipmentId: String, uri: String) {
        updateAndSave { prev ->
            prev.copy(
                shipments = prev.shipments.map {
                    if (it.id == shipmentId) it.copy(photoUris = it.photoUris - uri) else it
                }
            )
        }
    }

    // === Settings & Backup ===
    fun updateTheme(option: ThemeOption) = updateSettings { it.copy(themeOption = option) }
    fun updateThemeColorStyle(style: ThemeColorStyle) = updateSettings { it.copy(themeColorStyle = style) }
    fun updateCustomKeyColor(hex: Long?) = updateSettings { it.copy(customKeyColorHex = hex) }
    fun updateNavBarStyle(style: NavBarStyle) = updateSettings { it.copy(navBarStyle = style) }
    fun updateWideDualLayout(enabled: Boolean) = updateSettings { it.copy(wideDualLayoutEnabled = enabled) }
    fun updateAutoRefreshOnLaunch(enabled: Boolean) = updateSettings { it.copy(autoRefreshOnLaunch = enabled) }
    fun updateAutoDetectClipboard(enabled: Boolean) = updateSettings { it.copy(autoDetectClipboardWaybill = enabled) }
    fun updateAutoArchiveDeliveredDays(days: Int) {
        updateSettings { it.copy(autoArchiveDeliveredDays = days) }
        applyAutoArchiveIfNeeded()
    }
    fun updateBackgroundSync(
        enabled: Boolean,
        intervalHours: Int = _state.value.settings.backgroundSyncIntervalHours
    ) {
        val clamped = intervalHours.coerceIn(1, 24)
        updateSettings { it.copy(backgroundSyncEnabled = enabled, backgroundSyncIntervalHours = clamped) }
        ShipmentSyncScheduler.syncSchedule(getApplication(), enabled, clamped)
    }
    fun updateSweetTrackerApiKey(key: String) = updateSettings { it.copy(sweetTrackerApiKey = key.trim()) }
    fun updateCustomTrackerEndpoint(endpoint: String) = updateSettings {
        it.copy(customTrackerEndpoint = endpoint.trim().ifBlank { "https://apis.tracker.delivery" })
    }
    fun updateAutoCaptureNotifications(enabled: Boolean) = updateSettings {
        it.copy(autoCaptureNotificationsEnabled = enabled)
    }
    fun updateAutoCaptureRegisterImmediately(immediate: Boolean) = updateSettings {
        it.copy(autoCaptureRegisterImmediately = immediate)
    }
    fun toggleCapturePackage(packageName: String) = updateSettings { cur ->
        val next = if (packageName in cur.autoCaptureAllowedPackages) {
            cur.autoCaptureAllowedPackages - packageName
        } else {
            cur.autoCaptureAllowedPackages + packageName
        }
        cur.copy(autoCaptureAllowedPackages = next)
    }
    fun updateQuietHours(enabled: Boolean, startHour: Int, endHour: Int) = updateSettings {
        it.copy(
            quietHoursEnabled = enabled,
            quietHoursStartHour = startHour.coerceIn(0, 23),
            quietHoursEndHour = endHour.coerceIn(0, 23)
        )
    }
    fun toggleNotifyPhase(phase: StandardDeliveryPhase) = updateSettings { cur ->
        val next = if (phase in cur.notifyPhases) cur.notifyPhases - phase else cur.notifyPhases + phase
        cur.copy(notifyPhases = next)
    }
    fun updateNotifyAnomalies(enabled: Boolean) = updateSettings { it.copy(notifyAnomalies = enabled) }
    fun updatePersonalCustomsCode(pccc: String) = updateSettings {
        it.copy(personalCustomsCode = pccc.trim().uppercase())
    }
    fun updateBiometricLock(enabled: Boolean) = updateSettings { it.copy(biometricLockEnabled = enabled) }
    fun updateHomeSortOption(option: HomeSortOption) = updateSettings { it.copy(homeSortOption = option) }
    fun updateSwipeActions(right: SwipeActionOption, left: SwipeActionOption) = updateSettings {
        it.copy(swipeRightAction = right, swipeLeftAction = left)
    }
    fun updateDoorLockHint(hint: String) = updateSettings { it.copy(doorLockHint = hint.trim()) }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        updateAndSave { prev -> prev.copy(settings = transform(prev.settings)) }
    }

    fun exportBackupJson(includeSecrets: Boolean = false): String = repo.exportJson(_state.value, includeSecrets)

    fun previewBackupJson(raw: String): Result<ToraBoxDataState> = repo.parseBackupJson(raw)

    fun importBackupJson(raw: String): Result<Unit> = runCatching {
        val parsed = repo.parseBackupJson(raw).getOrThrow()
        updateAndSave { parsed }
    }

    fun resetAllData() {
        updateAndSave { prev -> repo.createResetState(prev.settings) }
    }

    // === In-App GitHub Updater (x2tora/tora-box) ===
    fun checkForUpdate(manual: Boolean = false, onFeedback: (String) -> Unit = {}) {
        if (_updateUiState.value.isChecking) return
        viewModelScope.launch {
            _updateUiState.update { it.copy(isChecking = true) }
            when (val outcome = AppUpdater.checkLatestRelease(BuildConfig.VERSION_NAME)) {
                is UpdateCheckOutcome.UpdateAvailable -> {
                    _updateUiState.update {
                        it.copy(
                            isChecking = false,
                            showDialog = true,
                            phase = AppUpdatePhase.Available(outcome.release),
                            lastStatusNote = "새 버전 v${outcome.release.versionName}"
                        )
                    }
                }
                is UpdateCheckOutcome.UpToDate -> {
                    _updateUiState.update {
                        it.copy(
                            isChecking = false,
                            lastStatusNote = "최신 버전입니다"
                        )
                    }
                    if (manual) onFeedback("현재 최신 버전(v${BuildConfig.VERSION_NAME})을 사용 중입니다")
                }
                is UpdateCheckOutcome.Failed -> {
                    _updateUiState.update {
                        it.copy(
                            isChecking = false,
                            lastStatusNote = outcome.message
                        )
                    }
                    if (manual) onFeedback(outcome.message)
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateUiState.update { it.copy(showDialog = false) }
    }

    fun startUpdateDownloadAndInstall() {
        val release = when (val p = _updateUiState.value.phase) {
            is AppUpdatePhase.Available -> p.release
            is AppUpdatePhase.Error -> p.release ?: return
            else -> return
        }
        val app = getApplication<Application>()
        viewModelScope.launch {
            _updateUiState.update {
                it.copy(
                    phase = AppUpdatePhase.Downloading(release, 0f, 0L, release.selectedAsset.sizeBytes)
                )
            }
            val dl = AppUpdater.downloadApk(app, release.selectedAsset) { prog, down, total ->
                _updateUiState.update {
                    it.copy(phase = AppUpdatePhase.Downloading(release, prog, down, total))
                }
            }
            dl.onSuccess { file ->
                downloadedApkFile = file
                val outcome = AppUpdater.launchApkInstaller(app, file)
                val needsPerm = outcome == InstallLaunchOutcome.NEEDS_UNKNOWN_SOURCE_PERMISSION
                _updateUiState.update {
                    it.copy(phase = AppUpdatePhase.ReadyToInstall(release, needsPermission = needsPerm))
                }
            }.onFailure { err ->
                _updateUiState.update {
                    it.copy(phase = AppUpdatePhase.Error(release, err.message ?: "다운로드에 실패했습니다"))
                }
            }
        }
    }

    fun retryApkInstall() {
        val file = downloadedApkFile ?: return
        val app = getApplication<Application>()
        val rel = (_updateUiState.value.phase as? AppUpdatePhase.ReadyToInstall)?.release ?: return
        val outcome = AppUpdater.launchApkInstaller(app, file)
        _updateUiState.update {
            it.copy(
                phase = AppUpdatePhase.ReadyToInstall(
                    rel,
                    needsPermission = outcome == InstallLaunchOutcome.NEEDS_UNKNOWN_SOURCE_PERMISSION
                )
            )
        }
    }

    fun openInstallUnknownSourcesSettings() {
        AppUpdater.openInstallPermissionSettings(getApplication())
    }
}
