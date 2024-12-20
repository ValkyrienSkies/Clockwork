package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.jozufozu.flywheel.util.transform.TransformStack
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.contraptions.render.ContraptionMatrices
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AngleHelper
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
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

        val blades = blockEntity.getAllBlades()
        val bladeAngle = blockEntity.clientBladeAngle.getValue(partialTicks)
        val bladeLength = blockEntity.clientBladeLength.getValue(partialTicks)

        val blockState = blockEntity.blockState

        val bladeRotations = blockEntity.clientBladeRotation.map { it.value.getValue(partialTicks) }

        renderShared(blades, bladeAngle, bladeLength, blockState, partialTicks, ms, buffer, bladeRotations)
    }

    companion object {
        fun renderShared(blades: List<ItemStack>, bladeAngle: Float, bladeLength: Float, blockState: BlockState, partialTicks: Float, ms: PoseStack, buffer: MultiBufferSource, bladeRotations: List<Float>, contraption: Boolean = false, contraptionMatrices: ContraptionMatrices? = null) {
            val renderBuffer = buffer.getBuffer(RenderType.cutout())

            val facing = blockState.getValue(BlockStateProperties.FACING)
            val msr = TransformStack.cast(ms)

            //ms.pushPose()
            //msr.rotateCentered(Direction.UP, Math.toRadians(contraptionAngle.toDouble()).toFloat())
            for (i in blades.indices) {
                val bladeRotation = if (bladeRotations.size - 1 >= i) bladeRotations[i] else continue

                val wide = blades[i].`is`(ClockworkItems.WIDE_PROPELLER_BLADE.get())

                val bladeBasePartial = if (wide) ClockworkPartials.WIDEBLADE_BASE else ClockworkPartials.BLADE_BASE
                val bladeExtensionPartial = if (wide) ClockworkPartials.WIDEBLADE_EXTENSION else ClockworkPartials.BLADE_EXTENSION
                val bladeTipPartial = if (wide) ClockworkPartials.WIDEBLADE_TIP else ClockworkPartials.BLADE_TIP

                val bladeBase = CachedBufferer.partial(bladeBasePartial, blockState)
                val bladeExtension = CachedBufferer.partial(bladeExtensionPartial, blockState)
                val bladeTip = CachedBufferer.partial(bladeTipPartial, blockState)
                if (contraptionMatrices != null) {
                    bladeBase.transform(contraptionMatrices.model)
                    bladeExtension.transform(contraptionMatrices.model)
                    bladeTip.transform(contraptionMatrices.model)
                }

                renderBlade(bladeBase, bladeExtension, bladeTip, bladeAngle, bladeLength, bladeRotation, ms, renderBuffer, facing, contraption)
            }
            //ms.popPose()
        }

        fun renderBlade(bladeBase: SuperByteBuffer, bladeExtension: SuperByteBuffer, bladeTip: SuperByteBuffer, bladeAngle: Float, bladeLength: Float, bladeRotation: Float, ms: PoseStack, buffer: VertexConsumer, facing: Direction, contraption: Boolean) {
            // Render the blade here
            ms.pushPose()
            ms.translate(0.0, 0.0, 0.0)

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

            ms.popPose()
            ms.pushPose()

            bladeBase.rotateZ(bladeAngle.toDouble())
            bladeExtension.rotateZ(bladeAngle.toDouble())
            bladeTip.rotateZ(bladeAngle.toDouble())
            bladeBase.rotateCentered(Direction.UP, Math.toRadians(bladeRotation.toDouble()).toFloat())
            bladeExtension.rotateCentered(Direction.UP, Math.toRadians(bladeRotation.toDouble()).toFloat())
            bladeTip.rotateCentered(Direction.UP, Math.toRadians(bladeRotation.toDouble()).toFloat())

            ms.popPose()
            ms.pushPose()

            bladeExtension.scale(1.0f, 1.0f, bladeLength)
            bladeTip.translate(0.0, 0.0, -bladeLength.toDouble() + 1.0)

            ms.popPose()
            ms.pushPose()

            bladeBase.translate(0.0, 0.0, -0.3)
            bladeExtension.translate(0.0, 0.0, -0.3)
            bladeTip.translate(0.0, 0.0, -0.3)

            ms.popPose()
            ms.pushPose()

            bladeBase.renderInto(ms, buffer)
            bladeExtension.renderInto(ms, buffer)
            bladeTip.renderInto(ms, buffer)
            ms.popPose()
        }
    }
}