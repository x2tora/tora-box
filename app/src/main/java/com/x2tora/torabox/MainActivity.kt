package com.x2tora.torabox

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.x2tora.torabox.model.ThemeOption
import com.x2tora.torabox.ui.screens.MainScaffoldScreen
import com.x2tora.torabox.ui.theme.ToraBoxTheme
import com.x2tora.torabox.viewmodel.ToraBoxViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
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
                MainScaffoldScreen(viewModel = viewModel)
            }
        }
    }
}
