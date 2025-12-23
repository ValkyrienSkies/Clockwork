package org.valkyrienskies.clockwork.content.logistics.gas.valve

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import dev.engine_room.flywheel.api.visualization.VisualizationManager
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkPartials

class ValveDuctRenderer(context: BlockEntityRendererProvider.Context?) : KineticBlockEntityRenderer<ValveDuctBlockEntity>(context) {


    override fun renderSafe(be: ValveDuctBlockEntity, partialTicks: Float, ms: PoseStack?, buffer: MultiBufferSource, light: Int, overlay: Int) {

        if (VisualizationManager.supportsVisualization(be.getLevel())) return

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
        val blockState = be.blockState
        val pointer = CachedBuffers.partial(ClockworkPartials.VALVE_DUCT_POINTER, blockState)
        val facing = blockState.getValue(DirectionalKineticBlock.FACING)

        val pointerRotation = Mth.lerp(be.pointer.getValue(partialTicks), 0f, -90f)
        val ductAxis = ValveDuctBlock.getDuctAxis(blockState)
        val shaftAxis = getRotationAxisOf(be)

        var pointerRotationOffset = 0
        if (ductAxis.isHorizontal && shaftAxis === Direction.Axis.X || ductAxis.isVertical) pointerRotationOffset = 90

        pointer.center()
            .rotateYDegrees(AngleHelper.horizontalAngle(facing))
            .rotateXDegrees((if (facing == Direction.UP) 0f else if (facing == Direction.DOWN) 180f else 90f))
            .rotateYDegrees((pointerRotationOffset + pointerRotation))
            .uncenter()
            .light<SuperByteBuffer>(light)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }

    override fun getRenderedBlockState(be: ValveDuctBlockEntity?): BlockState? {
        return shaft(getRotationAxisOf(be))
    }
}
