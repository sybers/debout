package fr.sybers.debout.ui.theme

import android.animation.ValueAnimator
import android.graphics.RuntimeShader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.InfiniteAnimationPolicy

/** Light and dark specks over a flat color, like film grain. Premultiplied output, no blend mode needed. */
private const val GRAIN_SHADER = """
    uniform float frame;
    uniform float intensity;

    float hash(float2 p) {
        float3 q = fract(float3(p.xyx) * 0.1031);
        q += dot(q, q.yzx + 33.33);
        return fract((q.x + q.y) * q.z);
    }

    half4 main(float2 coord) {
        float2 cell = mod(floor(coord / 1.5), 1024.0) + mod(frame, 64.0) * float2(37.0, 91.0);
        float n = hash(cell);
        float a = abs(n - 0.5) * 2.0 * intensity;
        return n > 0.5 ? half4(a, a, a, a) : half4(0.0, 0.0, 0.0, a);
    }
"""

/** Frames per second of the grain: film-like, and cheap since the shader redraws only on a new frame. */
private const val GRAIN_FPS = 8

/** Fills its bounds with moving grain. Place it above flat backgrounds and below text. */
@Composable
fun Grain(modifier: Modifier = Modifier, intensity: Float = 0.12f) {
    val shader = remember { RuntimeShader(GRAIN_SHADER) }
    val brush = remember(shader) { ShaderBrush(shader) }
    var frame by remember { mutableIntStateOf(0) }
    val motion = remember { ValueAnimator.areAnimatorsEnabled() }

    LaunchedEffect(motion) {
        if (!motion) return@LaunchedEffect
        val run: suspend () -> Unit = {
            while (true) withFrameMillis { frame = (it * GRAIN_FPS / 1_000).toInt() }
        }
        // Never ends: declaring it lets UI tests skip it instead of waiting forever.
        val policy = coroutineContext[InfiniteAnimationPolicy]
        if (policy == null) run() else policy.onInfiniteOperation(run)
    }

    Canvas(modifier) {
        shader.setFloatUniform("frame", (frame % 1_000).toFloat())
        shader.setFloatUniform("intensity", intensity)
        drawRect(brush)
    }
}
