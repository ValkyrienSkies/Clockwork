package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AngleHelper
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkPartials

class BladeControllerRenderer(context: BlockEntityRendererProvider.Context?) : SmartBlockEntityRenderer<BladeControllerBlockEntity>(
    context
) {
    override fun renderSafe(
        blockEntity: BladeControllerBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)

        val blades = blockEntity.blades
        val bladeAngle = blockEntity.clientBladeAngle.value
        val bladeLength = blockEntity.clientBladeLength.value

        val blockState = blockEntity.blockState

        val renderBuffer = buffer.getBuffer(RenderType.cutout())

        val facing = blockState.getValue(BlockStateProperties.FACING)

        for (i in blades.indices) {
            val bladeRotation = blockEntity.clientBladeRotation[i]!!.value

            val wide = blades[i].`is`(ClockworkItems.WIDE_PROPELLER_BLADE.get())

            val bladeBasePartial = if (wide) ClockworkPartials.WIDEBLADE_BASE else ClockworkPartials.BLADE_BASE
            val bladeExtensionPartial = if (wide) ClockworkPartials.WIDEBLADE_EXTENSION else ClockworkPartials.BLADE_EXTENSION
            val bladeTipPartial = if (wide) ClockworkPartials.WIDEBLADE_TIP else ClockworkPartials.BLADE_TIP

            val bladeBase = CachedBufferer.partial(bladeBasePartial, blockState)
            val bladeExtension = CachedBufferer.partial(bladeExtensionPartial, blockState)
            val bladeTip = CachedBufferer.partial(bladeTipPartial, blockState)

            renderBlade(bladeBase, bladeExtension, bladeTip, bladeAngle, bladeLength, bladeRotation, ms, light, overlay, renderBuffer, facing)
        }
    }

    fun renderBlade(bladeBase: SuperByteBuffer, bladeExtension: SuperByteBuffer, bladeTip: SuperByteBuffer, bladeAngle: Float, bladeLength: Float, bladeRotation: Float, ms: PoseStack, light: Int, overlay: Int, buffer: VertexConsumer, facing: Direction) {
        // Render the blade here
        ms.pushPose()
        ms.translate(0.0, 0.0, 0.25)

        bladeBase.rotateZ(bladeAngle.toDouble())
        bladeExtension.rotateZ(bladeAngle.toDouble())
        bladeTip.rotateZ(bladeAngle.toDouble())

        bladeExtension.scale(1.0f, bladeLength, 1.0f)
        bladeTip.translate(0.0, bladeLength.toDouble(), 0.0)
        ms.popPose()
        ms.pushPose()
        bladeBase.rotateY(bladeRotation.toDouble())
        bladeExtension.rotateY(bladeRotation.toDouble())
        bladeTip.rotateY(bladeRotation.toDouble())

        ms.popPose()
        ms.pushPose()

        if (facing.axis.isHorizontal) {
            bladeBase.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
            bladeExtension.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
            bladeTip.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
        }
        bladeBase.rotateCentered(
            Direction.EAST,
            AngleHelper.rad((-90.0 - AngleHelper.verticalAngle(facing)).toDouble())
        )
        bladeExtension.rotateCentered(
            Direction.EAST,
            AngleHelper.rad((-90.0 - AngleHelper.verticalAngle(facing)).toDouble())
        )
        bladeTip.rotateCentered(
            Direction.EAST,
            AngleHelper.rad((-90.0 - AngleHelper.verticalAngle(facing)).toDouble())
        )

        bladeBase.light(light).renderInto(ms, buffer)
        bladeExtension.light(light).renderInto(ms, buffer)
        bladeTip.light(light).renderInto(ms, buffer)
        ms.popPose()
    }
}