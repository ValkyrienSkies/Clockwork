package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.tags.TagKey
import net.minecraft.world.phys.AABB
import org.joml.Matrix4dc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.core.api.bodies.PhysVsBody
import org.valkyrienskies.core.api.bodies.ServerVsBody
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.mod.common.getShipsIntersecting
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

internal class WheelInteractions {
    private data class Water(val height: Double, val flow: Vector3d)
    private data class Region(val body: Long?, val cells: Map<Long, Water>)
    private data class Snapshot(val wheel: Long, val time: Long, val regions: List<Region>)
    @Volatile private var water: Snapshot? = null
    private var waterTime = Double.NaN

    fun tick(level: ServerLevel, body: ServerVsBody, tire: WheelTire, parent: Long?) {
        if (tire.type.carving && level.gameTime % 2L == 0L) carve(level, body, tire, parent)
        if (!tire.type.floating) { water = null; return }
        if (level.gameTime % 2L != 0L && water?.wheel == body.id) return
        val center = body.kinematics.position
        val reach = (tire.radius + .25) * abs(body.kinematics.scaling.x())
        val bounds = AABB(center.x() - reach, center.y() - reach, center.z() - reach,
            center.x() + reach, center.y() + reach, center.z() + reach).inflate(.25)
        val regions = ArrayList<Region>()
        fun collect(id: Long?, transform: Matrix4dc?) {
            var min = Vector3d(bounds.minX, bounds.minY, bounds.minZ)
            var max = Vector3d(bounds.maxX, bounds.maxY, bounds.maxZ)
            if (transform != null) {
                val corners = (0..7).map { i -> transform.transformPosition(Vector3d(
                    if (i and 1 == 0) bounds.minX else bounds.maxX,
                    if (i and 2 == 0) bounds.minY else bounds.maxY,
                    if (i and 4 == 0) bounds.minZ else bounds.maxZ)) }
                min = Vector3d(corners.minOf { it.x }, corners.minOf { it.y }, corners.minOf { it.z })
                max = Vector3d(corners.maxOf { it.x }, corners.maxOf { it.y }, corners.maxOf { it.z })
            }
            if ((max.x - min.x + 2) * (max.y - min.y + 2) * (max.z - min.z + 2) > 4096) return
            val cells = HashMap<Long, Water>()
            for (pos in BlockPos.betweenClosed(floor(min.x).toInt(), floor(min.y).toInt(), floor(min.z).toInt(),
                floor(max.x).toInt(), floor(max.y).toInt(), floor(max.z).toInt())) {
                if (!level.hasChunkAt(pos)) continue
                val fluid = level.getFluidState(pos)
                if (!fluid.`is`(FluidTags.WATER)) continue
                val flow = fluid.getFlow(level, pos)
                cells[pos.asLong()] = Water(pos.y + fluid.getHeight(level, pos).toDouble(), Vector3d(flow.x, flow.y, flow.z))
            }
            if (cells.isNotEmpty()) regions.add(Region(id, cells))
        }
        collect(null, null)
        for (ship in level.getShipsIntersecting(bounds).take(8)) collect(ship.id, ship.transform.worldToShip)
        water = Snapshot(body.id, System.nanoTime(), regions)
    }

    fun applyWater(level: VsiPhysLevel, body: PhysVsBody, tire: WheelTire) {
        val snapshot = water?.takeIf { it.wheel == body.id && System.nanoTime() - it.time < 250_000_000L } ?: return
        if (body.isStatic || snapshot.regions.isEmpty()) return
        val time = WheelContacts.lastFrame(level.dimension)?.endTime ?: return
        val dt = if (waterTime.isFinite()) (time - waterTime).coerceIn(0.0, .05) else 1.0 / 60
        waterTime = time
        if (dt <= 0) return
        val regions = snapshot.regions.mapNotNull { region ->
            val support = region.body?.let(level::getBodyById)
            if (region.body != null && support == null) null else region to support
        }
        val scale = abs(body.kinematics.scaling.x())
        val volume = PI * (tire.radius * tire.radius - tire.rimRadius * tire.rimRadius) * tire.width * scale * scale * scale
        val gravity = level.getGravity()
        fun sample(local: Vector3d, paddle: Boolean) {
            val point = body.kinematics.toWorld.transformPosition(local, Vector3d())
            for ((region, support) in regions) {
                val probe = support?.kinematics?.toModel?.transformPosition(point, Vector3d()) ?: point
                val pos = BlockPos.containing(probe.x, probe.y, probe.z)
                val fluid = region.cells[pos.asLong()] ?: continue
                val immersion = ((fluid.height - probe.y) / .12).coerceIn(0.0, 1.0)
                if (immersion <= 0.0) continue
                val lever = Vector3d(point).sub(body.kinematics.position)
                val velocity = Vector3d(body.angularVelocity).cross(lever).add(body.velocity)
                val flow = Vector3d(fluid.flow)
                if (support != null) {
                    support.kinematics.rotation.transform(flow)
                    flow.add(Vector3d(support.angularVelocity).cross(Vector3d(point).sub(support.kinematics.position))).add(support.velocity)
                }
                velocity.sub(flow)
                val force = if (!paddle) Vector3d(gravity).mul(-8000.0 * volume * immersion / 64)
                else {
                    val tangent = body.kinematics.rotation.transform(Vector3d(0.0, -local.z, local.y).normalize())
                    val coefficient = 1000.0 * tire.width * tire.radius * .12 * scale * scale * immersion / 2
                    val inertia = body.inertiaData.inertiaTensor.m00() * scale * scale
                    val inverseMass = 1.0 / body.inertiaData.mass + tire.radius * tire.radius * scale * scale / inertia
                    paddleForce(velocity, tangent, coefficient, inverseMass * 16, dt)
                }
                if (force.isFinite) {
                    body.applyWorldForce(force, point)
                    if (support != null && !support.isStatic) support.applyWorldForce(Vector3d(force).negate(), point)
                }
                break
            }
        }
        for (side in listOf(-.3, .3)) {
            for (ring in 0..1) {
                val radius = sqrt(tire.rimRadius * tire.rimRadius +
                    (tire.radius * tire.radius - tire.rimRadius * tire.rimRadius) * (ring + .5) / 2)
                for (i in 0 until 16) sample(Vector3d(side * tire.width, cos(i * PI / 8) * radius, sin(i * PI / 8) * radius), false)
            }
            for (i in 0 until 8) sample(Vector3d(side * tire.width, cos(i * PI / 4) * tire.radius, sin(i * PI / 4) * tire.radius), true)
        }
    }

    private fun carve(level: ServerLevel, body: ServerVsBody, tire: WheelTire, parent: Long?) {
        val axle = body.kinematics.rotation.transform(Vector3d(1.0, 0.0, 0.0))
        if (abs(body.angularVelocity.dot(axle)) < .5) return
        val center = body.kinematics.position
        val reach = tire.radius * abs(body.kinematics.scaling.x()) + .25
        val ships = level.getShipsIntersecting(AABB(center.x() - reach, center.y() - reach, center.z() - reach,
            center.x() + reach, center.y() + reach, center.z() + reach)).filter { it.id != parent }.take(8)
        val candidates = LinkedHashSet<BlockPos>()
        for (i in 0 until 16) for (side in listOf(-.4, 0.0, .4)) {
            val radius = tire.radius + .06
            val point = body.kinematics.toWorld.transformPosition(Vector3d(side * tire.width,
                cos(i * PI / 8) * radius, sin(i * PI / 8) * radius))
            candidates.add(BlockPos.containing(point.x, point.y, point.z))
            for (ship in ships) {
                val local = ship.transform.worldToShip.transformPosition(point, Vector3d())
                candidates.add(BlockPos.containing(local.x, local.y, local.z))
            }
        }
        var broken = 0
        for (pos in candidates) {
            if (!level.hasChunkAt(pos)) continue
            val state = level.getBlockState(pos)
            if (!state.`is`(CARVABLE) || state.hasBlockEntity() || state.getDestroySpeed(level, pos) !in 0f..2f) continue
            if (level.destroyBlock(pos, true) && ++broken == 2) break
        }
    }

    companion object {
        private val CARVABLE by lazy { TagKey.create(Registries.BLOCK, ResourceLocation(ClockworkMod.MOD_ID, "carver_wheel_breakable")) }

        internal fun waterDrag(speed: Double, coefficient: Double, inverseMass: Double, dt: Double): Double {
            if (!speed.isFinite() || coefficient <= 0 || inverseMass <= 0 || dt <= 0) return 0.0
            val damping = coefficient * abs(speed)
            return -damping * speed / (1 + damping * dt * inverseMass)
        }

        internal fun paddleForce(velocity: Vector3dc, tangent: Vector3dc, coefficient: Double, inverseMass: Double, dt: Double): Vector3d {
            val speed = velocity.dot(tangent)
            return Vector3d(tangent).mul(waterDrag(speed, coefficient, inverseMass, dt))
                .add(Vector3d(velocity).fma(-speed, tangent)
                    .mul(-coefficient * .04 / (1 + coefficient * .04 * dt * inverseMass)))
        }
    }
}
