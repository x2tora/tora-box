package com.x2tora.torabox.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

object ToraBoxMotion {
    val FluidSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.74f,
        stiffness = 165f
    )

    val InteractiveSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.70f,
        stiffness = 210f
    )

    val SettleSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = 0.82f,
        stiffness = 185f
    )

    val OffsetSpring: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.75f,
        stiffness = 165f
    )

    val ExitOffsetSpring: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.84f,
        stiffness = 185f
    )

    val LayoutSizeSpring: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 0.76f,
        stiffness = 160f
    )

    val ExitLayoutSizeSpring: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = 0.84f,
        stiffness = 180f
    )

    val OdometerSpring: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.74f,
        stiffness = 200f
    )

    fun expandEnter(): EnterTransition =
        expandVertically(
            animationSpec = LayoutSizeSpring,
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = tween(durationMillis = 320))

    fun shrinkExit(): ExitTransition =
        shrinkVertically(
            animationSpec = ExitLayoutSizeSpring,
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(durationMillis = 240))

    fun tabTransition(
        forward: Boolean,
        slideDivisor: Int = 12
    ): ContentTransform {
        val enterOffset: (Int) -> Int = { width ->
            if (forward) width / slideDivisor else -width / slideDivisor
        }
        val exitOffset: (Int) -> Int = { width ->
            if (forward) -width / slideDivisor else width / slideDivisor
        }
        return (
            slideInHorizontally(animationSpec = OffsetSpring, initialOffsetX = enterOffset) +
                fadeIn(animationSpec = tween(durationMillis = 320)) +
                scaleIn(initialScale = 0.98f, animationSpec = FluidSpring)
            ) togetherWith (
            slideOutHorizontally(animationSpec = ExitOffsetSpring, targetOffsetX = exitOffset) +
                fadeOut(animationSpec = tween(durationMillis = 230)) +
                scaleOut(targetScale = 0.98f, animationSpec = SettleSpring)
            )
    }
}
