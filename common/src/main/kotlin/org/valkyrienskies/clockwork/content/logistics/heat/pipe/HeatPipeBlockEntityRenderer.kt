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
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.ClockworkShaders
import org.valkyrienskies.clockwork.util.render.RenderUtil

class HeatPipeBlockEntityRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<HeatPipeBlockEntity>(context) {

    override fun renderSafe(blockEntity: HeatPipeBlockEntity?,
                            partialTicks: Float,
                            ms: PoseStack?,
                            buffer: MultiBufferSource?,
                            light: Int,
                            overlay: Int) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)

        if (blockEntity != null) {
            renderPipe(blockEntity, ms, buffer, light)
        }

    }

    fun renderPipe(blockEntity: HeatPipeBlockEntity, ms: PoseStack?, buffer: MultiBufferSource?, light: Int){
        buffer?.getBuffer(RenderType.cutout())//Reset buffer because :KEKW:

        val shader = ClockworkShaders.heat()
        val d = blockEntity.temperature / blockEntity.getHeatLimit().toFloat()
        val intensity = d * 10f
        shader?.safeGetUniform("Intensity")?.set(intensity.toFloat())//from 0 to 100 % intensity, shader handel color now :sungalges:

        for (direction in Direction.values()) {
            buffer?.getBuffer(RenderType.cutout())//Reset buffer because :KEKW:
            if (blockEntity.canTransferHeat(direction)) {
                // pipe connection
                val connection = getPipeModel(direction, false)

                val connectionBuffer = CachedBufferer.partial(connection, blockEntity.blockState)

                actuallyRenderPipe(connectionBuffer, light, ms, buffer)
                // if anything but a pipe, add vent cover
                if (!blockEntity.isNeighborPipe(direction)) {
                    val rim = getPipeModel(direction, true)
                    val rimBuffer = CachedBufferer.partial(rim, blockEntity.blockState)

                    actuallyRenderPipe(rimBuffer, light, ms, buffer)
                }
            }
        }

        val pipe : SuperByteBuffer = CachedBufferer.partial(ClockworkPartials.DUCT_CORE, blockEntity.blockState)
        actuallyRenderPipe(pipe, light, ms, buffer)
    }

    fun actuallyRenderPipe(model: SuperByteBuffer, light: Int, ms: PoseStack?, buffer: MultiBufferSource?){

        //Real pipe
        model.color(255,255,255,255).light(light).overlay().renderInto(ms, buffer?.getBuffer(ClockworkRenderTypes.HEAT))

        buffer?.getBuffer(RenderType.cutout())
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
}
