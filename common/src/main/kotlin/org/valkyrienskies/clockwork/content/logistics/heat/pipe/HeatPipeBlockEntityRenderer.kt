package org.valkyrienskies.clockwork.content.logistics.heat.pipe

import com.jozufozu.flywheel.core.PartialModel
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Vector4f
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.ClockworkShaders

class HeatPipeBlockEntityRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<HeatPipeBlockEntity>(context) {

    @OptIn(ExperimentalStdlibApi::class)
    override fun renderSafe(blockEntity: HeatPipeBlockEntity?,
                            partialTicks: Float,
                            ms: PoseStack?,
                            buffer: MultiBufferSource?,
                            light: Int,
                            overlay: Int) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)

        if (blockEntity != null) {
            buffer?.getBuffer(RenderType.cutout())//Reset buffer because :KEKW:
            val pipe : SuperByteBuffer = CachedBufferer.partial(ClockworkPartials.DUCT_CORE, blockEntity.blockState)
            for (direction in Direction.entries) {
                buffer?.getBuffer(RenderType.cutout())//Reset buffer because :KEKW:
                if (blockEntity.canTransferHeat(direction)) {
                    // pipe connection
                    val connection = getPipeModel(direction, false)

                    val connectionBuffer = CachedBufferer.partial(connection, blockEntity.blockState)

                    connectionBuffer.color(255,255,255,255).light(light).overlay().renderInto(ms, buffer?.getBuffer(ClockworkRenderTypes.HEAT))
                    buffer?.getBuffer(RenderType.cutout())//Reset buffer because :KEKW:
                    // if anything but a pipe, add vent cover
                    if (!blockEntity.isNeighborPipe(direction)) {
                        val rim = getPipeModel(direction, true)
                        val rimBuffer = CachedBufferer.partial(rim, blockEntity.blockState)

                        rimBuffer.color(255,255,255,255).light(light).overlay().renderInto(ms, buffer?.getBuffer(ClockworkRenderTypes.HEAT))
                        buffer?.getBuffer(RenderType.cutout())//Reset buffer because :KEKW:
                    }
                }
            }
            val shader = ClockworkShaders.heat()
            val intensity = AnimationTickHolder.getTicks() / 1f % 100f //TODO here is the test code for the heat shader intensity, reimplement however you want
            shader?.safeGetUniform("Intensity")?.set(intensity)//from 0 to 100 % intensity, shader handel color now :sungalges:

            pipe.color(255,255,255,255).light(light).overlay().renderInto(ms, buffer?.getBuffer(ClockworkRenderTypes.HEAT))
            buffer?.getBuffer(RenderType.cutout())//Reset buffer because :KEKW:
        }


        buffer?.getBuffer(RenderType.cutout())//Reset buffer because :KEKW:
    }

    fun getPipeModel(direction: Direction, rim: Boolean): PartialModel {
        if (rim) {
            return when (direction) {
                Direction.UP -> ClockworkPartials.DUCT_RIM_UP
                Direction.DOWN -> ClockworkPartials.DUCT_RIM_DOWN
                Direction.NORTH -> ClockworkPartials.DUCT_RIM_NORTH
                Direction.SOUTH -> ClockworkPartials.DUCT_RIM_SOUTH
                Direction.EAST -> ClockworkPartials.DUCT_RIM_EAST
                Direction.WEST -> ClockworkPartials.DUCT_RIM_WEST
            }
        } else {
            return when (direction) {
                Direction.UP -> ClockworkPartials.DUCT_CONN_UP
                Direction.DOWN -> ClockworkPartials.DUCT_CONN_DOWN
                Direction.NORTH -> ClockworkPartials.DUCT_CONN_NORTH
                Direction.SOUTH -> ClockworkPartials.DUCT_CONN_SOUTH
                Direction.EAST -> ClockworkPartials.DUCT_CONN_EAST
                Direction.WEST -> ClockworkPartials.DUCT_CONN_WEST
            }
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