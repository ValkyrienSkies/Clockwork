package org.valkyrienskies.clockwork.content.logistics.gas.thermal

import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.foundation.block.IBE
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode

class HeatExchangerBlock(properties: Properties) :
    RotatedPillarBlock(properties), IBE<HeatExchangerBlockEntity>, IDuct, IHaveDuctStats, IWrenchable {

    // Bake collision/selection shapes once, rather than constructing them during world queries.
    private val shapes = Direction.Axis.entries.associateWith { axis ->
        fun box(min: Double, from: Double, max: Double, to: Double): VoxelShape = when (axis) {
            Direction.Axis.X -> Block.box(from, min, min, to, max, max)
            Direction.Axis.Y -> Block.box(min, from, min, max, to, max)
            Direction.Axis.Z -> Block.box(min, min, from, max, max, to)
        }
        Shapes.or(box(3.0, 0.0, 13.0, 16.0), box(2.0, 4.0, 14.0, 12.0),
            box(0.0, 6.0, 16.0, 10.0)).optimize()
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        shapes.getValue(state.getValue(AXIS))

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter) =
        direction.axis == level.getBlockState(self).getValue(AXIS)

    override fun getInternalVolume() = ClockworkConfig.SERVER.ductVolme

    override fun createNode(pos: DuctNodePos): DuctNode = PipeDuctNode(
        pos, volume = getInternalVolume(), maxPressure = getMaximumPressure(),
        maxTemperature = getMaximumTemperature(), heatCapacity = 44.9
    )

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
        if (level.isClientSide) return
        withBlockEntityDo(level, pos) { it.refreshConnections(level, pos) }
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighborBlock: Block,
                                 neighborPos: BlockPos, movedByPiston: Boolean) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
        if (level.isClientSide) return
        val direction = Direction.fromDelta(neighborPos.x - pos.x, neighborPos.y - pos.y, neighborPos.z - pos.z) ?: return
        withBlockEntityDo(level, pos) { it.updateConnection(level, pos, direction) }
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos) { be ->
                val kelvin = ClockworkMod.getKelvin(level)
                val nodePos = be.getDuctNodePosition()
                if (!state.`is`(newState.block)) {
                    kelvin.removeNode(nodePos)
                } else if (state.getValue(AXIS) != newState.getValue(AXIS)) {
                    // Keep stored gas/heat on rotation, but discard edges through the old ports.
                    kelvin.getNodeAt(nodePos)?.getEdges()?.toList()?.forEach {
                        kelvin.removeEdge(it.nodeA, it.nodeB)
                    }
                    be.queueConnectionRefresh()
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getBlockEntityClass() = HeatExchangerBlockEntity::class.java
    override fun getBlockEntityType() = ClockworkBlockEntities.HEAT_EXCHANGER.get()
    override fun getAdditionalInfoLines() = listOf(
        Component.translatable("vs_clockwork.heat_exchanger.function").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
    )

    companion object {
        fun canExchangeHeat(axis: Direction.Axis, neighborAxis: Direction.Axis, direction: Direction) =
            axis == neighborAxis && direction.axis != axis
    }
}
