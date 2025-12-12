package org.valkyrienskies.clockwork.util.render

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.ClockworkRenderTypes

object RenderUtil {

    val CRYSTAL_MATRIX = ClockworkMod.asResource("textures/block/empty.png");
    val PURPLE_HUE = ClockworkMod.asResource("textures/block/purple_hue.png")

    /**
     * Renders three cubes making up the Core with the PartialItemModelRenderer. Adds offset to make up for natural model transformation pivot
     * @param innerData Data for inner cube offset and rotation
     * @param data Data for middle and outer cube's offset and rotation
     */
    fun renderCubeMatrix(matrices: PoseStack, renderer: PartialItemModelRenderer, innerData: TransformData, data: TransformData, scale: Float, light: Int) {
        var modelOffset = org.joml.Vector3f(0f, -4.5f / 16.0f, 0f)

        renderAndTransform(matrices, ClockworkPartials.CRYSTAL_INNER, RenderType.endPortal(), renderer, modelOffset, innerData.offset, innerData.rotation, scale, light)
        renderAndTransform(matrices, ClockworkPartials.CRYSTAL, ClockworkRenderTypes.CRYSTAL.apply(CRYSTAL_MATRIX), renderer, modelOffset, data.offset, data.rotation, scale, light)
        renderAndTransform(matrices, ClockworkPartials.CRYSTAL_OUTER, RenderType.entityTranslucent(PURPLE_HUE), renderer, modelOffset, data.offset, data.rotation, scale, light)
    }

    fun renderCube(matrices: PoseStack, renderer: PartialItemModelRenderer, data: TransformData, scale: Float, light: Int) {
        var modelOffset = org.joml.Vector3f(0f, -4.5f / 16.0f, 0f)
        renderAndTransform(matrices, ClockworkPartials.CRYSTAL, ClockworkRenderTypes.CRYSTAL.apply(CRYSTAL_MATRIX), renderer, modelOffset, data.offset, data.rotation, scale, light)
        renderAndTransform(matrices, ClockworkPartials.CRYSTAL_OUTER, RenderType.entityTranslucent(PURPLE_HUE), renderer, modelOffset, data.offset, data.rotation, scale, light)
    }

    /**
     * Helper function for
     * @see RenderUtil.renderCubeMatrix
     * Transforms and renders a model
     */
    fun renderAndTransform(matrices: PoseStack, model: PartialModel, renderType: RenderType, renderer: PartialItemModelRenderer, modelCorrection: org.joml.Vector3f, offset: org.joml.Vector3f, rotationVec: org.joml.Vector3f, scale : Float, light: Int) {
        matrices.pushPose()
        matrices.translate(offset.x().toDouble(), offset.y().toDouble(), offset.z().toDouble())
        matrices.translate(0.25,0.25,0.25)
        matrices.pushPose()
        //Scale
        //val scale = 1.5f
        matrices.scale(scale, scale, scale)
        matrices.translate(-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)))

        matrices.translate(-modelCorrection.x().toDouble(), -modelCorrection.y().toDouble(), -modelCorrection.z().toDouble())
        Quaternionf(AxisAngle4f(AngleHelper.rad(rotationVec.y().toDouble()), 0f, 1f, 0f))
        Quaternionf(AxisAngle4f(AngleHelper.rad(rotationVec.x().toDouble()), 1f, 0f, 0f))
        Quaternionf(AxisAngle4f(AngleHelper.rad(rotationVec.z().toDouble()), 0f, 0f, 1f))
        matrices.translate(modelCorrection.x().toDouble(), modelCorrection.y().toDouble(), modelCorrection.z().toDouble())
        renderer.render(model.get(), renderType, light)


        matrices.popPose()
        matrices.popPose()
    }

    /**
     * Renders three cubes making up the Core with the CachedBuffers using a BlockState.
     * @param innerData Data for inner cube offset and rotation
     * @param data Data for middle cube offset and rotation
     * @param outerData Data for outer cube offset and rotation
     */
    fun renderCubeMatrix(matrices: PoseStack, buffer: MultiBufferSource, blockState: BlockState, innerData: TransformData, data: TransformData, outerData: TransformData, scale: Float, light: Int, overlay: Int){
        val crystal_inner_buffer = buffer.getBuffer(RenderType.endPortal())
        val crystal_inner = CachedBuffers.partial(ClockworkPartials.CRYSTAL_INNER, blockState)
        renderAndTransform(crystal_inner, scale, innerData.offset, innerData.rotation).light<SuperByteBuffer>(light).color<SuperByteBuffer>(255,255,255, 255).overlay<SuperByteBuffer>(overlay).disableDiffuse<SuperByteBuffer>().renderInto(matrices, crystal_inner_buffer)

        val crystal_buffer = buffer.getBuffer(ClockworkRenderTypes.CRYSTAL.apply(CRYSTAL_MATRIX))
        val crystal = CachedBuffers.partial(ClockworkPartials.CRYSTAL, blockState)
        renderAndTransform(crystal, scale, data.offset, data.rotation).light<SuperByteBuffer>(light).color<SuperByteBuffer>(255,255,255, 255).overlay<SuperByteBuffer>(overlay).disableDiffuse<SuperByteBuffer>().renderInto(matrices, crystal_buffer)

        val crystal_outer_buffer = buffer.getBuffer(RenderType.entityTranslucent(PURPLE_HUE))
        val crystal_outer = CachedBuffers.partial(ClockworkPartials.CRYSTAL_OUTER, blockState)
        renderAndTransform(crystal_outer, scale, outerData.offset, outerData.rotation).light<SuperByteBuffer>(light).color<SuperByteBuffer>(255,255,255, 255).overlay<SuperByteBuffer>(overlay).renderInto(matrices, crystal_outer_buffer)
    }

    /**
     * Helper function for
     * @see RenderUtil.renderCubeMatrix
     * Transforms and renders a model
     */
    private fun renderAndTransform(buffer: SuperByteBuffer, scale: Float, coreOffset: org.joml.Vector3f, coreRotation: org.joml.Vector3f): SuperByteBuffer {
        //Scale
        buffer.scale(scale)
        buffer.translate(-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)))
        //Y
        buffer.translateY((coreOffset.y * 2)).rotateCentered((coreRotation.y / 180 * Math.PI).toFloat(), Direction.UP)
        //Z
        buffer.translateY((coreOffset.z * 2)).rotateCentered((coreRotation.z / 180 * Math.PI).toFloat(), Direction.NORTH)
        //X
        buffer.translateY((coreOffset.x * 2)).rotateCentered((coreRotation.x / 180 * Math.PI).toFloat(), Direction.EAST)

        buffer.translateY((-(4.5 / 16.0)).toFloat())
        return buffer
    }
}
