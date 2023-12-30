package org.valkyrienskies.clockwork.content.logistics.heat.pipe

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import org.valkyrienskies.clockwork.ClockworkRenderTypes

class HeatPipeBlockEntityRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<HeatPipeBlockEntity>(context) {

    override fun renderSafe(blockEntity: HeatPipeBlockEntity?,
                            partialTicks: Float,
                            ms: PoseStack?,
                            buffer: MultiBufferSource?,
                            light: Int,
                            overlay: Int) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)

        if (blockEntity != null) {

            val pipe : SuperByteBuffer = CachedBufferer.block(blockEntity.blockState)
            val scale = 1.1f
            pipe
                .scale(scale)
                .translate((1 / (scale.toDouble() * 4)),(1 / (scale.toDouble() * 4)),(1 / (scale.toDouble() * 4)))
                .light(light)
                .color(255,255,255, 100)
                .overlay()
                .renderInto(ms, buffer?.getBuffer(ClockworkRenderTypes.HEAT))
            pipe.renderInto(ms, buffer?.getBuffer(RenderType.cutout()))
        }


    }
/* Dont need this but im sure as hell not deleting it
    private fun getModelFromState(blockState: BlockState) : MutableList<PartialModel> {
        var north = false
        var east = false
        var south = false
        var west = false
        var up = false
        var down = false
        var modelList = mutableListOf<PartialModel>()

        if (blockState.hasProperty(PipeBlock.NORTH)) {
            north = blockState.getValue(PipeBlock.NORTH)
        }
        if (blockState.hasProperty(PipeBlock.NORTH)) {
            east = blockState.getValue(PipeBlock.NORTH)
        }
        if (blockState.hasProperty(PipeBlock.NORTH)) {
            south = blockState.getValue(PipeBlock.NORTH)
        }
        if (blockState.hasProperty(PipeBlock.NORTH)) {
            west = blockState.getValue(PipeBlock.NORTH)
        }
        if (blockState.hasProperty(PipeBlock.NORTH)) {
            up = blockState.getValue(PipeBlock.NORTH)
        }
        if (blockState.hasProperty(PipeBlock.NORTH)) {
            down = blockState.getValue(PipeBlock.NORTH)
        }


        if (!down && !north && south && up) {
            modelList.add(ClockworkPartials.CORE_LU_X)
        }
        if (!down && north && !south && up) {
            modelList.add(ClockworkPartials.CORE_RU_X)
        }
        if (down && !north && south && !up) {
            modelList.add(ClockworkPartials.CORE_LD_X)
        }
        if (down && north && !south && !up) {
            modelList.add(ClockworkPartials.CORE_RD_X)
        }
        if (down && !north && !south && up) {
            modelList.add(ClockworkPartials.CORE_UD_X)
        }
        if (!down && !north && !south && up) {
            modelList.add(ClockworkPartials.CORE_U_X)
        }
        if (down && !north && !south && !up) {
            modelList.add(ClockworkPartials.CORE_D_X)
        }
        if (!down && north && south && !up) {
            modelList.add(ClockworkPartials.CORE_LR_X)
        }
        if (!down && !north && south && !up) {
            modelList.add(ClockworkPartials.CORE_L_X)
        }
        if (!down && north && !south && !up) {
            modelList.add(ClockworkPartials.CORE_R_X)
        }
        if (east && !north && south && !west) {
            modelList.add(ClockworkPartials.CORE_LU_Y)
        }
        if (!east && !north && south && west) {
            modelList.add(ClockworkPartials.CORE_RU_Y)
        }
        if (east && north && !south && !west) {
            modelList.add(ClockworkPartials.CORE_LD_Y)
        }
        if (!east && north && !south && west) {
            modelList.add(ClockworkPartials.CORE_RD_Y)
        }
        if (!east && north && south && !west) {
            modelList.add(ClockworkPartials.CORE_UD_Y)
        }
        if (!east && !north && south && !west) {
            modelList.add(ClockworkPartials.CORE_U_Y)
        }
        if (!east && north && !south && !west) {
            modelList.add(ClockworkPartials.CORE_D_Y)
        }
        if (east && !north && !south && west) {
            modelList.add(ClockworkPartials.CORE_LR_Y)
        }
        if (east && !north && !south && !west) {
            modelList.add(ClockworkPartials.CORE_L_Y)
        }
        if (!east && !north && !south && west) {
            modelList.add(ClockworkPartials.CORE_R_Y)
        }
        if (!down && east && up && !west) {
            modelList.add(ClockworkPartials.CORE_LU_Z)
        }
        if (!down && !east && up && west) {
            modelList.add(ClockworkPartials.CORE_RU_Z)
        }
        if (down && east && !up && !west) {
            modelList.add(ClockworkPartials.CORE_LD_Z)
        }
        if (down && !east && !up && west) {
            modelList.add(ClockworkPartials.CORE_RD_Z)
        }
        if (down && !east && up && !west) {
            modelList.add(ClockworkPartials.CORE_UD_Z)
        }
        if (!down && !east && up && !west) {
            modelList.add(ClockworkPartials.CORE_U_Z)
        }
        if (down && !east && !up && !west) {
            modelList.add(ClockworkPartials.CORE_D_Z)
        }
        if (!down && east && !up && west) {
            modelList.add(ClockworkPartials.CORE_LR_Z)
        }
        if (!down && east && !up && !west) {
            modelList.add(ClockworkPartials.CORE_L_Z)
        }
        if (!down && !east && !up && west) {
            modelList.add(ClockworkPartials.CORE_R_Z)
        }

        return modelList
    }

 */
}