package com.x2tora.torabox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x2tora.torabox.update.RemoteReleaseInfo
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

sealed interface AppUpdatePhase {
    data object Idle : AppUpdatePhase
    data class Available(val release: RemoteReleaseInfo) : AppUpdatePhase
    data class Downloading(
        val release: RemoteReleaseInfo,
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : AppUpdatePhase
    data class ReadyToInstall(
        val release: RemoteReleaseInfo,
        val needsPermission: Boolean
    ) : AppUpdatePhase
    data class Error(
        val release: RemoteReleaseInfo?,
        val message: String
    ) : AppUpdatePhase
}

data class AppUpdateUiState(
    val currentVersionName: String,
    val isChecking: Boolean = false,
    val showDialog: Boolean = false,
    val phase: AppUpdatePhase = AppUpdatePhase.Idle,
    val lastStatusNote: String? = null
)

@Composable
fun AppUpdateDialog(
    uiState: AppUpdateUiState,
    onDismiss: () -> Unit,
    onStartDownload: () -> Unit,
    onRetryInstall: () -> Unit,
    onOpenPermissionSettings: () -> Unit
) {
    val activePhase = uiState.phase
    val title = when (activePhase) {
        is AppUpdatePhase.Available -> "새 버전 업데이트 안내"
        is AppUpdatePhase.Downloading -> "업데이트 다운로드 중"
        is AppUpdatePhase.ReadyToInstall -> if (activePhase.needsPermission) "설치 권한 허용 필요" else "업데이트 설치 준비 완료"
        is AppUpdatePhase.Error -> "업데이트 오류"
        AppUpdatePhase.Idle -> "앱 업데이트"
    }

    val isDownloading = activePhase is AppUpdatePhase.Downloading

    OverlayDialog(
        show = uiState.showDialog,
        title = title,
        onDismissRequest = { if (!isDownloading) onDismiss() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activePhase) {
                is AppUpdatePhase.Available -> {
                    val rel = activePhase.release
                    val sizeMb = if (rel.selectedAsset.sizeBytes > 0L) {
                        String.format(Locale.US, "%.2f MB", rel.selectedAsset.sizeBytes / (1024.0 * 1024.0))
                    } else "크기 확인 중"
                    Text(
                        text = "v${uiState.currentVersionName}  →  v${rel.versionName}",
                        style = MiuixTheme.textStyles.headline2.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MiuixTheme.colorScheme.primary
                    )
                    Text(
                        text = "기기 맞춤 패키지: ${rel.matchedAbiLabel} ($sizeMb)",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    if (rel.releaseNotes.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = rel.releaseNotes,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(text = "나중에", onClick = onDismiss, modifier = Modifier.weight(1f))
                        TextButton(
                            text = "지금 업데이트",
                            onClick = onStartDownload,
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is AppUpdatePhase.Downloading -> {
                    val pct = (activePhase.progress * 100f).toInt().coerceIn(0, 100)
                    val downloadedMb = String.format(Locale.US, "%.2f", activePhase.downloadedBytes / (1024.0 * 1024.0))
                    val totalMb = if (activePhase.totalBytes > 0L) {
                        String.format(Locale.US, "%.2f", activePhase.totalBytes / (1024.0 * 1024.0))
                    } else "?"
                    Text(
                        text = "v${activePhase.release.versionName} 내려받는 중... $pct%",
                        style = MiuixTheme.textStyles.body2.copy(fontFeatureSettings = "tnum")
                    )
                    LinearProgressIndicator(
                        progress = activePhase.progress.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "$downloadedMb MB / $totalMb MB (${activePhase.release.matchedAbiLabel})",
                        style = MiuixTheme.textStyles.footnote2.copy(fontFeatureSettings = "tnum"),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                }

                is AppUpdatePhase.ReadyToInstall -> {
                    Text(
                        text = if (activePhase.needsPermission) {
                            "보안 설정에서 '토라박스'의 출처를 알 수 없는 앱 설치 권한을 허용한 뒤 [설치 실행]을 눌러 주세요."
                        } else {
                            "업데이트 파일 다운로드가 완료되었습니다. 설치 화면이 열리지 않으면 아래 [설치 실행]을 눌러 주세요."
                        },
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(text = "닫기", onClick = onDismiss, modifier = Modifier.weight(1f))
                        if (activePhase.needsPermission) {
                            TextButton(text = "권한 설정", onClick = onOpenPermissionSettings, modifier = Modifier.weight(1f))
                        }
                        TextButton(
                            text = "설치 실행",
                            onClick = onRetryInstall,
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is AppUpdatePhase.Error -> {
                    Text(
                        text = activePhase.message,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.error
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(text = "닫기", onClick = onDismiss, modifier = Modifier.weight(1f))
                        if (activePhase.release != null) {
                            TextButton(
                                text = "다시 시도",
                                onClick = onStartDownload,
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                AppUpdatePhase.Idle -> {
                    TextButton(text = "닫기", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
