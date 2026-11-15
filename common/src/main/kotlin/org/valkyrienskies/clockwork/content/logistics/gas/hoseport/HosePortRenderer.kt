package org.valkyrienskies.clockwork.content.logistics.gas.hoseport

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.client.render.goggles.AeronautGoggleOverlayRenderer
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeCurve
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonRenderer
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos

class HosePortRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<HosePortBlockEntity>(context) {
    override fun shouldRenderOffScreen(blockEntity: HosePortBlockEntity) = true

    override fun renderSafe(be: HosePortBlockEntity, partialTicks: Float, ms: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val level = be.level ?: return
        val stress = be.previousStress + (be.displayStress - be.previousStress) * partialTicks
        val tint = AeronautGoggleOverlayRenderer.stressColor(stress)
        if (tint != 0xFFFFFF) {
            ms.pushPose()
            ms.translate(-.001, -.001, -.001)
            ms.scale(1.002f, 1.002f, 1.002f)
            CachedBuffers.block(be.blockState).color<SuperByteBuffer>(tint).light<SuperByteBuffer>(light)
                .renderInto(ms, buffers.getBuffer(RenderType.cutout()))
            ms.popPose()
        }
        val other = be.connectedBe ?: return
        if (!be.main) return
        val ship = level.getShipManagingPos(be.blockPos) as? ClientShip
        val otherShip = level.getShipManagingPos(other.blockPos) as? ClientShip
        val first = Vector3d(be.blockPos.x + .5, be.blockPos.y + .5, be.blockPos.z + .5)
        val second = Vector3d(other.blockPos.x + .5, other.blockPos.y + .5, other.blockPos.z + .5)
        ship?.renderTransform?.shipToWorld?.transformPosition(first)
        otherShip?.renderTransform?.shipToWorld?.transformPosition(second)
        val a = Vec3(first.x, first.y, first.z); val b = Vec3(second.x, second.y, second.z)
        val distance = a.distanceTo(b)
        if (!distance.isFinite() || distance < .001) return
        val curve = WandRopeCurve(a, b, be.connectionLength.coerceAtLeast(distance))
        val count = ((distance + curve.sag * 2) * 6).toInt().coerceIn(8, 128)
        val points = (0..count).map { i ->
            val point = curve.point(i.toDouble() / count)
            val local = Vector3d(point.x, point.y, point.z)
            ship?.renderTransform?.worldToShip?.transformPosition(local)
            Vec3(local.x - be.blockPos.x, local.y - be.blockPos.y, local.z - be.blockPos.z)
        }
        val horizontal = points.last().subtract(points.first()).cross(Vec3(0.0, 1.0, 0.0))
        val side = if (horizontal.lengthSqr() < 1e-8) Vec3(1.0, 0.0, 0.0) else horizontal.normalize()
        val radius = 2.0 / 16.0
        val rings = points.mapIndexed { i, p ->
            val tangent = points[(i + 1).coerceAtMost(count)].subtract(points[(i - 1).coerceAtLeast(0)]).normalize()
            val up = tangent.cross(side).normalize()
            listOf(p.add(side.scale(-radius)).add(up.scale(-radius)), p.add(side.scale(radius)).add(up.scale(-radius)),
                p.add(side.scale(radius)).add(up.scale(radius)), p.add(side.scale(-radius)).add(up.scale(radius)))
        }
        val vc = buffers.getBuffer(RenderType.entitySolid(ExtendonRenderer.texture))
        var length = 0.0
        for (i in 0 until count) {
            val nextLength = length + points[i].distanceTo(points[i + 1])
            for (face in 0..3) {
                val next = (face + 1) % 4
                val normal = rings[i][face].add(rings[i][next]).scale(.5).subtract(points[i]).normalize()
                fun vertex(point: Vec3, u: Float, v: Float) {
                    vc.vertex(ms.last().pose(), point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
                        .color(tint shr 16 and 255, tint shr 8 and 255, tint and 255, 255).uv(u, v)
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                        .normal(ms.last().normal(), normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).endVertex()
                }
                vertex(rings[i][face], 0f, length.toFloat())
                vertex(rings[i + 1][face], 0f, nextLength.toFloat())
                vertex(rings[i + 1][next], 4f / 16f, nextLength.toFloat())
                vertex(rings[i][next], 4f / 16f, length.toFloat())
            }
            length = nextLength
        }
    }
}
