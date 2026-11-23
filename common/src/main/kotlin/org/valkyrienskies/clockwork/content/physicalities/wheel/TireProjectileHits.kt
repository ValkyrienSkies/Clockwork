package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.projectile.Projectile
import org.joml.Vector3d
import org.joml.primitives.AABBd
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.toWorldCoordinates
import kotlin.math.abs
import kotlin.math.sqrt

object TireProjectileHits {
    private val NORMAL = TagKey.create(Registries.ENTITY_TYPE, ClockworkMod.asResource("tire_punctures"))
    private val SEVERE = TagKey.create(Registries.ENTITY_TYPE, ClockworkMod.asResource("severe_tire_punctures"))

    @JvmStatic
    fun check(level: ServerLevel, projectile: Projectile) {
        if (projectile.xOld == projectile.x && projectile.yOld == projectile.y && projectile.zOld == projectile.z) return
        val severity = when {
            projectile.type.`is`(SEVERE) -> 3.0
            projectile.type.`is`(NORMAL) -> 1.0
            else -> return
        }
        val start = level.toWorldCoordinates(projectile.xOld, projectile.yOld, projectile.zOld)
        val end = level.toWorldCoordinates(projectile.x, projectile.y, projectile.z)
        if (!start.isFinite || !end.isFinite || start.distanceSquared(end) !in .000001..65536.0) return
        val bounds = AABBd().union(start).union(end)
        var nearest = Double.POSITIVE_INFINITY
        var target: WheelHubBlockEntity? = null
        var hit: Vector3d? = null
        for (body in level.shipObjectWorld.allBodies.getIntersecting(bounds, level.dimensionId)) {
            val hub = WheelContacts.hub(level, body.id) ?: continue
            val tire = hub.tire ?: continue
            val local = body.kinematics.toModel
            val from = local.transformPosition(start, Vector3d())
            val to = local.transformPosition(end, Vector3d())
            val entry = intersection(from, to, tire)
            if (entry < nearest) {
                nearest = entry
                target = hub
                hit = from.lerp(to, entry)
            }
        }
        target?.puncture(severity, hit)
    }

    internal fun intersection(start: Vector3d, end: Vector3d, tire: WheelTire, radius: Double = tire.radius): Double {
        val miss = Double.POSITIVE_INFINITY
        val delta = end.sub(start, Vector3d())
        val half = tire.width / 2
        val radial = start.y * start.y + start.z * start.z - radius * radius
        if (abs(start.x) <= half && radial <= 0.0) return miss
        var enter = 0.0
        var leave = 1.0
        if (abs(delta.x) < 1e-10) {
            if (abs(start.x) > half) return miss
        } else {
            val a = (-half - start.x) / delta.x
            val b = (half - start.x) / delta.x
            enter = maxOf(enter, minOf(a, b))
            leave = minOf(leave, maxOf(a, b))
        }
        val a = delta.y * delta.y + delta.z * delta.z
        if (a < 1e-12) {
            if (radial > 0) return miss
        } else {
            val b = start.y * delta.y + start.z * delta.z
            val discriminant = b * b - a * radial
            if (discriminant < 0) return miss
            val root = sqrt(discriminant)
            enter = maxOf(enter, (-b - root) / a)
            leave = minOf(leave, (-b + root) / a)
        }
        if (enter > leave) return miss
        val y = start.y + enter * delta.y
        val z = start.z + enter * delta.z
        return if (y * y + z * z >= tire.rimRadius * tire.rimRadius) enter else miss
    }
}
