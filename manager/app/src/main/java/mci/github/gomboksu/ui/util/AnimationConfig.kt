package mci.github.gomboksu.ui.util

import android.content.SharedPreferences
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

data class AnimationConfig(
    val enabled: Boolean = true,
    val bounceMultiplier: Float = 1.0f, // 0.0f (no bounce) .. 2.5f (jelly)
    val speedMultiplier: Float = 1.0f,  // 0.4f (slow-mo) .. 2.5f (hyper)
    val pageTransitionStyle: Int = 1,   // 0=Slide, 1=Bouncy Zoom, 2=Depth Push, 3=Vertical Rise, 4=Elastic Pop
    val splashStyle: Int = 1,           // 0=Classic Spin, 1=Elastic Pop, 2=Pulse Wave, 3=3D Flip
    val cardPressBounce: Boolean = true,
    val staggeredEntrance: Boolean = true,
    val statusPulseGlow: Boolean = true,
    val navBarIconBounce: Boolean = true
) {
    val dampingRatio: Float
        get() {
            if (!enabled || bounceMultiplier <= 0.02f) return Spring.DampingRatioNoBouncy
            // Maps bounceMultiplier 0..2.5 -> dampingRatio 0.92..0.22
            val clamped = bounceMultiplier.coerceIn(0f, 2.5f)
            return (0.92f - (clamped * 0.28f)).coerceIn(0.22f, 1.0f)
        }

    val stiffness: Float
        get() {
            if (!enabled) return Spring.StiffnessHigh
            val speed = speedMultiplier.coerceIn(0.4f, 2.5f)
            return (380f * speed).coerceIn(120f, 1400f)
        }

    fun scaledDuration(baseMs: Int): Int {
        if (!enabled) return 1
        val speed = speedMultiplier.coerceIn(0.4f, 2.5f)
        return (baseMs / speed).toInt().coerceIn(80, 1200)
    }

    fun <T> springSpec(stiffnessScale: Float = 1f): SpringSpec<T> {
        return spring(
            dampingRatio = dampingRatio,
            stiffness = (stiffness * stiffnessScale).coerceIn(100f, 1800f)
        )
    }

    companion object {
        fun fromPrefs(prefs: SharedPreferences): AnimationConfig {
            return AnimationConfig(
                enabled = prefs.getBoolean("anim_enabled", true),
                bounceMultiplier = prefs.getFloat("anim_bounce_multiplier", 1.0f).coerceIn(0f, 2.5f),
                speedMultiplier = prefs.getFloat("anim_speed_multiplier", 1.0f).coerceIn(0.4f, 2.5f),
                pageTransitionStyle = prefs.getInt("anim_page_transition", 1).coerceIn(0, 4),
                splashStyle = prefs.getInt("anim_splash_style", 1).coerceIn(0, 3),
                cardPressBounce = prefs.getBoolean("anim_card_press_bounce", true),
                staggeredEntrance = prefs.getBoolean("anim_staggered_entrance", true),
                statusPulseGlow = prefs.getBoolean("anim_status_pulse_glow", true),
                navBarIconBounce = prefs.getBoolean("anim_navbar_icon_bounce", true)
            )
        }
    }
}

val LocalAnimationConfig = compositionLocalOf { AnimationConfig() }

/**
 * Tactile spring squish & bounce effect on press, respecting the user's Bounce Multiplier & Speed.
 */
fun Modifier.gombokPressBounce(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f
): Modifier = composed {
    val anim = LocalAnimationConfig.current
    if (!enabled || !anim.enabled || !anim.cardPressBounce) return@composed this

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = anim.springSpec(stiffnessScale = 1.2f),
        label = "gombokPressBounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled, anim.enabled, anim.cardPressBounce) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}

/**
 * Staggered cascade entrance animation for cards & sections.
 */
@Composable
fun GombokAnimatedEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val anim = LocalAnimationConfig.current
    if (!anim.enabled || !anim.staggeredEntrance) {
        Box(modifier = modifier) { content() }
        return
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val delayMs = (index * anim.scaledDuration(45)).coerceAtMost(450).toLong()
        if (delayMs > 0L) delay(delayMs)
        visible = true
    }

    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = anim.springSpec(stiffnessScale = 0.95f),
        label = "gombokEntrance_$index"
    )

    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.coerceIn(0f, 1f)
            translationY = (1f - progress) * 42f
            val s = 0.92f + (0.08f * progress)
            scaleX = s
            scaleY = s
        }
    ) {
        content()
    }
}
