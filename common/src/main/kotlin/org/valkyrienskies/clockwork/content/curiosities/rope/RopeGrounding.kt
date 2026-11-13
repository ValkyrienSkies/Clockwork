package org.valkyrienskies.clockwork.content.curiosities.rope

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeCurve
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Client-only support for the visual strand; never changes the rope's constraint length. */
internal class RopeGrounding {
    private var sampledCurve: WandRopeCurve? = null
    private var sampledTick = Long.MIN_VALUE
    private var refreshTick = Long.MIN_VALUE
    private val phase = nextPhase++ % REFRESH_TICKS
    private var floors = DoubleArray(0)
    private var renderedCurve: WandRopeCurve? = null
    private var renderedPoints = emptyList<Vec3>()

    fun points(level: ClientLevel, curve: WandRopeCurve): List<Vec3> {
        val tick = level.gameTime
        if (sampledTick != tick && (!sameCurve(sampledCurve, curve) || tick >= refreshTick)) {
            sampleGround(level, curve)
            sampledCurve = curve
            sampledTick = tick
            // Spread stationary ropes over five ticks. Moving ropes refresh at most once per tick.
            refreshTick = tick + 1 + Math.floorMod(phase - tick - 1, REFRESH_TICKS.toLong())
            renderedCurve = null
        }
        if (!sameCurve(renderedCurve, curve)) {
            val count = floors.lastIndex
            renderedPoints = (0..count).map { i ->
                val point = curve.point(i.toDouble() / count)
                if (i == 0 || i == count || point.y >= floors[i]) point
                else Vec3(point.x, floors[i], point.z)
            }
            renderedCurve = curve
        }
        return renderedPoints
    }

    private fun sampleGround(level: ClientLevel, curve: WandRopeCurve) {
        val count = ceil(curve.a.distanceTo(curve.b) * 0.5 + curve.sag * 12).toInt().coerceIn(4, 128)
        if (floors.size != count + 1) floors = DoubleArray(count + 1)
        floors.fill(Double.NEGATIVE_INFINITY)
        var start = curve.a
        var straightStart = curve.a
        for (i in 0 until count) {
            val t = (i + 1).toDouble() / count
            val end = curve.point(t)
            val straightEnd = curve.a.lerp(curve.b, t)
            // Search only the short column between the sagging segment and its taut position.
            // Sweeping the whole segment also catches ledges between rendered vertices.
            val ceiling = min(straightStart.y, straightEnd.y) + 0.001
            val bounds = AABB(
                min(start.x, end.x) - HORIZONTAL_CLEARANCE, min(start.y, end.y) - CLEARANCE,
                min(start.z, end.z) - HORIZONTAL_CLEARANCE,
                max(start.x, end.x) + HORIZONTAL_CLEARANCE, max(straightStart.y, straightEnd.y) + CLEARANCE,
                max(start.z, end.z) + HORIZONTAL_CLEARANCE
            )
            var floor = Double.NEGATIVE_INFINITY
            for (shape in level.getBlockCollisions(null, bounds)) {
                shape.forAllBoxes { minX, _, minZ, maxX, maxY, maxZ ->
                    // Ignore ceilings and anchor block sides above the unsagged strand.
                    // Inspect individual boxes so stairs and slabs retain their actual surface height.
                    if (maxY <= ceiling && maxX > bounds.minX && minX < bounds.maxX &&
                        maxZ > bounds.minZ && minZ < bounds.maxZ) {
                        floor = max(floor, maxY + CLEARANCE)
                    }
                }
            }
            floors[i] = max(floors[i], floor)
            floors[i + 1] = max(floors[i + 1], floor)
            start = end
            straightStart = straightEnd
        }
    }

    private fun sameCurve(previous: WandRopeCurve?, curve: WandRopeCurve) =
        previous != null && previous.a == curve.a && previous.b == curve.b && previous.sag == curve.sag

    companion object {
        const val RADIUS = 0.0625
        private const val CLEARANCE = RADIUS + 0.002
        // A tilted square strand can extend sqrt(2) radii horizontally.
        private const val HORIZONTAL_CLEARANCE = RADIUS * 1.4142135623730951 + 0.002
        private const val REFRESH_TICKS = 5
        private var nextPhase = 0
    }
}
