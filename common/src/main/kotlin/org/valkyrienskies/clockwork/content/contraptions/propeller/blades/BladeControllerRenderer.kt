package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.contraptions.render.ContraptionMatrices
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
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


        val blockState = blockEntity.blockState

        val bladeRotations = blockEntity.clientBladeRotation.map { it.value.getValue(partialTicks) }

        renderShared(blades, bladeAngle, blockState, partialTicks, ms, buffer, bladeRotations)
    }

    companion object {
        fun renderShared(blades: List<ItemStack>, bladeAngle: Float, blockState: BlockState, partialTicks: Float, ms: PoseStack, buffer: MultiBufferSource, bladeRotations: List<Float>, contraption: Boolean = false, contraptionMatrices: ContraptionMatrices? = null) {
            val renderBuffer = buffer.getBuffer(RenderType.cutout())

            val facing = blockState.getValue(BlockStateProperties.FACING)
            val msr = TransformStack.of(ms)

            //ms.pushPose()
            //msr.rotateCentered(Direction.UP, Math.toRadians(contraptionAngle.toDouble()).toFloat())
            for (i in blades.indices) {
                val bladeRotation = if (bladeRotations.size - 1 >= i) bladeRotations[i] else continue
                val bladeLength = blades[i].tag?.getDouble("BladeLength")?.toFloat() ?: 1f

                val wide = blades[i].`is`(ClockworkItems.WIDE_PROPELLER_BLADE.get())

                val bladeBasePartial = if (wide) ClockworkPartials.WIDEBLADE_BASE else ClockworkPartials.BLADE_BASE
                val bladeExtensionPartial = if (wide) ClockworkPartials.WIDEBLADE_EXTENSION else ClockworkPartials.BLADE_EXTENSION
                val bladeTipPartial = if (wide) ClockworkPartials.WIDEBLADE_TIP else ClockworkPartials.BLADE_TIP

                val bladeBase = CachedBuffers.partial(bladeBasePartial, blockState)
                val bladeExtension = CachedBuffers.partial(bladeExtensionPartial, blockState)
                val bladeTip = CachedBuffers.partial(bladeTipPartial, blockState)
                if (contraptionMatrices != null) {
                    bladeBase.transform(contraptionMatrices.model)
                    bladeExtension.transform(contraptionMatrices.model)
                    bladeTip.transform(contraptionMatrices.model)
                }

                renderBlade(bladeBase, bladeExtension, bladeTip, bladeAngle, bladeLength, bladeRotation, ms, renderBuffer, facing, contraption)
            }
            //ms.popPose()
        }

        fun rotateByPivot(buffer: SuperByteBuffer, pivot: Vec3, rotation: Double) {
            buffer.translate(pivot)
            buffer.rotateZDegrees(rotation.toFloat())
            buffer.translateBack(pivot)
        }

        fun renderBlade(bladeBase: SuperByteBuffer, bladeExtension: SuperByteBuffer, bladeTip: SuperByteBuffer, bladeAngle: Float, bladeLength: Float, bladeRotation: Float, ms: PoseStack, buffer: VertexConsumer, facing: Direction, contraption: Boolean) {
            // Render the blade here
            ms.pushPose()
            ms.translate(0.0, 0.0, 0.0)

            val pivot = Vec3(0.5,0.5,0.5)





            ms.popPose()
            ms.pushPose()

            if (facing.axis.isHorizontal) {
                bladeBase.rotateCentered(
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble()),
                    Direction.UP
                )
                bladeExtension.rotateCentered(
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble()),
                    Direction.UP
                )
                bladeTip.rotateCentered(
                    AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble()),
                    Direction.UP
                )
            }
            bladeBase.rotateCentered(
                AngleHelper.rad((-90.0 - AngleHelper.verticalAngle(facing)).toDouble()),
                Direction.EAST
            )
            bladeExtension.rotateCentered(
                AngleHelper.rad((-90.0 - AngleHelper.verticalAngle(facing)).toDouble()),
                Direction.EAST
            )
            bladeTip.rotateCentered(
                AngleHelper.rad((-90.0 - AngleHelper.verticalAngle(facing)).toDouble()),
                Direction.EAST
            )

            bladeBase.rotateCentered(Math.toRadians(bladeRotation.toDouble()).toFloat(), Direction.UP)
            bladeExtension.rotateCentered(Math.toRadians(bladeRotation.toDouble()).toFloat(), Direction.UP)
            bladeTip.rotateCentered(Math.toRadians(bladeRotation.toDouble()).toFloat(), Direction.UP)

            rotateByPivot(bladeBase, pivot, bladeAngle.toDouble())
            rotateByPivot(bladeExtension, pivot, bladeAngle.toDouble())
            rotateByPivot(bladeTip, pivot, bladeAngle.toDouble())

            ms.popPose()
            ms.pushPose()

            bladeExtension.translate(0.0,0.0,(bladeLength-1)*0.3)
            bladeTip.translate(0.0,0.0,(bladeLength-1)*0.5)

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
