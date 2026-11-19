package com.x2tora.torabox

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.x2tora.torabox.model.ThemeOption
import com.x2tora.torabox.notification.NotificationHelper
import com.x2tora.torabox.security.BiometricAuthHelper
import com.x2tora.torabox.ui.components.MiuixGlassIcon
import com.x2tora.torabox.ui.screens.MainScaffoldScreen
import com.x2tora.torabox.ui.theme.ToraBoxTheme
import com.x2tora.torabox.viewmodel.ToraBoxViewModel
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : FragmentActivity() {
    private var pendingSharedText by mutableStateOf<String?>(null)
    private var pendingShipmentId by mutableStateOf<String?>(null)
    private var pendingShortcutAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        consumeIntent(intent)
        val splashDeadlineMs = SystemClock.elapsedRealtime() + if (savedInstanceState == null) 550L else 0L
        splashScreen.setKeepOnScreenCondition {
            SystemClock.elapsedRealtime() < splashDeadlineMs
        }
        splashScreen.setOnExitAnimationListener { splashViewProvider ->
            val interpolator = DecelerateInterpolator(1.6f)
            val containerFade = ObjectAnimator.ofFloat(splashViewProvider.view, View.ALPHA, 1f, 0f).apply {
                duration = 300L
                this.interpolator = interpolator
            }
            val animators = mutableListOf<android.animation.Animator>(containerFade)
            runCatching { splashViewProvider.iconView }.getOrNull()?.let { icon ->
                animators += ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.08f).apply {
                    duration = 300L
                    this.interpolator = interpolator
                }
                animators += ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.08f).apply {
                    duration = 300L
                    this.interpolator = interpolator
                }
            }
            AnimatorSet().apply {
                playTogether(animators)
                doOnEnd { splashViewProvider.remove() }
                start()
            }
        }
        enableEdgeToEdge()

        setContent {
            val viewModel: ToraBoxViewModel = viewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (state.settings.themeOption) {
                ThemeOption.SYSTEM -> systemDark
                ThemeOption.LIGHT -> false
                ThemeOption.DARK -> true
            }

            LaunchedEffect(effectiveDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { effectiveDark },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { effectiveDark }
                )
            }

            ToraBoxTheme(
                themeOption = state.settings.themeOption,
                colorStyle = state.settings.resolvedColorStyle(),
                customKeyColorHex = state.settings.customKeyColorHex
            ) {
                var isUnlocked by remember { mutableStateOf(false) }
                val needsLock = state.settings.biometricLockEnabled &&
                    !isUnlocked &&
                    BiometricAuthHelper.canAuthenticate(this@MainActivity)

                LaunchedEffect(needsLock) {
                    if (needsLock) {
                        BiometricAuthHelper.authenticate(
                            activity = this@MainActivity,
                            onSuccess = { isUnlocked = true },
                            onError = {}
                        )
                    }
                }

                if (needsLock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MiuixTheme.colorScheme.surface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
                            cornerRadius = 22.dp,
                            insideMargin = PaddingValues(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MiuixGlassIcon(
                                    icon = MiuixIcons.Lock,
                                    contentDescription = null,
                                    accentColor = MiuixTheme.colorScheme.primary,
                                    size = 56.dp,
                                    iconSize = 26.dp
                                )
                                Text(
                                    text = "토라박스 잠금됨",
                                    style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.Bold),
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "등록된 생체인증 또는 기기 비밀번호로 잠금을 해제하세요.",
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                                TextButton(
                                    text = "잠금 해제",
                                    onClick = {
                                        BiometricAuthHelper.authenticate(
                                            activity = this@MainActivity,
                                            onSuccess = { isUnlocked = true },
                                            onError = {}
                                        )
                                    },
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    MainScaffoldScreen(
                        viewModel = viewModel,
                        pendingSharedText = pendingSharedText,
                        onConsumeSharedText = { pendingSharedText = null },
                        pendingShipmentId = pendingShipmentId,
                        onConsumeShipmentId = { pendingShipmentId = null },
                        pendingShortcutAction = pendingShortcutAction,
                        onConsumeShortcutAction = { pendingShortcutAction = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        val shipmentId = intent.getStringExtra(NotificationHelper.EXTRA_SHIPMENT_ID)
        if (!shipmentId.isNullOrBlank()) {
            pendingShipmentId = shipmentId
            intent.removeExtra(NotificationHelper.EXTRA_SHIPMENT_ID)
        }
        when (intent.action) {
            Intent.ACTION_SEND -> if (intent.type == "text/plain") {
                val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!shared.isNullOrBlank()) pendingSharedText = shared
            }
            "com.x2tora.torabox.ACTION_SCAN_BARCODE",
            "com.x2tora.torabox.ACTION_ADD_SHIPMENT",
            "com.x2tora.torabox.ACTION_CLIPBOARD_TRACK" -> {
                pendingShortcutAction = intent.action
            }
        }
    }
}
