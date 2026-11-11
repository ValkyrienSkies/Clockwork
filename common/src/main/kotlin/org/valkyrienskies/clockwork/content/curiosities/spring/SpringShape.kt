package org.valkyrienskies.clockwork.content.curiosities.spring

import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

internal object SpringShape {
    const val WIDTH = 12.0 / 16.0
    const val REPEAT_LENGTH = 12.0 / 16.0

    fun points(start: Vec3, end: Vec3, normalA: Vec3, normalB: Vec3): List<Vec3> {
        val distance = start.distanceTo(end)
        val stub = minOf(3.0 / 16.0, distance * 0.2)
        val a = start.add(normalA.scale(stub))
        val b = end.add(normalB.scale(stub))
        val handle = minOf(distance * 0.3, 1.5)
        val controlA = a.add(normalA.scale(handle))
        val controlB = b.add(normalB.scale(handle))
        val count = ceil(distance * 8).toInt().coerceIn(12, 256)
        return listOf(start) + (0..count).map { i ->
            val t = i.toDouble() / count
            val s = 1 - t
            a.scale(s*s*s).add(controlA.scale(3*s*s*t)).add(controlB.scale(3*s*t*t)).add(b.scale(t*t*t))
        } + end
    }

    fun recoilReach(progress: Double): Double {
        val p = progress.coerceIn(0.0, 1.0)
        val remaining = 1 - p
        return 0.5 * (remaining * remaining * remaining +
            0.08 * sin(p * PI * 4) * remaining * remaining).coerceIn(0.0, 1.0)
    }
}
