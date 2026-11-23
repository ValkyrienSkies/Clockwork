package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.joml.Intersectiond
import org.joml.Matrix4d
import org.joml.Matrix4dc
import org.joml.Vector3d
import org.joml.primitives.AABBd
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.core.api.bodies.ServerVsBody
import org.valkyrienskies.mod.common.getShipsIntersecting
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

internal class TireHazards {
    private var previousBody = -1L
    private var previousTick = -1L
    private val previousCenter = Vector3d()
    private var touching = HashSet<Long>()

    fun check(level: ServerLevel, body: ServerVsBody, tire: WheelTire, puncture: (Double, Vector3d) -> Unit) {
        val center = body.kinematics.position
        val scale = body.kinematics.scaling.x()
        val distance = previousCenter.distance(center)
        if (previousBody == body.id && distance < .01 && (level.gameTime + body.id) % 5 != 0L) return
        val sweep = previousBody == body.id && distance < 8 * scale
        if (body.id != previousBody) touching.clear()
        val pose = body.kinematics.toWorld
        val travel = if (sweep) Vector3d(previousCenter).sub(center) else Vector3d()
        val occupied = AABBd(-tire.width / 2, -tire.radius, -tire.radius, tire.width / 2, tire.radius, tire.radius)
        val worldBounds = AABBd(occupied).transform(pose).toMinecraft().expandTowards(travel.x, travel.y, travel.z)
        val overlaps = HashSet<Long>()
        fun scan(toRegion: Matrix4dc, movement: Vector3d) {
            val regionScale = toRegion.transformDirection(x, Vector3d()).length()
            val steps = ceil(movement.length() / (.4 * regionScale)).toInt().coerceIn(1, 20)
            val current = AABBd(occupied).transform(toRegion).toMinecraft()
            val bounds = current.expandTowards(movement.x, movement.y, movement.z)
            if (bounds.xsize > 64 || bounds.ysize > 64 || bounds.zsize > 64) return
            val low = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ)
            val high = BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)
            for (cx in (low.x shr 4)..(high.x shr 4)) for (cz in (low.z shr 4)..(high.z shr 4)) {
                val chunk = level.chunkSource.getChunkNow(cx, cz) ?: continue
                for (sy in (maxOf(low.y, level.minBuildHeight) shr 4)..(minOf(high.y, level.maxBuildHeight - 1) shr 4)) {
                    val section = chunk.getSection(level.getSectionIndexFromSectionY(sy))
                    if (section.hasOnlyAir() || !section.maybeHas { severity(it) > 0 }) continue
                    for (pos in BlockPos.betweenClosed(maxOf(low.x, cx * 16), maxOf(low.y, sy * 16), maxOf(low.z, cz * 16),
                        minOf(high.x, cx * 16 + 15), minOf(high.y, sy * 16 + 15), minOf(high.z, cz * 16 + 15))) {
                        val state = section.getBlockState(pos.x and 15, pos.y and 15, pos.z and 15)
                        val cuts = severity(state)
                        if (cuts == 0) continue
                        val shape = state.getCollisionShape(level, pos).takeUnless { it.isEmpty } ?: state.getShape(level, pos)
                        val boxes = shape.toAabbs().map { it.move(pos) }
                        val key = pos.asLong()
                        var hit = boxes.firstOrNull { hitsTread(toRegion, tire, it) }
                        if (hit != null) {
                            overlaps.add(key)
                        }
                        if (key in touching) continue
                        if (hit == null) for (i in 1 until steps) {
                            val t = i.toDouble() / steps
                            hit = boxes.firstNotNullOfOrNull { box ->
                                box.move(-movement.x * t, -movement.y * t, -movement.z * t).takeIf { hitsTread(toRegion, tire, it) }
                            }
                            if (hit != null) break
                        }
                        if (hit != null) puncture(cuts.toDouble(), toRegion.invert(Matrix4d()).transformPosition(hit.center.toJOML()))
                    }
                }
            }
        }
        scan(pose, travel)
        for (ship in level.getShipsIntersecting(worldBounds)) {
            val toShip = ship.transform.worldToShip
            val relativeTravel = if (sweep && previousTick == level.gameTime - 1)
                ship.prevTickTransform.worldToShip.transformPosition(previousCenter, Vector3d())
                    .sub(toShip.transformPosition(center, Vector3d()))
            else Vector3d()
            scan(Matrix4d(toShip).mul(pose), relativeTravel)
        }
        touching = overlaps
        previousBody = body.id
        previousTick = level.gameTime
        previousCenter.set(center)
    }

    companion object {
        private val normal = TagKey.create(Registries.BLOCK, ClockworkMod.asResource("tire_punctures"))
        private val severe = TagKey.create(Registries.BLOCK, ClockworkMod.asResource("severe_tire_punctures"))
        private val x = Vector3d(1.0, 0.0, 0.0)
        private val y = Vector3d(0.0, 1.0, 0.0)
        private val z = Vector3d(0.0, 0.0, 1.0)
        private val spokes = List(16) { Vector3d(0.0, cos(it * PI / 8), sin(it * PI / 8)) }

        private fun severity(state: BlockState) = when {
            state.`is`(severe) -> 3
            state.`is`(normal) -> 1
            else -> 0
        }

        private fun hitsTread(pose: Matrix4dc, tire: WheelTire, box: AABB): Boolean {
            val axle = pose.transformDirection(x, Vector3d())
            val scale = axle.length()
            axle.normalize()
            val center = box.center.toJOML()
            val half = Vector3d(box.xsize, box.ysize, box.zsize).mul(.5)
            val outer = tire.radius * cos(PI / 16)
            val middle = (outer + tire.rimRadius) / 2
            val tread = Vector3d(tire.width / 2, (outer - tire.rimRadius).coerceAtLeast(.002) / 2,
                tire.radius * sin(PI / 16)).mul(scale)
            return spokes.any { spoke ->
                val radial = pose.transformDirection(spoke, Vector3d()).normalize()
                val position = pose.transformPosition(Vector3d(spoke).mul(middle))
                Intersectiond.testObOb(position, axle, radial, axle.cross(radial, Vector3d()), tread, center, x, y, z, half)
            }
        }
    }
}
