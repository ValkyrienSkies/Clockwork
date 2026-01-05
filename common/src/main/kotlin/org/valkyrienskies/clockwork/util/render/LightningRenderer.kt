package org.valkyrienskies.clockwork.util.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.util.render.RenderUtil.addRibbonSegment
import org.valkyrienskies.clockwork.util.render.RenderUtil.generatePolylinesFollow

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
            val age = (now - inst.birthGameTime).toFloat() + partialTick
            val fade = (1f - age / inst.lifeTicks.toFloat()).coerceIn(0f, 1f)

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
                        r = 247f/255f, g = 207f/255f, bl = 239f/255f,
                        alpha = 0.65f * fade
                    )

                    // optional softer shell (still translucent, not additive)
                    addRibbonSegment(
                        vc, pose, a, b,
                        thickness = inst.thickness * 1.6f * (0.35f + 0.65f * taper),
                        r = 195f/255f, g = 160f/255f, bl = 227f/255f,
                        alpha = 0.18f * fade
                    )
                }
            }
        }

        buffers.endBatch(rt)
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
