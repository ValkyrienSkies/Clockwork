package org.valkyrienskies.clockwork.content.logistics.gas.pockets.blimp

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

class BlimpPortRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<BlimpPortBlockEntity>(context) {

    override fun renderSafe(
        be: BlimpPortBlockEntity,
        partialTicks: Float,
        ms: PoseStack?,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (VisualizationManager.supportsVisualization(be.level)) return

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
        val blockState = be.blockState
        val pointer = CachedBuffers.partial(ClockworkPartials.VALVE_DUCT_POINTER, blockState)
        val pointerFace = BlimpPortBlock.getPointerFace(blockState)

        val pointerRotation = Mth.lerp(be.pointer.getValue(partialTicks), 0f, -90f)
        val ductAxis = blockState.getValue(DirectionalKineticBlock.FACING).axis
        val shaftAxis = getRotationAxisOf(be)

        var pointerRotationOffset = 0
        if ((ductAxis.isHorizontal && shaftAxis === Direction.Axis.X) || ductAxis.isVertical) {
            pointerRotationOffset = 90
        }

        pointer.center()
            .rotateYDegrees(AngleHelper.horizontalAngle(pointerFace))
            .rotateXDegrees(
                when (pointerFace) {
                    Direction.UP -> 0f
                    Direction.DOWN -> 180f
                    else -> 90f
                }
            )
            .rotateYDegrees(pointerRotationOffset + pointerRotation)
            .uncenter()
            .light<SuperByteBuffer>(light)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }

    override fun getRenderedBlockState(be: BlimpPortBlockEntity?): BlockState? {
        return shaft(getRotationAxisOf(be))
    }
}
