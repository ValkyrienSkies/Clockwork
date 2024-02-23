package org.valkyrienskies.clockwork.content.contraptions.smart_propeller

import com.jozufozu.flywheel.backend.Backend
import com.jozufozu.flywheel.core.PartialModel
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AngleHelper
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.mod.common.util.toMinecraft


class SmartPropellerBearingRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<SmartPropellerBearingBlockEntity>(context) {

    override fun renderSafe(blockEntity: SmartPropellerBearingBlockEntity,
                            partialTicks: Float,
                            ms: PoseStack,
                            buffer: MultiBufferSource,
                            light: Int,
                            overlay: Int) {
        if (Backend.canUseInstancing(blockEntity.level)) return

        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)

        val facing: Direction = blockEntity.blockState.getValue(BlockStateProperties.FACING)
        val normal = Vec3(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble())
        val tiltQuaternion: Quaternionf = blockEntity.tiltQuaternion
        val quaternionCopy = Quaternionf(tiltQuaternion)
        quaternionCopy.conjugate()
        quaternionCopy.mul(Quaternionf(normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat(), 0f))
        quaternionCopy.mul(tiltQuaternion)

        val piston_nw: PartialModel = ClockworkPartials.SMART_PROP_PISTON_NW
        val piston_ne: PartialModel = ClockworkPartials.SMART_PROP_PISTON_NE
        val piston_sw: PartialModel = ClockworkPartials.SMART_PROP_PISTON_SW
        val piston_se: PartialModel = ClockworkPartials.SMART_PROP_PISTON_SE

        var superBuffer: SuperByteBuffer
        superBuffer = CachedBufferer.partial(piston_nw, blockEntity.blockState)
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))
        superBuffer = CachedBufferer.partial(piston_ne, blockEntity.blockState)
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))
        superBuffer = CachedBufferer.partial(piston_sw, blockEntity.blockState)
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))
        superBuffer = CachedBufferer.partial(piston_se, blockEntity.blockState)
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))

        val top: PartialModel = ClockworkPartials.SMART_PROP_TOP
        superBuffer = CachedBufferer.partial(top, blockEntity.blockState)

        superBuffer.translate(normal.scale(0.1))
        superBuffer.rotateCentered(tiltQuaternion.toMinecraft())
        superBuffer.translate(normal.scale(-0.1))

        val interpolatedAngle: Float = blockEntity.getInterpolatedAngle(partialTicks - 1)
        kineticRotationTransform(superBuffer, blockEntity, facing.axis, (interpolatedAngle / 180 * Math.PI).toFloat(), light)

        superBuffer.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }
}