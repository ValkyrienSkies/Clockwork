package org.valkyrienskies.clockwork.util.arc

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.util.render.RenderUtil.addRibbonSegment
import org.valkyrienskies.clockwork.util.render.RenderUtil.generatePolylinesFollow
import org.valkyrienskies.clockwork.util.render.RenderUtil.rand01
import java.math.BigInteger

object LightningRenderer {

    fun onRenderLevelStage(ms: PoseStack, partialTick: Float) {
        //if (e.stage != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return

        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val now = level.gameTime

        val bolts = LightningManager.instances(now)
        if (bolts.isEmpty()) return

        val camPos = mc.gameRenderer.mainCamera.position
        val pose = ms.last().pose()

        val buffers: MultiBufferSource.BufferSource = mc.renderBuffers().bufferSource()
        val rt = ClockworkRenderTypes.WANDER_LIGHTNING
        val vc = buffers.getBuffer(rt)

        for (inst in bolts) {
            renderPolylineRibbon(now, partialTick, inst, vc, pose, camPos, 0xF7CFEF, 0xC3A0E3)
        }

        buffers.endBatch(rt)
    }

    fun renderPolylineRibbon(now: Long, partialTick: Float, inst: LightningBolt, vc: VertexConsumer, pose: Matrix4f, camPos: Vec3, argb: Int, argb2: Int) {
        val age = (now - inst.birthGameTime).toFloat() + partialTick
        val fade = (1f - age / inst.lifeTicks.toFloat()).coerceIn(0f, 1f)

        val red = ((argb ushr 16) and 0xFF) / 255f
        val green = ((argb ushr 8) and 0xFF) / 255f
        val blue = (argb and 0xFF) / 255f

        val red2 = ((argb2 ushr 16) and 0xFF) / 255f
        val green2 = ((argb2 ushr 8) and 0xFF) / 255f
        val blue2 = (argb2 and 0xFF) / 255f

        val lines = generatePolylinesFollow(inst, now, partialTick)

        for (poly in lines) {
            val n = poly.size
            for (i in 0 until n - 1) {
                val t = i.toFloat() / (n - 1).toFloat()
                val taper = (1f - t).coerceIn(0f, 1f)

                val a = poly[i].subtract(camPos)
                val b = poly[i + 1].subtract(camPos)

                // core only (no additive glow, but we can still do a subtle outer)
                addRibbonSegment(
                    vc, pose, a, b,
                    thickness = inst.thickness * (0.35f + 0.65f * taper),
                    r = red, g = green, bl = blue,
                    alpha = 0.65f * fade
                )

                // optional softer shell (still translucent, not additive)
                addRibbonSegment(
                    vc, pose, a, b,
                    thickness = inst.thickness * 1.6f * (0.35f + 0.65f * taper),
                    r = red2, g = green2, bl = blue2,
                    alpha = 0.18f * fade
                )
            }
        }
    }

    fun spawnTestBolt() {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val cam = mc.gameRenderer.mainCamera
        val start = cam.position.add(Vec3(cam.lookVector.mul(2.0f, Vector3f())))
        val end = start.add(Vec3(cam.lookVector.mul(16.0f))).add(0.0, -2.0, 0.0)

        LightningManager.spawn(
            startProvider = {start},
            endProvider = {end},
            seed = System.nanoTime(),
            lifeTicks = 60
        )
    }
}
