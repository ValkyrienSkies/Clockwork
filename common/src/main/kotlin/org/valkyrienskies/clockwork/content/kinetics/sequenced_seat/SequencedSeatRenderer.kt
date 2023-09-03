package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkPartials

class SequencedSeatRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<SequencedSeatBlockEntity>(context) {
    private var te: SequencedSeatBlockEntity? = null
    protected override fun renderSafe(
        te: SequencedSeatBlockEntity, partialTicks: Float, ms: PoseStack, buffer: MultiBufferSource,
        light: Int, overlay: Int
    ) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay)
        this.te = te
        val seat = this.te
        val facing = te.blockState
            .getValue(BlockStateProperties.HORIZONTAL_FACING)
        val joystick = CachedBufferer.partialFacing(ClockworkPartials.JOYSTICK, te.blockState, facing.opposite)
        val buttonone = CachedBufferer.partialFacing(ClockworkPartials.BUTTON_ONE, te.blockState, facing.opposite)
        val buttontwo = CachedBufferer.partialFacing(ClockworkPartials.BUTTON_TWO, te.blockState, facing.opposite)

//        superBuffer.rotateCentered(Direction.EAST, AngleHelper.rad(-90 - AngleHelper.verticalAngle(facing)));
//        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        joystick.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()))
        buttonone.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()))
        buttontwo.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }
}