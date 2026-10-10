package org.valkyrienskies.clockwork.util.render

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronEffects

/** Draw world-space tool effects after vanilla translucency, before shader-pack postprocessing. */
object ToolWorldEffects {
    // Iris/Oculus replaces Minecraft's shared BufferSource with an entity batcher whose
    // endBatch(RenderType) is a no-op, even with packs disabled. Late geometry can survive
    // until the next entity flush and be covered by that frame's water. Own an immediate
    // source so ribbons, overlays and markers really finish at this point in the frame.
    private val buffers = MultiBufferSource.immediate(BufferBuilder(262144))

    @JvmStatic
    fun render(poseStack: PoseStack, camera: Camera, partialTick: Float) {
        val mc = Minecraft.getInstance()
        if (mc.level == null || ShaderPackCompat.shadowPass()) return

        // MAIN_TARGET's RenderStateShard is a no-op. Explicitly choose the main target
        // without packs; with packs, their vanilla shader overrides select the correct FBO.
        if (!ShaderPackCompat.enabled()) mc.mainRenderTarget.bindWrite(false)
        poseStack.pushPose()
        try {
            ClockworkModClient.WANDERWAND_EFFECT_RENDERER.render(poseStack, buffers, camera.position, partialTick)
            GravitronEffects.render(poseStack, buffers, partialTick)
            buffers.endBatch()
        } finally {
            RenderSystem.enableCull()
            poseStack.popPose()
        }
    }
}
