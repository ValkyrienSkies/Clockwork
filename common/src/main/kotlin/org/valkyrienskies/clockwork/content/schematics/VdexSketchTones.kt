package org.valkyrienskies.clockwork.content.schematics

import org.joml.Vector2f
import java.nio.ByteBuffer
import kotlin.math.max

internal object VdexSketchTones {
    fun measure(pixels: ByteBuffer): Vector2f {
        val histogram = IntArray(256)
        var count = 0
        for (i in 3 until pixels.limit() step 4) {
            val alpha = pixels.get(i).toInt() and 255
            if (alpha < 51) continue
            histogram[alpha]++
            count++
        }
        if (count < 16) return Vector2f(0f, 1f)
        fun percentile(fraction: Float): Float {
            val threshold = max(1, (count * fraction).toInt())
            var sum = 0
            for (i in 51..255) {
                sum += histogram[i]
                if (sum >= threshold) return (i - 51) / 204f
            }
            return 1f
        }
        val low = percentile(.04f)
        val high = percentile(.96f)
        val span = max(.25f, high - low + .08f).coerceAtMost(1f)
        val start = ((low + high - span) * .5f).coerceIn(0f, 1f - span)
        return Vector2f(start, start + span)
    }
}
