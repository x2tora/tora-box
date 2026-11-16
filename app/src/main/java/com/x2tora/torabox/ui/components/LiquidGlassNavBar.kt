package com.x2tora.torabox.ui.components

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints as ComposeConstraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x2tora.torabox.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language
import top.yukonga.miuix.kmp.basic.Badge
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.shader.asAndroidRuntimeShader
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

private val DockHeight = 62.dp
private const val DockPressedScale = 1.04f
private val DockBlurRadius = 3.5.dp
private val DockRefractionHeight = 12.dp
private val DockRefractionAmount = 7.5.dp
private val DockChromaticAberration = 2.4.dp

private val GlassSurfaceTint = Color(0xFF6E7D96).copy(alpha = 0.03f)
private const val GlassGrainAlpha = 0.08f
private const val GRAIN_TILE = 96

private val DockLensInset = 4.dp
private val SliderDragStartSlop = 4.dp
private val SliderRefractionHeight = 6.dp
private val SliderRefractionAmount = 4.5.dp
private val SliderChromaticAberration = 1.6.dp
private val SliderPressedOverhang = 1.5.dp
private const val SliderStretchMax = 0.12f

@Language("AGSL")
private const val LIQUID_LENS_SHADER = """
    uniform shader content;
    uniform float2 uPad;
    uniform float2 uSize;
    uniform float uRadius;
    uniform float uHeight;
    uniform float uAmount;
    uniform float uChromatic;

    float sdPill(float2 p, float2 halfSize, float radius) {
        float2 q = abs(p) - (halfSize - float2(radius));
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    }

    half4 main(float2 fragCoord) {
        float2 halfSize = max(uSize * 0.5, float2(1.0));
        float maxR = min(halfSize.x, halfSize.y);
        float r = (uRadius > 0.0) ? min(uRadius, maxR) : maxR;
        float2 local = (fragCoord - uPad) - halfSize;

        float sd = sdPill(local, halfSize, r);
        float distInward = -sd;
        if (distInward <= 0.0 || distInward >= uHeight) {
            return content.eval(fragCoord);
        }

        float eps = 1.5;
        float gx = sdPill(local + float2(eps, 0.0), halfSize, r) - sdPill(local - float2(eps, 0.0), halfSize, r);
        float gy = sdPill(local + float2(0.0, eps), halfSize, r) - sdPill(local - float2(0.0, eps), halfSize, r);
        float2 edgeNormal = float2(gx, gy);
        float gLen = length(edgeNormal);
        float2 dir = (gLen > 0.0001) ? (edgeNormal / gLen) : float2(0.0);

        float centerLen = length(local);
        if (centerLen > 0.0001) {
            dir = normalize(dir + (local / centerLen) * 0.18);
        }

        float t = clamp(1.0 - distInward / max(uHeight, 1.0), 0.0, 1.0);
        float bend = t * t * (3.0 - 2.0 * t);
        float2 maxCoord = uPad * 2.0 + uSize - float2(0.5);
        float2 baseCoord = fragCoord - dir * (bend * uAmount);
        float2 chromaShift = dir * (bend * uChromatic);

        half red = content.eval(clamp(baseCoord + chromaShift, float2(0.5), maxCoord)).r;
        half4 greenSample = content.eval(clamp(baseCoord, float2(0.5), maxCoord));
        half blue = content.eval(clamp(baseCoord - chromaShift, float2(0.5), maxCoord)).b;
        return half4(red, greenSample.g, blue, greenSample.a);
    }
"""

@Composable
private fun rememberGrainBrush(): ShaderBrush = remember {
    val random = Random(0x5EEDBEEF)
    val pixels = IntArray(GRAIN_TILE * GRAIN_TILE) {
        val alpha = random.nextInt(0, 49)
        val level = random.nextInt(60, 201)
        (alpha shl 24) or (level shl 16) or (level shl 8) or level
    }
    val bitmap = Bitmap.createBitmap(pixels, GRAIN_TILE, GRAIN_TILE, Bitmap.Config.ARGB_8888)
    ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
}

private fun BackdropEffectScope.chainPlatformRenderEffect(effect: android.graphics.RenderEffect) {
    val prev = renderEffect
    renderEffect = if (prev != null) {
        android.graphics.RenderEffect.createChainEffect(effect, prev.asAndroidRenderEffect())
            .asComposeRenderEffect()
    } else {
        effect.asComposeRenderEffect()
    }
}

private fun BackdropEffectScope.vibrancySaturation(saturation: Float = 1.5f) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val matrix = ColorMatrix().apply { setSaturation(saturation) }
    chainPlatformRenderEffect(
        android.graphics.RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix))
    )
}

private fun BackdropEffectScope.liquidLensEffect(
    cornerRadiusPx: Float,
    refractionHeightPx: Float,
    refractionAmountPx: Float,
    chromaticPx: Float
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val scale = downscaleFactor.coerceAtLeast(1).toFloat()
    val shader = obtainRuntimeShader("ToraBoxLiquidLens", LIQUID_LENS_SHADER)
    shader.setFloatUniform("uPad", padding / scale, padding / scale)
    shader.setFloatUniform("uSize", size.width / scale, size.height / scale)
    shader.setFloatUniform("uRadius", (cornerRadiusPx / scale).coerceAtLeast(0f))
    shader.setFloatUniform("uHeight", (refractionHeightPx / scale).coerceAtLeast(1f))
    shader.setFloatUniform("uAmount", refractionAmountPx / scale)
    shader.setFloatUniform("uChromatic", chromaticPx / scale)
    chainPlatformRenderEffect(
        android.graphics.RenderEffect.createRuntimeShaderEffect(
            shader.asAndroidRuntimeShader(),
            "content"
        )
    )
}

@Composable
private fun rememberCombinedBackdrop(vararg backdrops: Backdrop?): Backdrop? {
    val active = backdrops.filterNotNull()
    if (active.isEmpty()) return null
    if (active.size == 1) return active.first()
    return remember(active) {
        object : Backdrop {
            override val isCoordinatesDependent: Boolean
                get() = active.any { it.isCoordinatesDependent }

            override fun DrawScope.drawBackdrop(
                density: Density,
                coordinates: LayoutCoordinates?,
                layerBlock: (GraphicsLayerScope.() -> Unit)?,
                downscaleFactor: Int
            ) {
                active.forEach { backdrop ->
                    with(backdrop) {
                        drawBackdrop(
                            density = density,
                            coordinates = coordinates,
                            layerBlock = layerBlock,
                            downscaleFactor = downscaleFactor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.liquidGlassSurface(
    backdrop: Backdrop?,
    shape: Shape,
    cornerRadius: Dp = 0.dp,
    blurRadius: Dp = DockBlurRadius,
    refractionHeight: Dp = DockRefractionHeight,
    refractionAmount: Dp = DockRefractionAmount,
    chromaticAberration: Dp = DockChromaticAberration,
    refraction: Boolean = true,
    surfaceTint: Color = GlassSurfaceTint,
    grainAlpha: Float = GlassGrainAlpha,
    fallbackColor: Color = MiuixTheme.colorScheme.surfaceContainer
): Modifier {
    val isDark = LocalIsDarkTheme.current
    if (backdrop == null || !isRuntimeShaderSupported() || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        val resolvedFallback = if (fallbackColor.alpha >= 0.99f) fallbackColor.copy(alpha = 0.94f) else fallbackColor
        return this
            .clip(shape)
            .background(resolvedFallback)
            .border(
                width = 0.75.dp,
                color = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f),
                shape = shape
            )
    }

    val grainBrush = rememberGrainBrush()
    val highlight = if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight

    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        highlight = { highlight },
        onDrawSurface = {
            if (surfaceTint.alpha > 0f) drawRect(color = surfaceTint)
            if (grainAlpha > 0f) drawRect(brush = grainBrush, alpha = grainAlpha)
        },
        effects = {
            vibrancySaturation(1.5f)
            blur(blurRadius.toPx())
            if (refraction) {
                liquidLensEffect(
                    cornerRadiusPx = cornerRadius.toPx(),
                    refractionHeightPx = refractionHeight.toPx(),
                    refractionAmountPx = refractionAmount.toPx(),
                    chromaticPx = chromaticAberration.toPx()
                )
            }
        }
    )
}

private data class NavSlot(
    val slotIndex: Int,
    val tabIndex: Int?,
    val icon: ImageVector,
    val label: String
)

private fun tabToSlot(tabIndex: Int): Int = if (tabIndex < 2) tabIndex else tabIndex + 1
private fun slotToTab(slotIndex: Int): Int = if (slotIndex < 2) slotIndex else slotIndex - 1

private fun nearestAllSlot(
    leftPx: Float,
    baseOffsetPx: Float,
    itemWidthPx: Float
): Int {
    if (itemWidthPx <= 0f) return 0
    val approxSlot = ((leftPx - baseOffsetPx) / itemWidthPx).roundToInt()
    return approxSlot.coerceIn(0, 4)
}

private fun stretchOf(leftPx: Float, anchorPx: Float, itemWidthPx: Float): Float {
    if (itemWidthPx <= 0f) return 0f
    val t = (abs(leftPx - anchorPx) / itemWidthPx).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Composable
fun FloatingLiquidGlassNavBar(
    backdrop: LayerBackdrop?,
    selectedTab: Int,
    isCenterAddSelected: Boolean = false,
    activeDeliveryCount: Int,
    outForDeliveryCount: Int,
    onSelectTab: (Int) -> Unit,
    onCenterAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    useLensTint: Boolean = true
) {
    var barPressed by remember { mutableStateOf(false) }
    val barScale by animateFloatAsState(
        targetValue = if (barPressed) DockPressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 220f),
        label = "dockPressScale"
    )

    val slots = remember {
        listOf(
            NavSlot(0, 0, MiuixIcons.Regular.Home, "배송"),
            NavSlot(1, 1, MiuixIcons.Scan, "추측"),
            NavSlot(2, null, MiuixIcons.Add, "등록"),
            NavSlot(3, 2, MiuixIcons.ListView, "택배사"),
            NavSlot(4, 3, MiuixIcons.Folder, "보관함")
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DockHeight)
            .graphicsLayer {
                scaleX = barScale
                scaleY = barScale
            }
    ) {
        LiquidGlassNavBarContent(
            slots = slots,
            selectedTab = selectedTab,
            isCenterAddSelected = isCenterAddSelected,
            activeDeliveryCount = activeDeliveryCount,
            outForDeliveryCount = outForDeliveryCount,
            onSelectTab = onSelectTab,
            onCenterAddClick = onCenterAddClick,
            pageBackdrop = backdrop,
            contentHeight = DockHeight,
            useLensTint = useLensTint,
            onBarPressedChange = { barPressed = it },
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
private fun LiquidGlassNavBarContent(
    slots: List<NavSlot>,
    selectedTab: Int,
    isCenterAddSelected: Boolean,
    activeDeliveryCount: Int,
    outForDeliveryCount: Int,
    onSelectTab: (Int) -> Unit,
    onCenterAddClick: () -> Unit,
    pageBackdrop: LayerBackdrop?,
    contentHeight: Dp,
    useLensTint: Boolean,
    onBarPressedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val dockBackdrop = rememberLayerBackdrop()
    val lensBackdrop = rememberCombinedBackdrop(pageBackdrop, dockBackdrop)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val slotCount = slots.size
    var barWidthPx by remember { mutableIntStateOf(0) }

    val insetPx = with(density) { DockLensInset.toPx() }
    val innerWidthPx = (barWidthPx.toFloat() - insetPx * 2f).coerceAtLeast(0f)
    val itemWidthPx = if (innerWidthPx <= 0f) 0f else innerWidthPx / slotCount
    val baseOffsetPx = insetPx
    val sliderWidthPx = itemWidthPx
    val sliderHeightPx = (with(density) { contentHeight.toPx() } - insetPx * 2f).coerceAtLeast(0f)

    val targetSlot = if (isCenterAddSelected) 2 else tabToSlot(selectedTab.coerceIn(0, 3))
    val targetOffsetPx = itemWidthPx * targetSlot + baseOffsetPx

    val interactionSources = remember(slotCount) { List(slotCount) { MutableInteractionSource() } }
    var pressedCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(interactionSources) {
        coroutineScope {
            interactionSources.forEach { src ->
                launch {
                    src.interactions.collect { interaction ->
                        when (interaction) {
                            is PressInteraction.Press -> pressedCount++
                            is PressInteraction.Release, is PressInteraction.Cancel ->
                                pressedCount = (pressedCount - 1).coerceAtLeast(0)
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
    val anyPressed = pressedCount > 0

    var sliderLeftPx by remember { mutableFloatStateOf(0f) }
    val fingerTargetState = remember { mutableFloatStateOf(0f) }
    val restingTargetState = remember { mutableFloatStateOf(0f) }
    SideEffect { restingTargetState.floatValue = targetOffsetPx }

    val springVelocity = remember { mutableFloatStateOf(0f) }
    val draggingState = remember { mutableStateOf(false) }

    val pressed = anyPressed || draggingState.value
    SideEffect { onBarPressedChange(pressed) }

    var dragHighlightSlot by remember { mutableStateOf<Int?>(null) }
    val anchorSlot = dragHighlightSlot ?: targetSlot
    val anchorPx = itemWidthPx * anchorSlot + baseOffsetPx

    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 220f),
        label = "sliderPress"
    )
    val pressOutsetPx = with(density) { SliderPressedOverhang.toPx() } * pressProgress

    LaunchedEffect(itemWidthPx) {
        if (itemWidthPx <= 0f) return@LaunchedEffect
        sliderLeftPx = restingTargetState.floatValue
        springVelocity.floatValue = 0f

        val stiffness = 110f
        val dampingRest = 2f * 0.70f * sqrt(stiffness)
        val dampingDrag = 2f * 1.0f * sqrt(stiffness)
        var lastNanos = 0L

        while (true) {
            val now = withFrameNanos { it }
            val dt = if (lastNanos == 0L) 0f else (now - lastNanos) / 1_000_000_000f
            lastNanos = now
            if (dt <= 0f) continue

            val safeDt = dt.coerceAtMost(0.05f)
            val dragging = draggingState.value
            val target = if (dragging) fingerTargetState.floatValue else restingTargetState.floatValue
            var v = springVelocity.floatValue
            v += (stiffness * (target - sliderLeftPx) - (if (dragging) dampingDrag else dampingRest) * v) * safeDt
            springVelocity.floatValue = v
            sliderLeftPx += v * safeDt
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(contentHeight)
            .onSizeChanged { barWidthPx = it.width }
            .pointerInput(itemWidthPx) {
                if (itemWidthPx <= 0f) return@pointerInput
                val minLeft = baseOffsetPx
                val maxLeft = itemWidthPx * (slotCount - 1) + baseOffsetPx
                val slopPx = with(density) { SliderDragStartSlop.toPx() }
                val edgeGuardPx = with(density) { 16.dp.toPx() }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    val startX = down.position.x
                    val startY = down.position.y
                    if (startX < edgeGuardPx || startX > size.width - edgeGuardPx) {
                        return@awaitEachGesture
                    }
                    var started = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null || !change.pressed) {
                            if (started) {
                                draggingState.value = false
                                dragHighlightSlot = null
                                val snapSlot = nearestAllSlot(
                                    leftPx = fingerTargetState.floatValue,
                                    baseOffsetPx = baseOffsetPx,
                                    itemWidthPx = itemWidthPx
                                )
                                if (snapSlot == 2) {
                                    onCenterAddClick()
                                } else {
                                    onSelectTab(slotToTab(snapSlot))
                                }
                            }
                            break
                        }
                        if (change.isConsumed) {
                            if (started) {
                                draggingState.value = false
                                dragHighlightSlot = null
                            }
                            break
                        }

                        val dx = change.position.x - startX
                        val dy = change.position.y - startY
                        if (!started) {
                            if (abs(dy) > slopPx && abs(dy) > abs(dx)) break
                            if (abs(dx) < slopPx) continue
                            started = true
                            draggingState.value = true
                            val initialTarget = (change.position.x - sliderWidthPx / 2f).coerceIn(minLeft, maxLeft)
                            fingerTargetState.floatValue = initialTarget
                            dragHighlightSlot = nearestAllSlot(initialTarget, baseOffsetPx, itemWidthPx)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        change.consume()

                        val next = (change.position.x - sliderWidthPx / 2f).coerceIn(minLeft, maxLeft)
                        fingerTargetState.floatValue = next
                        val nearest = nearestAllSlot(next, baseOffsetPx, itemWidthPx)
                        if (nearest != dragHighlightSlot) {
                            dragHighlightSlot = nearest
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    }
                }
            }
    ) {
        // Layer 1: Dock glass surface + items (badges excluded)
        Box(
            modifier = Modifier
                .matchParentSize()
                .layerBackdrop(dockBackdrop)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .liquidGlassSurface(
                        backdrop = pageBackdrop,
                        shape = RoundedCornerShape(50),
                        cornerRadius = contentHeight / 2f,
                        blurRadius = DockBlurRadius,
                        refractionHeight = DockRefractionHeight,
                        refractionAmount = DockRefractionAmount,
                        chromaticAberration = DockChromaticAberration,
                        refraction = true,
                        surfaceTint = if (isDark) {
                            Color(0xFF14161B).copy(alpha = 0.56f)
                        } else {
                            Color(0xFFF6F7FB).copy(alpha = 0.60f)
                        },
                        grainAlpha = if (isDark) 0.05f else 0.07f,
                        fallbackColor = MiuixTheme.colorScheme.surfaceContainer
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight)
                    .padding(horizontal = DockLensInset),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                slots.forEachIndexed { slotIdx, slot ->
                    if (slot.tabIndex == null) {
                        val centerActive = isCenterAddSelected || dragHighlightSlot == 2
                        val addIconRotation by animateFloatAsState(
                            targetValue = if (centerActive) 45f else 0f,
                            animationSpec = spring(dampingRatio = 0.70f, stiffness = 190f),
                            label = "centerAddRotation"
                        )
                        val addPillScale by animateFloatAsState(
                            targetValue = if (centerActive) 1.07f else 1f,
                            animationSpec = spring(dampingRatio = 0.70f, stiffness = 210f),
                            label = "centerAddScale"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = interactionSources[slotIdx],
                                    indication = null,
                                    role = Role.Button,
                                    onClick = onCenterAddClick
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 50.dp, height = 44.dp)
                                    .graphicsLayer {
                                        scaleX = addPillScale
                                        scaleY = addPillScale
                                    }
                                    .squircleBackground(
                                        color = MiuixTheme.colorScheme.primary,
                                        cornerRadius = 22.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = slot.icon,
                                    contentDescription = if (centerActive) "빠른 메뉴 닫기" else "송장 등록 메뉴",
                                    tint = MiuixTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer { rotationZ = addIconRotation }
                                )
                            }
                        }
                    } else {
                        val selected = slotIdx == anchorSlot
                        val tint by animateColorAsState(
                            targetValue = if (selected) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurfaceVariantSummary
                            },
                            animationSpec = tween(260),
                            label = "navTint_$slotIdx"
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = interactionSources[slotIdx],
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = { onSelectTab(slot.tabIndex) }
                                ),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = slot.icon,
                                contentDescription = slot.label,
                                modifier = Modifier.size(22.dp),
                                tint = tint
                            )
                            Text(
                                text = slot.label,
                                color = tint,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Layer 2: Concentric Liquid Glass Selector Lens
        if (sliderWidthPx > 0f && sliderHeightPx > 0f) {
            val lensCornerRadius = (contentHeight - DockLensInset * 2f) / 2f
            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val w = (sliderWidthPx + 2f * pressOutsetPx).roundToInt().coerceAtLeast(0)
                        val h = (sliderHeightPx + 2f * pressOutsetPx).roundToInt().coerceAtLeast(0)
                        val placeable = measurable.measure(ComposeConstraints.fixed(w, h))
                        val reportW = w.coerceIn(constraints.minWidth, constraints.maxWidth)
                        val reportH = h.coerceIn(constraints.minHeight, constraints.maxHeight)
                        layout(reportW, reportH) {
                            placeable.place(
                                x = (sliderLeftPx - pressOutsetPx).roundToInt(),
                                y = (insetPx - pressOutsetPx).roundToInt()
                            )
                        }
                    }
                    .graphicsLayer {
                        val stretch = stretchOf(sliderLeftPx, anchorPx, itemWidthPx)
                        scaleX = 1f + SliderStretchMax * stretch
                    }
                    .liquidGlassSurface(
                        backdrop = lensBackdrop,
                        shape = RoundedCornerShape(50),
                        cornerRadius = lensCornerRadius,
                        blurRadius = if (draggingState.value) 1.5.dp else 0.8.dp,
                        refractionHeight = SliderRefractionHeight,
                        refractionAmount = SliderRefractionAmount,
                        chromaticAberration = SliderChromaticAberration,
                        refraction = true,
                        surfaceTint = if (useLensTint) {
                            MiuixTheme.colorScheme.primary.copy(alpha = if (isDark) 0.14f else 0.08f)
                        } else Color.Transparent,
                        grainAlpha = 0.04f,
                        fallbackColor = if (useLensTint) {
                            MiuixTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.16f)
                        } else {
                            if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.07f)
                        }
                    )
            )
        }

        // Layer 3: Unrefracted Badge pass above lens layer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight)
                .padding(horizontal = DockLensInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            slots.forEach { slot ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    if (slot.tabIndex == 0 && activeDeliveryCount > 0) {
                        Box(
                            modifier = Modifier.offset(
                                x = 13.dp,
                                y = (-12).dp
                            )
                        ) {
                            if (outForDeliveryCount > 0) {
                                Badge { Text(outForDeliveryCount.toString()) }
                            } else {
                                Badge(modifier = Modifier.size(7.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
