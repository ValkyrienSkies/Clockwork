package org.valkyrienskies.clockwork.content.curiosities.strut

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.abs

internal object StrutRenderer {
    private fun material(name: String) = RenderType.entityCutoutNoCull(ClockworkMod.asResource("textures/entity/strut/$name.png"))
    private val side = material("side")
    private val end = material("end")
    private val base = material("base")

    fun render(level: ClientLevel, shipId: Long, a: Vec3, b: Vec3, origin: Vec3, ms: PoseStack, buffers: MultiBufferSource, tint: Int = 0xffffff) {
        val length = a.distanceTo(b)
        if (length < .001) return
        val forward = b.subtract(a).normalize()
        val rotation = (level.shipObjectWorld.loadedShips.getById(shipId) as? ClientShip)?.renderTransform?.shipToWorldRotation
        val local = Vector3d(forward.x, forward.y, forward.z)
        rotation?.transformInverse(local)
        val reference = if (abs(local.y) < .9) Vector3d(0.0, 1.0, 0.0) else Vector3d(1.0, 0.0, 0.0)
        rotation?.transform(reference)
        val right = forward.cross(Vec3(reference.x, reference.y, reference.z)).normalize()
        val up = forward.cross(right)

        fun prism(start: Double, stop: Double, radius: Double, sides: RenderType, cap: RenderType) {
            val first = a.add(forward.scale(start))
            val last = a.add(forward.scale(stop))
            val offsets = listOf(right.scale(-radius).add(up.scale(-radius)), right.scale(radius).add(up.scale(-radius)),
                right.scale(radius).add(up.scale(radius)), right.scale(-radius).add(up.scale(radius)))
            val lightA = LevelRenderer.getLightColor(level, BlockPos.containing(first))
            val lightB = LevelRenderer.getLightColor(level, BlockPos.containing(last))
            fun face(material: RenderType, normal: Vec3, points: List<Vec3>, repeat: Float, lights: List<Int>) {
                val consumer = buffers.getBuffer(material)
                for (i in points.indices) {
                    val point = points[i].subtract(origin)
                    consumer.vertex(ms.last().pose(), point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
                        .color(tint shr 16 and 255, tint shr 8 and 255, tint and 255, 255)
                        .uv(if (i == 1 || i == 2) 1f else 0f, if (i >= 2) repeat else 0f)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lights[i])
                        .normal(ms.last().normal(), normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).endVertex()
                }
            }
            for (i in offsets.indices) {
                val p = offsets[i]
                val q = offsets[(i + 1) % 4]
                face(sides, p.add(q).normalize(), listOf(first.add(p), first.add(q), last.add(q), last.add(p)),
                    ((stop - start) / (radius * 2)).toFloat(), listOf(lightA, lightA, lightB, lightB))
            }
            face(cap, forward.scale(-1.0), offsets.reversed().map(first::add), 1f, List(4) { lightA })
            face(cap, forward, offsets.map(last::add), 1f, List(4) { lightB })
        }

        val collar = minOf(1 / 16.0, length / 3)
        prism(collar, length - collar, 2 / 16.0, side, end)
        prism(0.0, collar, 2.5 / 16.0, end, base)
        prism(length - collar, length, 2.5 / 16.0, end, base)
    }
}
