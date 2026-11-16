package com.x2tora.torabox.ui.components

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.x2tora.torabox.ui.theme.LocalAppBackdrop
import com.x2tora.torabox.ui.theme.LocalIsDarkTheme
import com.x2tora.torabox.ui.theme.LocalLiquidGlassEnabled
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@Stable
data class ToastEntry(
    val id: Long,
    val message: String,
    val durationMs: Long = 3400L,
    val isExiting: Boolean = false,
    val exitDelayMs: Long = 0L
)

@Stable
class StackedToastState {
    private var nextId by mutableLongStateOf(0L)
    val entries = mutableStateListOf<ToastEntry>()

    fun show(message: String, durationMs: Long = 3400L) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        entries.removeAll { it.isExiting }
        val id = ++nextId
        entries.add(0, ToastEntry(id = id, message = trimmed, durationMs = durationMs))
        if (entries.size > 5) {
            entries.removeAt(entries.lastIndex)
        }
    }

    fun requestDismiss(id: Long) {
        val idx = entries.indexOfFirst { it.id == id }
        if (idx >= 0 && !entries[idx].isExiting) {
            entries[idx] = entries[idx].copy(isExiting = true, exitDelayMs = 0L)
        }
    }

    fun requestDismissAllCascade(staggerMs: Long = 50L, skipId: Long? = null) {
        var step = 0
        for (i in entries.indices) {
            val entry = entries[i]
            if (entry.id != skipId && !entry.isExiting) {
                entries[i] = entry.copy(isExiting = true, exitDelayMs = step * staggerMs)
                step++
            }
        }
    }

    fun dismiss(id: Long) {
        entries.removeAll { it.id == id }
    }
}

@Composable
fun rememberStackedToastState(): StackedToastState = remember { StackedToastState() }

@Composable
fun StackedToastHost(
    state: StackedToastState,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = LocalAppBackdrop.current,
    isLiquidGlass: Boolean = LocalLiquidGlassEnabled.current
) {
    val entries = state.entries
    if (entries.isEmpty()) return

    val frontEntry = entries.firstOrNull()
    LaunchedEffect(frontEntry?.id, frontEntry?.isExiting) {
        if (frontEntry != null && !frontEntry.isExiting) {
            delay(frontEntry.durationMs)
            state.requestDismissAllCascade(staggerMs = 50L)
        }
    }

    val visibleEntries = entries.take(5)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        for (depth in visibleEntries.lastIndex downTo 0) {
            val item = visibleEntries[depth]
            key(item.id) {
                val targetScale = (1f - depth * 0.04f).coerceAtLeast(0.84f)
                val targetOffsetY = (-8 * depth).dp
                val targetAlpha = (1f - depth * 0.14f).coerceAtLeast(0.44f)

                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(dampingRatio = 0.74f, stiffness = 195f),
                    label = "toastDeckScale"
                )
                val animatedOffsetY by animateDpAsState(
                    targetValue = targetOffsetY,
                    animationSpec = spring(dampingRatio = 0.74f, stiffness = 195f),
                    label = "toastDeckOffsetY"
                )
                val animatedAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(durationMillis = 280),
                    label = "toastDeckAlpha"
                )

                SwipeableToastCard(
                    message = item.message,
                    isFront = depth == 0,
                    isExiting = item.isExiting,
                    exitDelayMs = item.exitDelayMs,
                    deckAlpha = animatedAlpha,
                    backdrop = backdrop,
                    isLiquidGlass = isLiquidGlass,
                    onSwipeStartDismissStack = { state.requestDismissAllCascade(staggerMs = 45L, skipId = item.id) },
                    onRemove = { state.dismiss(item.id) },
                    modifier = Modifier
                        .zIndex((visibleEntries.size - depth).toFloat())
                        .offset(y = animatedOffsetY)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                )
            }
        }
    }
}

@Composable
private fun SwipeableToastCard(
    message: String,
    isFront: Boolean,
    isExiting: Boolean,
    exitDelayMs: Long,
    deckAlpha: Float,
    backdrop: LayerBackdrop?,
    isLiquidGlass: Boolean,
    onSwipeStartDismissStack: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val scope = rememberCoroutineScope()
    val dragX = remember { Animatable(0f) }
    val exitShiftY = remember { Animatable(0f) }
    val fadeAnim = remember { Animatable(0f) }
    var cardWidthPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        fadeAnim.animateTo(1f, animationSpec = tween(durationMillis = 220))
    }

    LaunchedEffect(isExiting) {
        if (isExiting) {
            if (exitDelayMs > 0L) delay(exitDelayMs)
            coroutineScope {
                launch { fadeAnim.animateTo(0f, animationSpec = tween(durationMillis = 200)) }
                launch { exitShiftY.animateTo(22f, animationSpec = tween(durationMillis = 200)) }
            }
            onRemove()
        }
    }

    val swipeFade = if (cardWidthPx > 0) {
        (1f - (abs(dragX.value) / (cardWidthPx * 0.72f))).coerceIn(0f, 1f)
    } else 1f
    val combinedAlpha = (deckAlpha * fadeAnim.value * swipeFade).coerceIn(0f, 1f)

    val containerColor = if (isFront) {
        if (isDark) MiuixTheme.colorScheme.surfaceContainerHighest else MiuixTheme.colorScheme.surfaceContainerHigh
    } else {
        if (isDark) MiuixTheme.colorScheme.surfaceContainerHigh else MiuixTheme.colorScheme.surfaceContainer
    }
    val borderColor = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.14f else 0.09f)
    val corner = 18.dp
    val shape = RoundedCornerShape(corner)
    val useRealLiquidGlass = isLiquidGlass &&
        backdrop != null &&
        isRuntimeShaderSupported() &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val surfaceModifier = if (useRealLiquidGlass) {
        Modifier.liquidGlassSurface(
            backdrop = backdrop,
            shape = shape,
            cornerRadius = corner,
            blurRadius = 2.5.dp,
            refractionHeight = 10.dp,
            refractionAmount = 7.dp,
            chromaticAberration = 2.4.dp,
            surfaceTint = if (isDark) {
                Color(0xFF141822).copy(alpha = if (isFront) 0.44f else 0.34f)
            } else {
                Color(0xFFF6F7FB).copy(alpha = if (isFront) 0.48f else 0.38f)
            },
            grainAlpha = 0.06f,
            fallbackColor = containerColor
        )
    } else {
        Modifier
            .squircleClip(corner)
            .squircleBackground(color = containerColor, cornerRadius = corner)
            .border(width = 0.75.dp, color = borderColor, shape = shape)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { cardWidthPx = it.width }
            .offset { IntOffset(dragX.value.roundToInt(), exitShiftY.value.roundToInt()) }
            .alpha(combinedAlpha)
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = if (isFront) 14.dp else 8.dp,
                    color = Color.Black,
                    alpha = if (isDark) 0.34f else 0.13f
                )
            )
            .then(surfaceModifier)
            .then(
                if (isFront && !isExiting) {
                    Modifier
                        .pointerInput(Unit) { detectTapGestures { } }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val current = dragX.value
                                    val threshold = size.width * 0.22f
                                    if (abs(current) > threshold) {
                                        val direction = if (current == 0f) 1f else sign(current)
                                        onSwipeStartDismissStack()
                                        scope.launch {
                                            dragX.animateTo(
                                                targetValue = direction * size.width * 0.9f,
                                                animationSpec = tween(durationMillis = 200)
                                            )
                                            onRemove()
                                        }
                                    } else {
                                        scope.launch {
                                            dragX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 190f)
                                            )
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        dragX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 190f)
                                        )
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    scope.launch { dragX.snapTo(dragX.value + dragAmount) }
                                }
                            )
                        }
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = MiuixIcons.Ok,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
