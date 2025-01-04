package org.valkyrienskies.clockwork.content.logistics.gas.valve

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.utility.AngleHelper
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkPartials

class ValveDuctRenderer(context: BlockEntityRendererProvider.Context?) : KineticBlockEntityRenderer<ValveDuctBlockEntity>(context) {


    override fun renderSafe(be: ValveDuctBlockEntity, partialTicks: Float, ms: PoseStack?, buffer: MultiBufferSource, light: Int, overlay: Int) {


        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
        val blockState = be.blockState
        val pointer = CachedBufferer.partial(ClockworkPartials.VALVE_DUCT_POINTER, blockState)
        val facing = blockState.getValue(BlockStateProperties.FACING)

        val pointerRotation = Mth.lerp(be.pointer.getValue(partialTicks), 0f, -90f)
        val ductAxis = ValveDuctBlock.getDuctAxis(blockState)
        val shaftAxis = getRotationAxisOf(be)

        var pointerRotationOffset = 0
        if (ductAxis.isHorizontal && shaftAxis === Direction.Axis.X || ductAxis.isVertical) pointerRotationOffset = 90

        pointer.centre()
            .rotateY(AngleHelper.horizontalAngle(facing).toDouble())
            .rotateX((if (facing == Direction.UP) 0 else if (facing == Direction.DOWN) 180 else 90).toDouble())
            .rotateY((pointerRotationOffset + pointerRotation).toDouble())
            .unCentre()
            .light(light)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }

    override fun getRenderedBlockState(be: ValveDuctBlockEntity?): BlockState? {
        return shaft(getRotationAxisOf(be))
    }
}