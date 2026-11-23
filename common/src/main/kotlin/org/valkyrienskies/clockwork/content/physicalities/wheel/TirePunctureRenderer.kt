package org.valkyrienskies.clockwork.content.physicalities.wheel

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkMod
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.sin

internal class TirePunctureRenderer(private val tire: WheelTire, contacts: List<TireContact>) {
    private val layered = tire.type.appearance.mesh == TireType.Mesh.LAYERED
    private val radius = tire.carcassRadius - when (tire.type.appearance.cleats) {
        TireType.Cleats.PADDLES, TireType.Cleats.TEETH -> LayeredTireRenderer.cleatHeight(tire)
        else -> 0.0
    }
    private val profile = if (layered) LayeredTireRenderer.profile(tire, tire.width, radius)
        else TireMesh.profile(radius, tire.width, tire.rimRadius, tire.rimWidth).map { (x, r) -> x * tire.width to r * radius }
    private val deformation = TireDeformation(tire, contacts, if (layered) 1.0 else .5) { normal ->
        val radial = (0 until 8).maxOf { i ->
            val angle = (i - .5) * PI / 4
            -cos(angle) * normal.y - sin(angle) * normal.z
        }
        if (layered) radial * radius else profile.maxOf { (x, r) -> r * radial - x * normal.x }
    }

    data class Surface(val point: Vector3d, val tangent: Vector3d, val normal: Vector3d)

    private fun point(angle: Double, across: Double): Vector3d {
        val band = (across + 1) * .5 * profile.lastIndex
        val index = floor(band).toInt().coerceIn(0, profile.lastIndex - 1)
        val fraction = (band - index).coerceIn(0.0, 1.0)
        val face = round(angle / (PI / 4)) * PI / 4
        val apothem = cos(PI / 8)
        val radial = apothem / cos(angle - face)
        fun ring(i: Int): Vector3d {
            val (x, r) = profile[i]
            val p = Vector3d(x, cos(angle) * radial * r, sin(angle) * radial * r)
            val rubber = when {
                i == 0 || i == profile.lastIndex -> 0.0
                layered -> (r - tire.rimRadius) / tire.carcassThickness
                else -> (hypot(p.y, p.z) / apothem - tire.rimRadius) / (radius - tire.rimRadius)
            }
            return deformation.apply(p, rubber.coerceIn(0.0, 1.0))
        }
        return ring(index).lerp(ring(index + 1), fraction)
    }

    fun surface(hole: TireCondition.Hole): Surface? {
        val p = point(hole.angle, hole.across)
        val tangent = point(hole.angle + .004, hole.across).sub(p)
        if (tangent.lengthSquared() < 1e-12) return null
        tangent.normalize()
        val normal = tangent.cross(point(hole.angle, hole.across + .004).sub(p), Vector3d())
        if (normal.lengthSquared() < 1e-12) return null
        normal.normalize()
        return Surface(p.fma(.005, normal), tangent, normal)
    }

    fun render(holes: List<TireCondition.Hole>, stack: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val buffer = buffers.getBuffer(type)
        val pose = stack.last()
        for (hole in holes) {
            val (p, tangent, normal) = surface(hole) ?: continue
            val across = normal.cross(tangent, Vector3d())
            val halfSize = minOf(.095, tire.width * .28)
            val halfWidth = if (abs(hole.across) > .65) minOf(halfSize, tire.carcassThickness * .32) else halfSize
            for (corner in 0..3) {
                val u = if (corner == 1 || corner == 2) 1f else 0f
                val v = if (corner >= 2) 1f else 0f
                val vertex = Vector3d(p).fma((2 * u - 1) * halfSize, tangent).fma((2 * v - 1) * halfWidth, across)
                buffer.vertex(pose.pose(), vertex.x.toFloat(), vertex.y.toFloat(), vertex.z.toFloat()).color(255, 255, 255, 255)
                    .uv(u, v).overlayCoords(overlay).uv2(light)
                    .normal(pose.normal(), normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).endVertex()
            }
        }
    }

    companion object {
        private val type = RenderType.entityCutoutNoCull(ClockworkMod.asResource("textures/block/wheel/puncture.png"))
    }
}
