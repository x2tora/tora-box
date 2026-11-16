package com.x2tora.torabox.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x2tora.torabox.domain.KoreanCarrierCatalog
import com.x2tora.torabox.model.CarrierInfo
import com.x2tora.torabox.model.CarrierStageStep
import com.x2tora.torabox.model.ShipmentRecord
import com.x2tora.torabox.model.StandardDeliveryPhase
import com.x2tora.torabox.ui.theme.LocalIsDarkTheme
import com.x2tora.torabox.ui.theme.LocalLiquidGlassEnabled
import com.x2tora.torabox.ui.theme.ToraBoxIconCatalog
import com.x2tora.torabox.ui.theme.ToraBoxMotion
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Pin
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixGlassIcon(
    icon: ImageVector,
    contentDescription: String?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp
) {
    val resolvedIconSize = maxOf(iconSize * 1.42f, (size.value * 0.72f).dp)
    val containerSize = maxOf(resolvedIconSize, (size.value * 0.80f).dp)

    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accentColor,
            modifier = Modifier.size(resolvedIconSize)
        )
    }
}

@Composable
fun SquirclePillBadge(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    textStyle: TextStyle = MiuixTheme.textStyles.footnote1.copy(
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum"
    ),
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.94f else 1f,
        animationSpec = ToraBoxMotion.InteractiveSpring,
        label = "pillPressScale"
    )
    val base = modifier
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        }
        .squircleBackground(backgroundColor, cornerRadius = 10.dp)
        .squircleClip(10.dp)
    val finalModifier = if (onClick != null) {
        base.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
    } else base

    Box(
        modifier = finalModifier.padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                style = textStyle,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyDataBanner(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color = MiuixTheme.colorScheme.primary,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MiuixGlassIcon(
                icon = icon,
                contentDescription = null,
                accentColor = accentColor,
                size = 60.dp,
                iconSize = 28.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MiuixTheme.textStyles.title4.copy(fontWeight = FontWeight.Bold),
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    text = actionLabel,
                    onClick = onAction,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    text = secondaryActionLabel,
                    onClick = onSecondaryAction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ConfirmActionDialog(
    show: Boolean,
    title: String,
    summary: String,
    confirmText: String,
    isDestructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    OverlayDialog(
        show = show,
        title = title,
        summary = summary,
        onDismissRequest = onDismiss
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(text = "취소", onClick = onDismiss, modifier = Modifier.weight(1f))
            TextButton(
                text = confirmText,
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = if (isDestructive) {
                    ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = Color.White
                    )
                } else ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MiuixTopBarActionDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 8.dp)
            .width(1.dp)
            .height(18.dp)
            .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.14f))
    )
}

@Composable
fun MiuixFloatingScrollTopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    isScrolledOverride: Boolean? = null,
    backdrop: LayerBackdrop? = null,
    maxContentWidth: Dp = Dp.Unspecified,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    val density = LocalDensity.current
    val surfaceBg = MiuixTheme.colorScheme.surface
    SideEffect {
        if (scrollBehavior.state.heightOffsetLimit != 0f) {
            scrollBehavior.state.heightOffsetLimit = 0f
        }
        if (scrollBehavior.state.heightOffset != 0f) {
            scrollBehavior.state.heightOffset = 0f
        }
    }
    val isScrolled by remember(scrollBehavior, isScrolledOverride) {
        derivedStateOf {
            isScrolledOverride ?: (scrollBehavior.state.contentOffset < -12f ||
                scrollBehavior.state.overlappedFraction > 0.04f)
        }
    }

    var hasNav by remember { mutableStateOf(false) }
    var hasActions by remember { mutableStateOf(false) }
    var navPillVisible by remember { mutableStateOf(false) }
    var titlePillVisible by remember { mutableStateOf(false) }
    var actionsPillVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isScrolled, hasNav, hasActions) {
        val target = isScrolled
        val stepDelayMs = 85L
        if (target) {
            if (hasNav) {
                navPillVisible = true
                delay(stepDelayMs)
            } else {
                navPillVisible = false
            }
            titlePillVisible = true
            if (hasActions) {
                delay(stepDelayMs)
                actionsPillVisible = true
            } else {
                actionsPillVisible = false
            }
        } else {
            if (hasActions) {
                actionsPillVisible = false
                delay(stepDelayMs)
            } else {
                actionsPillVisible = false
            }
            titlePillVisible = false
            if (hasNav) {
                delay(stepDelayMs)
                navPillVisible = false
            } else {
                navPillVisible = false
            }
        }
    }

    val islandSpring = spring<Float>(dampingRatio = 0.74f, stiffness = 175f)
    val navProgress by animateFloatAsState(if (navPillVisible) 1f else 0f, islandSpring, label = "topNav")
    val titleProgress by animateFloatAsState(if (titlePillVisible) 1f else 0f, islandSpring, label = "topTitle")
    val actionsProgress by animateFloatAsState(if (actionsPillVisible) 1f else 0f, islandSpring, label = "topActions")
    val slideOffsetPx = with(density) { 48.dp.toPx() }
    val useShader = isRuntimeShaderSupported()
    val edgeOverlayColor = surfaceBg.copy(alpha = if (useShader) 0.48f else 0.90f)
    val topEdgeBlurMod = if (backdrop != null && useShader) {
        Modifier.progressiveTextureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = 6f,
            gradient = ProgressiveBlur.Top,
            colors = BlurDefaults.blurColors(
                blendColors = listOf(BlendColorEntry(edgeOverlayColor, BlurBlendMode.SrcOver))
            )
        )
    } else {
        Modifier.background(
            Brush.verticalGradient(
                0.0f to surfaceBg.copy(alpha = 0.94f),
                0.62f to surfaceBg.copy(alpha = 0.56f),
                1.0f to Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(topEdgeBlurMod)
        )

        Row(
            modifier = Modifier
                .then(if (maxContentWidth != Dp.Unspecified) Modifier.widthIn(max = maxContentWidth) else Modifier)
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingTopBarIsland(
                    progress = navProgress,
                    isDark = isDark,
                    backdrop = backdrop,
                    cornerRadius = 16.dp,
                    slideOffsetPx = slideOffsetPx,
                    onHasContentChange = { hasNav = it },
                    content = navigationIcon
                )
                FloatingTopBarIsland(
                    progress = titleProgress,
                    isDark = isDark,
                    backdrop = backdrop,
                    cornerRadius = 16.dp,
                    slideOffsetPx = slideOffsetPx,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    AnimatedContent(
                        targetState = title,
                        transitionSpec = {
                            (slideInVertically(animationSpec = ToraBoxMotion.OffsetSpring) { it / 6 } +
                                fadeIn(tween(280)))
                                .togetherWith(
                                    slideOutVertically(animationSpec = ToraBoxMotion.ExitOffsetSpring) { -it / 6 } +
                                        fadeOut(tween(190))
                                )
                        },
                        label = "topBarTitleFade"
                    ) { animTitle ->
                        Text(
                            text = animTitle,
                            style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.SemiBold),
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            FloatingTopBarIsland(
                progress = actionsProgress,
                isDark = isDark,
                backdrop = backdrop,
                cornerRadius = 16.dp,
                slideOffsetPx = slideOffsetPx,
                onHasContentChange = { hasActions = it }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

@Composable
private fun FloatingTopBarIsland(
    progress: Float,
    isDark: Boolean,
    backdrop: LayerBackdrop?,
    cornerRadius: Dp,
    slideOffsetPx: Float,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onHasContentChange: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val p = progress.coerceIn(0f, 1f)
    val density = LocalDensity.current
    val isLiquidGlass = LocalLiquidGlassEnabled.current
    val minSizePx = with(density) { 48.dp.roundToPx() }
    val pillShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val pillFill = if (isDark) {
        MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
    } else {
        Color.White.copy(alpha = 0.94f)
    }
    val pillBorder = MiuixTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.14f else 0.08f)
    val useRealLiquidGlass = isLiquidGlass &&
        backdrop != null &&
        isRuntimeShaderSupported() &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    Layout(
        modifier = modifier.onSizeChanged { size ->
            onHasContentChange(size.width > 0 && size.height > 0)
        },
        content = {
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
            if (p > 0.005f) {
                val bgSurfaceMod = if (useRealLiquidGlass) {
                    Modifier.liquidGlassSurface(
                        backdrop = backdrop,
                        shape = pillShape,
                        cornerRadius = cornerRadius,
                        blurRadius = 2.5.dp,
                        refractionHeight = 8.dp,
                        refractionAmount = 5.dp,
                        chromaticAberration = 2.0.dp,
                        surfaceTint = if (isDark) {
                            Color(0xFF141822).copy(alpha = 0.40f)
                        } else {
                            Color(0xFFF6F7FB).copy(alpha = 0.46f)
                        },
                        grainAlpha = 0.06f,
                        fallbackColor = if (isDark) MiuixTheme.colorScheme.surfaceContainerHigh else Color.White
                    )
                } else {
                    val blurMod = if (backdrop != null && isRuntimeShaderSupported()) {
                        Modifier.textureBlur(
                            backdrop = backdrop,
                            shape = pillShape,
                            blurRadius = 10f,
                            colors = BlurDefaults.blurColors(
                                blendColors = listOf(BlendColorEntry(pillFill, BlurBlendMode.SrcOver))
                            )
                        )
                    } else Modifier
                    Modifier
                        .clip(pillShape)
                        .then(blurMod)
                        .background(pillFill, pillShape)
                        .border(1.dp, pillBorder, pillShape)
                }
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = (1f - p) * -slideOffsetPx
                            alpha = p
                            val scale = 0.92f + 0.08f * p
                            scaleX = scale
                            scaleY = scale
                        }
                        .shadow(
                            elevation = (8f * p).dp,
                            shape = pillShape,
                            ambientColor = Color.Black.copy(alpha = if (isDark) 0.40f else 0.12f),
                            spotColor = Color.Black.copy(alpha = if (isDark) 0.40f else 0.14f)
                        )
                        .then(bgSurfaceMod)
                        .pointerInput(Unit) { detectTapGestures { } }
                )
            }
        }
    ) { measurables, constraints ->
        val contentPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        if (contentPlaceable.width == 0 || contentPlaceable.height == 0) {
            return@Layout layout(0, 0) {}
        }
        val pillW = contentPlaceable.width.coerceAtLeast(minSizePx).coerceAtMost(constraints.maxWidth)
        val pillH = contentPlaceable.height.coerceAtLeast(minSizePx).coerceAtMost(constraints.maxHeight)
        val bgPlaceable = if (measurables.size > 1) {
            measurables[1].measure(Constraints.fixed(pillW, pillH))
        } else null
        layout(pillW, pillH) {
            bgPlaceable?.placeRelative(0, 0)
            contentPlaceable.placeRelative(
                (pillW - contentPlaceable.width) / 2,
                (pillH - contentPlaceable.height) / 2
            )
        }
    }
}

@Composable
fun MiuixBottomEdgeBlur(
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    extraHeight: Dp = 52.dp
) {
    val surfaceBg = MiuixTheme.colorScheme.surface
    val useShader = isRuntimeShaderSupported()
    val edgeOverlayColor = surfaceBg.copy(alpha = if (useShader) 0.48f else 0.90f)
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val blurMod = if (useShader) {
        Modifier.progressiveTextureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = 6f,
            gradient = ProgressiveBlur.Bottom,
            colors = BlurDefaults.blurColors(
                blendColors = listOf(BlendColorEntry(edgeOverlayColor, BlurBlendMode.SrcOver))
            )
        )
    } else {
        Modifier.background(
            Brush.verticalGradient(
                0.0f to Color.Transparent,
                0.38f to surfaceBg.copy(alpha = 0.56f),
                1.0f to surfaceBg.copy(alpha = 0.94f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(navBottomInset + extraHeight)
            .then(blurMod)
    )
}

@Composable
fun phaseSemanticColor(phase: StandardDeliveryPhase): Color = when (phase) {
    StandardDeliveryPhase.PREPARING -> Color(0xFF64748B)
    StandardDeliveryPhase.PICKED_UP -> Color(0xFF0EA5E9)
    StandardDeliveryPhase.IN_TRANSIT -> MiuixTheme.colorScheme.primary
    StandardDeliveryPhase.ARRIVED_AT_BRANCH -> Color(0xFF8B5CF6)
    StandardDeliveryPhase.OUT_FOR_DELIVERY -> Color(0xFFF97316)
    StandardDeliveryPhase.DELIVERED -> Color(0xFF16A34A)
    StandardDeliveryPhase.EXCEPTION -> MiuixTheme.colorScheme.error
}

@Composable
fun CarrierStageStepper(
    stages: List<CarrierStageStep>,
    currentStageIndex: Int,
    currentPhase: StandardDeliveryPhase,
    accentColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (stages.isEmpty()) return
    val clampedIndex = currentStageIndex.coerceIn(0, stages.lastIndex)
    val isException = currentPhase == StandardDeliveryPhase.EXCEPTION
    val activeColor = if (isException) MiuixTheme.colorScheme.error else accentColor
    val trackBg = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stages.forEachIndexed { idx, step ->
                val isCompleted = idx < clampedIndex
                val isCurrent = idx == clampedIndex
                val dotColor by animateColorAsState(
                    targetValue = when {
                        isCurrent -> activeColor
                        isCompleted -> activeColor.copy(alpha = 0.78f)
                        else -> MiuixTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                    },
                    animationSpec = tween(260),
                    label = "stageDot_$idx"
                )

                Box(
                    modifier = Modifier
                        .size(if (isCurrent) (if (compact) 11.dp else 14.dp) else (if (compact) 7.dp else 9.dp))
                        .clip(CircleShape)
                        .background(dotColor)
                        .then(
                            if (isCurrent && !compact) {
                                Modifier.border(2.dp, activeColor.copy(alpha = 0.30f), CircleShape)
                            } else Modifier
                        )
                )

                if (idx < stages.lastIndex) {
                    val segColor by animateColorAsState(
                        targetValue = if (idx < clampedIndex) activeColor.copy(alpha = 0.65f) else trackBg,
                        animationSpec = tween(260),
                        label = "stageSeg_$idx"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 2.5.dp else 3.dp)
                            .padding(horizontal = 2.dp)
                            .clip(CircleShape)
                            .background(segColor)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stages.forEachIndexed { idx, step ->
                val isCurrent = idx == clampedIndex
                val isCompleted = idx < clampedIndex
                Text(
                    text = step.stageName,
                    style = MiuixTheme.textStyles.footnote2.copy(
                        fontSize = if (compact) 10.sp else 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        fontFeatureSettings = "tnum"
                    ),
                    color = when {
                        isCurrent -> activeColor
                        isCompleted -> MiuixTheme.colorScheme.onSurface.copy(alpha = 0.76f)
                        else -> MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.72f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = when (idx) {
                        0 -> TextAlign.Start
                        stages.lastIndex -> TextAlign.End
                        else -> TextAlign.Center
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ShipmentSummaryCard(
    shipment: ShipmentRecord,
    isRefreshing: Boolean,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val carrier: CarrierInfo? = KoreanCarrierCatalog.findById(shipment.carrierId)
    val carrierShort = carrier?.shortName ?: shipment.carrierId
    val semanticColor = phaseSemanticColor(shipment.currentPhase)
    val accentColor = Color(shipment.colorHex)
    val latestEvent = shipment.events.firstOrNull()
    val pipeline = carrier?.stagePipeline.orEmpty()
    val titleText = shipment.displayTitle(carrierShort)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiuixGlassIcon(
                    icon = ToraBoxIconCatalog.resolveIcon(shipment.iconKey),
                    contentDescription = titleText,
                    accentColor = accentColor,
                    size = 42.dp,
                    iconSize = 21.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (shipment.isPinned) {
                            Icon(
                                imageVector = MiuixIcons.Pin,
                                contentDescription = "고정됨",
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = titleText,
                            style = MiuixTheme.textStyles.headline2.copy(fontWeight = FontWeight.Bold),
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val subLine = buildString {
                        append(carrierShort)
                        append(" · ")
                        append(shipment.waybillNumber)
                        if (shipment.senderName.isNotBlank()) {
                            append(" · ")
                            append(shipment.senderName)
                        }
                    }
                    Text(
                        text = subLine,
                        style = MiuixTheme.textStyles.footnote2.copy(fontFeatureSettings = "tnum"),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                SquirclePillBadge(
                    text = shipment.currentStatusLabel.ifBlank { shipment.currentPhase.label },
                    backgroundColor = semanticColor.copy(alpha = 0.14f),
                    contentColor = semanticColor
                )

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = MiuixIcons.Refresh,
                        contentDescription = "새로고침",
                        tint = if (isRefreshing) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (pipeline.isNotEmpty()) {
                CarrierStageStepper(
                    stages = pipeline,
                    currentStageIndex = shipment.currentCarrierStageIndex,
                    currentPhase = shipment.currentPhase,
                    accentColor = semanticColor,
                    compact = true
                )
            }

            if (latestEvent != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val eventSummary = listOf(latestEvent.location, latestEvent.description)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                        .ifBlank { latestEvent.statusName }
                    Text(
                        text = eventSummary,
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (latestEvent.displayTime.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = latestEvent.displayTime,
                            style = MiuixTheme.textStyles.footnote2.copy(fontFeatureSettings = "tnum"),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
