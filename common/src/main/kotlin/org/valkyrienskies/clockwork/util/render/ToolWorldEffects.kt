package org.valkyrienskies.clockwork.util.render

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronEffects
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.InfuserEffects

/** Draw world-space tool effects after vanilla translucency, before shader postprocessing. */
object ToolWorldEffects {
    private val buffers = MultiBufferSource.immediate(BufferBuilder(262144))

    @JvmStatic
    fun render(poseStack: PoseStack, camera: Camera, partialTick: Float) {
        val mc = Minecraft.getInstance()
        if (mc.level == null || ShaderPackCompat.shadowPass()) return

        if (!ShaderPackCompat.enabled()) mc.mainRenderTarget.bindWrite(false)
        poseStack.pushPose()
        try {
            ClockworkModClient.WANDERWAND_EFFECT_RENDERER.render(poseStack, buffers, camera.position, partialTick)
            GravitronEffects.render(poseStack, buffers, partialTick)
            InfuserEffects.render(poseStack, buffers, camera.position, partialTick)
            buffers.endBatch()
        } finally {
            RenderSystem.enableCull()
            poseStack.popPose()
        }
    }
}
