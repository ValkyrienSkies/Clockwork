package org.valkyrienskies.clockwork.content.curiosities.spring

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.*

object SpringRenderer {
    private val type = RenderType.entityCutoutNoCull(ClockworkMod.asResource("textures/entity/spring.png"))
    private val capType = RenderType.entityCutoutNoCull(ClockworkMod.asResource("textures/entity/spring_end.png"))
    private data class Snap(val a: WandAnchor?, val b: WandAnchor?, val start: Vec3, val end: Vec3,
                            val restLength: Double, val born: Long)
    private val snaps = mutableListOf<Snap>()
    private var world: ClientLevel? = null

    private fun checkWorld(level: ClientLevel) {
        if (world !== level) { snaps.clear(); world = level }
    }

    private fun renderRotation(level: ClientLevel, anchor: WandAnchor) =
        (level.shipObjectWorld.loadedShips.getById(anchor.shipId) as? ClientShip)?.renderTransform?.shipToWorldRotation

    private fun transform(level: ClientLevel, anchor: WandAnchor, local: Vec3): Vec3 {
        val v = Vector3d(local.x, local.y, local.z)
        renderRotation(level, anchor)?.transform(v)
        return Vec3(v.x, v.y, v.z)
    }

    private fun normal(level: ClientLevel, anchor: WandAnchor) = transform(level, anchor,
        Vec3(anchor.face.stepX.toDouble(), anchor.face.stepY.toDouble(), anchor.face.stepZ.toDouble()))

    private fun side(level: ClientLevel, anchor: WandAnchor): Vec3 {
        val n = Vec3.atLowerCornerOf(anchor.face.normal)
        return transform(level, anchor, if (abs(n.x) < 0.5) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 0.0, 1.0))
    }

    fun render(level: ClientLevel, a: WandAnchor, b: WandAnchor, frameA: Quaterniondc, frameB: Quaterniondc,
               restLength: Double, origin: Vec3, pose: PoseStack, buffers: MultiBufferSource) {
        checkWorld(level)
        val start = a.world(level, true) ?: return
        val end = b.world(level, true) ?: return
        if (start.distanceToSqr(end) < 1e-8) return
        val na = normal(level, a)
        val nb = normal(level, b)
        val worldA = Quaterniond(renderRotation(level, a) ?: Quaterniond()).mul(frameA)
        val worldB = Quaterniond(renderRotation(level, b) ?: Quaterniond()).mul(frameB)
        val side = worldA.transform(Vector3d(0.0, 1.0, 0.0))
        strand(level, SpringShape.points(start, end, na, nb), na, nb.scale(-1.0),
            Vec3(side.x, side.y, side.z), SpringJoints.twistAngle(worldA, worldB),
            restLength, 1.0, origin, pose, buffers)
    }

    fun preview(level: ClientLevel, anchor: WandAnchor, end: Vec3, origin: Vec3, pose: PoseStack, buffers: MultiBufferSource) {
        checkWorld(level)
        val start = anchor.world(level, true) ?: return
        if (start.distanceToSqr(end) < 1e-8) return
        val na = normal(level, anchor)
        val direction = end.subtract(start).normalize()
        strand(level, SpringShape.points(start, end, na, direction.scale(-1.0)), na, direction, side(level, anchor), 0.0,
            start.distanceTo(end), 1.0, origin, pose, buffers)
    }

    fun acceptBreak(level: ClientLevel, tag: CompoundTag) {
        checkWorld(level)
        val start = Vec3(tag.getDouble("ax"), tag.getDouble("ay"), tag.getDouble("az"))
        val end = Vec3(tag.getDouble("bx"), tag.getDouble("by"), tag.getDouble("bz"))
        val length = tag.getDouble("length")
        if (!length.isFinite() || length <= 0) return
        if (snaps.size >= 32) snaps.removeAt(0)
        snaps.add(Snap(if (tag.contains("a")) WandAnchor.load(tag.getCompound("a")) else null,
            if (tag.contains("b")) WandAnchor.load(tag.getCompound("b")) else null, start, end, length, level.gameTime))
        if (!tag.getBoolean("untied")) {
            val center = start.lerp(end, 0.5)
            repeat(10) {
                level.addParticle(ParticleTypes.CRIT, center.x, center.y, center.z,
                    (level.random.nextDouble() - 0.5) * 0.3, (level.random.nextDouble() - 0.5) * 0.3,
                    (level.random.nextDouble() - 0.5) * 0.3)
            }
        }
    }

    fun renderEffects(level: ClientLevel, origin: Vec3, pose: PoseStack, buffers: MultiBufferSource, partialTick: Float) {
        checkWorld(level)
        snaps.removeAll { level.gameTime - it.born >= 14 }
        for (snap in snaps) {
            val progress = ((level.gameTime - snap.born + partialTick.toDouble()) / 14).coerceIn(0.0, 1.0)
            val start = snap.a?.world(level, true) ?: snap.start
            val end = snap.b?.world(level, true) ?: snap.end
            val direction = end.subtract(start).normalize()
            val na = snap.a?.let { normal(level, it) } ?: direction
            val nb = snap.b?.let { normal(level, it) } ?: direction.scale(-1.0)
            val curve = SpringShape.points(start, end, na, nb)
            val reach = SpringShape.recoilReach(progress)
            fun point(t: Double): Vec3 {
                val index = (t * curve.lastIndex).coerceIn(0.0, curve.lastIndex.toDouble())
                val i = floor(index).toInt()
                return curve[i].lerp(curve[minOf(i + 1, curve.lastIndex)], index - i)
            }
            for (endpoint in 0..1) {
                val points = (0..32).map { i -> point(if (endpoint == 0) i / 32.0 * reach else 1 - i / 32.0 * reach) }
                val n = if (endpoint == 0) na else nb
                val anchor = if (endpoint == 0) snap.a else snap.b
                val basis = anchor?.let { side(level, it) } ?: perpendicular(n)
                strand(level, points, n, points.last().subtract(points[points.lastIndex - 1]).normalize(), basis, 0.0,
                    snap.restLength * 0.5, ((1 - progress) / 0.2).coerceIn(0.0, 1.0), origin, pose, buffers, capEnd = false)
            }
        }
    }

    private fun perpendicular(tangent: Vec3) = tangent.cross(
        if (abs(tangent.y) < 0.9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)).normalize()

    private fun strand(level: ClientLevel, points: List<Vec3>, startNormal: Vec3, endNormal: Vec3,
                       initialSide: Vec3, twist: Double, restLength: Double, scale: Double,
                       origin: Vec3, pose: PoseStack, buffers: MultiBufferSource, capEnd: Boolean = true) {
        if (points.first().distanceToSqr(points.last()) < 1e-10) return
        val tangents = points.indices.map { i ->
            when {
                i <= 1 -> startNormal
                i >= points.lastIndex - 1 -> endNormal
                else -> points[i + 1].subtract(points[i - 1]).normalize()
            }
        }
        var side = initialSide
        val sides = tangents.map { tangent ->
            val projected = side.subtract(tangent.scale(side.dot(tangent)))
            side = if (projected.lengthSqr() > 1e-8) projected.normalize() else perpendicular(tangent)
            side
        }
        val radius = SpringShape.WIDTH * 0.5 * scale
        val rings = points.indices.map { i ->
            val t = ((i - 1).toDouble() / (points.lastIndex - 2)).coerceIn(0.0, 1.0)
            val angle = twist * t * t * (3 - 2 * t)
            val tangent = tangents[i]
            val s = sides[i].scale(cos(angle)).add(tangent.cross(sides[i]).scale(sin(angle)))
            val up = tangent.cross(s)
            listOf(points[i].add(s.scale(-radius)).add(up.scale(-radius)),
                points[i].add(s.scale(radius)).add(up.scale(-radius)),
                points[i].add(s.scale(radius)).add(up.scale(radius)),
                points[i].add(s.scale(-radius)).add(up.scale(radius)))
        }
        val lengths = DoubleArray(points.size)
        for (i in 1..points.lastIndex) lengths[i] = lengths[i - 1] + points[i].distanceTo(points[i - 1])
        val total = lengths.last().coerceAtLeast(1e-6)
        val transform = pose.last()
        fun vertex(consumer: VertexConsumer, point: Vec3, u: Float, v: Float, light: Int, normal: Vec3) {
            val local = point.subtract(origin)
            consumer.vertex(transform.pose(), local.x.toFloat(), local.y.toFloat(), local.z.toFloat())
                .color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(transform.normal(), normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).endVertex()
        }
        val consumer = buffers.getBuffer(type)
        for (i in 0 until points.lastIndex) {
            val light = LevelRenderer.getLightColor(level, BlockPos.containing(points[i]))
            val v0 = (lengths[i] / total * restLength / SpringShape.REPEAT_LENGTH).toFloat()
            val v1 = (lengths[i + 1] / total * restLength / SpringShape.REPEAT_LENGTH).toFloat()
            for (face in 0..3) {
                val next = (face + 1) % 4
                val n = rings[i][face].add(rings[i][next]).scale(0.5).subtract(points[i]).normalize()
                vertex(consumer, rings[i][face], 0f, v0, light, n)
                vertex(consumer, rings[i + 1][face], 0f, v1, light, n)
                vertex(consumer, rings[i + 1][next], 1f, v1, light, n)
                vertex(consumer, rings[i][next], 1f, v0, light, n)
            }
        }
        val caps = buffers.getBuffer(capType)
        fun cap(index: Int) {
            val ring = rings[index]
            val first = index == 0
            val corners = if (first) listOf(ring[1], ring[0], ring[3], ring[2]) else ring
            val normal = if (first) startNormal.scale(-1.0) else endNormal
            val light = LevelRenderer.getLightColor(level, BlockPos.containing(points[index]))
            vertex(caps, corners[0], 0f, 1f, light, normal)
            vertex(caps, corners[1], 1f, 1f, light, normal)
            vertex(caps, corners[2], 1f, 0f, light, normal)
            vertex(caps, corners[3], 0f, 0f, light, normal)
        }
        cap(0)
        if (capEnd) cap(rings.lastIndex)
    }
}
