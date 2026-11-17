package org.valkyrienskies.clockwork.client.render.goggles

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkStatus
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3dc
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

internal class AeronautSupportRim {
    private class Contour(val points: List<Vector3dc>) {
        val lengths = DoubleArray(points.size + 1)
        val length: Double

        init {
            for (i in points.indices) lengths[i + 1] = lengths[i] + points[i].distance(points[(i + 1) % points.size])
            length = lengths.last()
        }

        fun pointAt(phase: Double): Vector3dc {
            val distance = wrap(phase) * length
            val edge = points.indices.firstOrNull { distance < lengths[it + 1] } ?: return points.first()
            val fraction = (distance - lengths[edge]) / (lengths[edge + 1] - lengths[edge])
            return Vector3d(points[edge]).lerp(points[(edge + 1) % points.size], fraction)
        }

        fun phaseNear(point: Vector3dc): Double {
            var bestDistance = Double.POSITIVE_INFINITY
            var bestPhase = 0.0
            for (i in points.indices) {
                val from = points[i]
                val direction = points[(i + 1) % points.size].sub(from, Vector3d())
                val lengthSquared = direction.lengthSquared()
                if (lengthSquared <= 1e-12) continue
                val fraction = (point.sub(from, Vector3d()).dot(direction) / lengthSquared).coerceIn(0.0, 1.0)
                val closest = Vector3d(from).fma(fraction, direction)
                val distance = closest.distanceSquared(point)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestPhase = (lengths[i] + fraction * (lengths[i + 1] - lengths[i])) / length
                }
            }
            return wrap(bestPhase)
        }
    }

    private class Transition(val from: List<Vector3dc>, val to: List<Vector3dc>, val started: Long) {
        fun contourAt(now: Long): Contour {
            val t = ((now - started) / LERP_NANOS).coerceIn(0.0, 1.0)
            val eased = t * t * (3.0 - 2.0 * t)
            return Contour(from.indices.map { Vector3d(from[it]).lerp(to[it], eased) })
        }
    }

    private var contour: Contour? = null
    private var transition: Transition? = null
    private var phase = 0.0
    private var phaseAt = 0L
    private var appearedAt = 0L

    fun update(points: List<Vector3dc>, now: Long, clearSweep: (List<Vector3dc>, List<Vector3dc>) -> Boolean) {
        if (points.size < 2) {
            clear()
            return
        }
        val previous = contour
        val active = transition
        if (points == previous?.points) {
            if (active != null && (now - active.started >= LERP_NANOS || !clearSweep(active.from, active.to))) transition = null
            return
        }
        val next = Contour(points)
        if (next.length <= 1e-6) {
            clear()
            return
        }
        val displayed = contourAt(now)
        val head = displayed?.pointAt(phaseOn(displayed, now))
        transition = displayed?.let { transition(it, next, now, clearSweep) }
        phase = head?.let(next::phaseNear) ?: 0.0
        phaseAt = now
        if (previous == null) appearedAt = now
        contour = next
    }

    fun clear() {
        contour = null
        transition = null
    }

    private fun transition(from: Contour, to: Contour, now: Long,
                           clearSweep: (List<Vector3dc>, List<Vector3dc>) -> Boolean): Transition? {
        if (from.points.size < 3 || to.points.size < 3) return null
        val offset = to.phaseNear(from.points.first())
        val corners = from.points.indices.map { from.lengths[it] / from.length } +
            to.points.indices.map { wrap(to.lengths[it] / to.length - offset) }
        val phases = corners.sorted().fold(ArrayList<Double>()) { positions, corner ->
            if (positions.isEmpty() || corner - positions.last() > 1e-7) positions.add(corner)
            positions
        }
        if (phases.size > MAX_TRANSITION_VERTICES) return null
        val start = phases.map(from::pointAt)
        val end = phases.map { to.pointAt(it + offset) }
        val movement = start.indices.maxOf { start[it].distanceSquared(end[it]) }
        if (movement !in 1e-8..MAX_VERTEX_TRAVEL_SQUARED || !clearSweep(start, end)) return null
        return Transition(start, end, now)
    }

    internal fun outlineAt(now: Long): List<Vector3dc> = contourAt(now)?.points.orEmpty()

    internal fun headPosition(now: Long): Vector3dc? = contourAt(now)?.let { it.pointAt(phaseOn(it, now)) }

    private fun contourAt(now: Long): Contour? = transition?.takeIf { now - it.started < LERP_NANOS }?.contourAt(now) ?: contour

    private fun phaseOn(rim: Contour, now: Long): Double {
        val phaseNow = currentPhase(now)
        val latest = contour ?: return phaseNow
        return if (rim === latest) phaseNow else rim.phaseNear(latest.pointAt(phaseNow))
    }

    private fun currentPhase(now: Long) = wrap(phase + (now - phaseAt).toDouble() / ORBIT_NANOS)

    fun render(graphics: GuiGraphics, now: Long, project: (Vector3dc) -> AeronautHudMath.Point?) {
        val rim = contourAt(now) ?: return
        val points = rim.points.map(project)
        val fade = ((now - appearedAt).toDouble() / FADE_NANOS).coerceIn(0.0, 1.0).toFloat()
        val head = phaseOn(rim, now)
        val type = RenderType.gui()
        val buffer = graphics.bufferSource()
        val vertices = buffer.getBuffer(type)
        val pose = graphics.pose().last().pose()

        for (i in points.indices) {
            if (i == 1 && points.size == 2) break
            val from = points[i] ?: continue
            val to = points[(i + 1) % points.size] ?: continue
            stroke(vertices, pose, from, to, 3f, 3f, opacity(0x2A398675, fade), opacity(0x2A398675, fade))
            stroke(vertices, pose, from, to, 0.9f, 0.9f, opacity(0xB06BC6A4.toInt(), fade), opacity(0xB06BC6A4.toInt(), fade))
        }
        for (i in points.indices) {
            val from = points[i] ?: continue
            val to = points[(i + 1) % points.size] ?: continue
            val start = rim.lengths[i] / rim.length
            val end = rim.lengths[i + 1] / rim.length
            if (end - start <= 1e-9) continue
            for (side in 0..1) {
                val pulse = wrap(head + side * 0.5)
                drawPulse(vertices, pose, from, to, start, end, pulse, fade)
                if (pulse < TRAIL) drawPulse(vertices, pose, from, to, start, end, pulse + 1.0, fade)
            }
        }
        buffer.endBatch(type)
    }

    private fun drawPulse(vertices: VertexConsumer, pose: Matrix4f, from: AeronautHudMath.Point, to: AeronautHudMath.Point,
                          start: Double, end: Double, head: Double, fade: Float) {
        val tail = head - TRAIL
        for (i in 0 until profile.lastIndex) {
            val first = maxOf(start, tail + TRAIL * profile[i])
            val last = minOf(end, tail + TRAIL * profile[i + 1])
            if (last <= first) continue
            val a = mix(from, to, ((first - start) / (end - start)).toFloat())
            val b = mix(from, to, ((last - start) / (end - start)).toFloat())
            val aEnergy = energy(i, ((first - tail) / TRAIL).toFloat()) * fade
            val bEnergy = energy(i, ((last - tail) / TRAIL).toFloat()) * fade
            stroke(vertices, pose, a, b, 3f + aEnergy * 2.8f, 3f + bEnergy * 2.8f,
                opacity(0x705CEAC6, aEnergy), opacity(0x705CEAC6, bEnergy))
            stroke(vertices, pose, a, b, 1f + aEnergy * 1.4f, 1f + bEnergy * 1.4f,
                pulseColor(aEnergy), pulseColor(bEnergy))
        }
    }

    private fun stroke(vertices: VertexConsumer, pose: Matrix4f, from: AeronautHudMath.Point, to: AeronautHudMath.Point,
                       headWidth: Float, tailWidth: Float, fromColor: Int, toColor: Int) {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = hypot(dx, dy)
        if (length < 0.01f) return
        val nx = -dy * 0.5f / length
        val ny = dx * 0.5f / length
        fun vertex(point: AeronautHudMath.Point, width: Float, color: Int) {
            vertices.vertex(pose, point.x + nx * width, point.y + ny * width, 0f)
                .color(color shr 16 and 255, color shr 8 and 255, color and 255, color ushr 24).endVertex()
        }
        vertex(from, headWidth, fromColor)
        vertex(to, tailWidth, toColor)
        vertex(to, -tailWidth, toColor)
        vertex(from, -headWidth, fromColor)
    }

    companion object {
        private const val MAX_TRANSITION_VERTICES = 48
        private const val MAX_VERTEX_TRAVEL_SQUARED = 2.25
        private const val MAX_SWEEP_BLOCKS = 2048L
        private const val SWEEP_MARGIN = 0.0001
        private const val LERP_NANOS = 200_000_000.0
        const val ORBIT_NANOS = 3_600_000_000.0
        const val FADE_NANOS = 160_000_000.0
        const val TRAIL = 0.31
        val profile = floatArrayOf(0f, 0.36f, 0.73f, 0.93f, 1f)
        val intensity = floatArrayOf(0f, 0.22f, 0.62f, 1f, 0f)

        fun terrainClear(world: ClientLevel, eye: Vec3, from: List<Vector3dc>, to: List<Vector3dc>): Boolean {
            val points = from + to
            val bounds = AABB(minOf(eye.x, points.minOf { it.x() }), minOf(eye.y, points.minOf { it.y() }),
                minOf(eye.z, points.minOf { it.z() }), maxOf(eye.x, points.maxOf { it.x() }),
                maxOf(eye.y, points.maxOf { it.y() }), maxOf(eye.z, points.maxOf { it.z() }))
            val minimum = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ).below()
            val maximum = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)
            val width = maximum.x - minimum.x + 1L
            val height = maximum.y - minimum.y + 1L
            val depth = maximum.z - minimum.z + 1L
            if (maxOf(width, height, depth) > MAX_SWEEP_BLOCKS || width * height * depth > MAX_SWEEP_BLOCKS) return false
            val chunks = HashMap<Long, ChunkAccess?>()
            for (pos in BlockPos.betweenClosed(minimum, maximum)) {
                if (world.isOutsideBuildHeight(pos)) continue
                val x = pos.x shr 4
                val z = pos.z shr 4
                val chunk = chunks.getOrPut(ChunkPos.asLong(x, z)) { world.chunkSource.getChunk(x, z, ChunkStatus.FULL, false) }
                    ?: return false
                val state = chunk.getBlockState(pos)
                if (state.isAir) continue
                var blocked = false
                state.getCollisionShape(world, pos).forAllBoxes { x0, y0, z0, x1, y1, z1 ->
                    if (bounds.intersects(pos.x + x0, pos.y + y0, pos.z + z0,
                            pos.x + x1, pos.y + y1 - SWEEP_MARGIN, pos.z + z1)) blocked = true
                }
                if (blocked) return false
            }
            return true
        }

        fun wrap(value: Double) = value - floor(value)

        fun mix(a: AeronautHudMath.Point, b: AeronautHudMath.Point, t: Float) =
            AeronautHudMath.Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

        fun energy(index: Int, t: Float): Float {
            val f = ((t - profile[index]) / (profile[index + 1] - profile[index])).coerceIn(0f, 1f)
            return intensity[index] + (intensity[index + 1] - intensity[index]) * f
        }

        fun opacity(color: Int, fade: Float) = (color and 0xFFFFFF) or (((color ushr 24) * fade).roundToInt() shl 24)

        fun pulseColor(energy: Float): Int {
            val red = (101 + 142 * energy).roundToInt()
            val green = (220 + 35 * energy).roundToInt()
            val blue = (186 + 65 * energy).roundToInt()
            return (opacity(0xFF000000.toInt(), energy) or (red shl 16) or (green shl 8) or blue)
        }
    }
}
