package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.inventory.InventoryMenu
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.clockwork.util.render.ShaderPackCompat

class PhysicsInfuserRenderer(context: BlockEntityRendererProvider.Context?) :
    SmartBlockEntityRenderer<PhysicsInfuserBlockEntity>(context) {
    override fun renderSafe(be: PhysicsInfuserBlockEntity, partialTicks: Float, ms: PoseStack,
                            buffer: MultiBufferSource, light: Int, overlay: Int) {
        InfuserEffects.track(be)
        val now = be.level?.gameTime ?: 0L
        val sequence = be.sequence
        val age = sequence?.age(now, partialTicks) ?: 0f
        val lift = sequence?.lift(age) ?: 0f
        val motion = InfuserEffects.pose(be, age)
        CachedBuffers.partial(ClockworkPartials.INFUSER_FRAME, be.blockState)
            .rotateCentered((-Math.PI / 4).toFloat(), net.minecraft.core.Direction.UP)
            .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay)
            .renderInto(ms, buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)))

        ms.pushPose()
        ms.translate(0.5 + motion.offset.x, 0.65 + lift + motion.offset.y, 0.5 + motion.offset.z)
        ms.mulPose(Axis.YP.rotationDegrees(InfuserSequence.angle(now, partialTicks, 1) + motion.yaw))
        ms.mulPose(Axis.XP.rotationDegrees(InfuserSequence.angle(now, partialTicks, 0) + motion.pitch))
        ms.mulPose(Axis.ZP.rotationDegrees(InfuserSequence.angle(now, partialTicks, 2) + motion.roll))
        val size = motion.scale
        ms.scale(size, size, size)
        // These item partials are exported around (8, 12.5, 8), not the block center.
        ms.translate(-0.5, -12.5 / 16.0, -0.5)
        val crystalType = if (ShaderPackCompat.enabled()) RenderType.entityTranslucentEmissive(RenderUtil.PURPLE_HUE)
            else ClockworkRenderTypes.CRYSTAL.apply(RenderUtil.CRYSTAL_MATRIX)
        CachedBuffers.partial(ClockworkPartials.CRYSTAL_INNER, be.blockState).light<SuperByteBuffer>(0xF000F0)
            .renderInto(ms, buffer.getBuffer(if (ShaderPackCompat.enabled())
                RenderType.entityTranslucentEmissive(RenderUtil.PURPLE_HUE) else RenderType.endPortal()))
        CachedBuffers.partial(ClockworkPartials.CRYSTAL, be.blockState).light<SuperByteBuffer>(0xF000F0)
            .renderInto(ms, buffer.getBuffer(crystalType))
        CachedBuffers.partial(ClockworkPartials.CRYSTAL_OUTER, be.blockState).light<SuperByteBuffer>(0xF000F0)
            .renderInto(ms, buffer.getBuffer(RenderType.entityTranslucentEmissive(RenderUtil.PURPLE_HUE)))
        ms.popPose()
    }

    override fun shouldRenderOffScreen(be: PhysicsInfuserBlockEntity) = be.assembling
    override fun getViewDistance() = 128

    companion object {
        const val SCAN_GROWTH_DURATION = 2000 // Legacy scanner compatibility.
    }
}
