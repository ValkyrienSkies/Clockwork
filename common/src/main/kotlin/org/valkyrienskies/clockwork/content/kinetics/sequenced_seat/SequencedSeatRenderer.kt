package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties

class SequencedSeatRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<SequencedSeatBlockEntity>(context) {
    private var te: SequencedSeatBlockEntity? = null
    override fun renderSafe(
        te: SequencedSeatBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay)
        this.te = te
        val seat = this.te
        val facing = te.blockState
            .getValue(BlockStateProperties.HORIZONTAL_FACING)

        renderRotatingBuffer(te, getRotatedModel(te, te.blockState), ms,
            buffer.getBuffer(RenderType.solid()), light)

        val time = AnimationTickHolder.getRenderTime(te.level!!)
        for (hDir in Direction.Plane.HORIZONTAL) {

            val shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, te.blockState, hDir)
            /*
            val speedMod = te.getRotationSpeedModifier(hDir)
            var angle = 0.0f
            if (te.getSpeed() != 0f && te.hasSource()) {
                angle = (time * speedMod * 3f / 20) % 360
            }

            val axis: Direction.Axis = hDir.axis

            kineticRotationTransform(shaft, te, axis, angle, light)

             */
            shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()))
        }
    }

    override fun getRotatedModel(te: SequencedSeatBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBuffers.partialFacing(
            AllPartialModels.SHAFT_HALF, state, Direction.DOWN
        )
    }
}
