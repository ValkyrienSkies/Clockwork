package org.valkyrienskies.clockwork.content.contraptions.phys.linear

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties

class PhysicalGantryRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<PhysicalGantryBlockEntity>(context) {

    override fun getRenderedBlockState(be: PhysicalGantryBlockEntity): BlockState = shaft(getRotationAxisOf(be))

    override fun renderSafe(be: PhysicalGantryBlockEntity, partialTicks: Float, pose: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        super.renderSafe(be, partialTicks, pose, buffers, light, overlay)
        val state = be.blockState
        val facing = state.getValue(BlockStateProperties.FACING)
        val alongFirst = state.getValue(AXIS_ALONG_FIRST_COORDINATE)
        val cogs = CachedBuffers.partial(AllPartialModels.GANTRY_COGS, state)
        cogs.center()
            .rotateYDegrees(AngleHelper.horizontalAngle(facing))
            .rotateXDegrees(when (facing) {
                Direction.UP -> 0f
                Direction.DOWN -> 180f
                else -> 90f
            })
            .rotateYDegrees(if (alongFirst xor (facing.axis == Direction.Axis.X)) 0f else 90f)
            .uncenter()
        cogs.light<SuperByteBuffer>(light).renderInto(pose, buffers.getBuffer(RenderType.solid()))
    }
}
