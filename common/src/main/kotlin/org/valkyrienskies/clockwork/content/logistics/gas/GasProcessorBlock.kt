package org.valkyrienskies.clockwork.content.logistics.gas

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctPlacementHelper
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.content.physicalities.steering.DrivingControlItem
import org.valkyrienskies.clockwork.content.physicalities.steering.DrivingControlLinks
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode

class GasProcessorBlock<T : KNodeBlockEntity>(
    properties: Properties,
    private val entityClass: Class<T>,
    private val hasSideInlets: Boolean = false,
    private val entityType: () -> BlockEntityType<out T>,
) : HorizontalDirectionalBlock(properties), IBE<T>, IDuct, IHaveDuctStats, MeteredGasInlet {
    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val preferred = DuctPlacementHelper.getPreferredHorizontalFacing(context)
        val facing = DuctPlacementHelper.resolveHorizontalFacing(
            preferredSide = preferred,
            horizontalDirection = context.horizontalDirection,
            sneaking = context.player?.isShiftKeyDown == true
        )
        return defaultBlockState().setValue(FACING, facing)
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        rotate(state, mirror.getRotation(state.getValue(FACING)))

    override fun getBlockEntityClass(): Class<T> = entityClass

    override fun getBlockEntityType(): BlockEntityType<out T> = entityType()

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean =
        level.getBlockState(self).getValue(FACING) == direction

    override fun isMeteredInlet(state: BlockState, side: Direction): Boolean {
        val outlet = state.getValue(FACING)
        return side == outlet.opposite || hasSideInlets && side.axis.isHorizontal && side.axis != outlet.axis
    }

    override fun getAdditionalInfoLines() =
        listOf(Component.translatable("${descriptionId.removePrefix("block.")}.function"))

    override fun getInternalVolume() = ClockworkConfig.SERVER.ducts.ductVolme

    override fun getMaximumPressure() = GasTransfer.MAX_PRESSURE

    override fun getMaximumTemperature() = GasTransfer.MAX_TEMPERATURE

    override fun createNode(pos: DuctNodePos) =
        PipeDuctNode(pos, volume = getInternalVolume(), maxPressure = getMaximumPressure(),
            maxTemperature = getMaximumTemperature(), heatCapacity = 44.9)

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
        refreshOutlet(level, pos, state)
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighbor: Block, neighborPos: BlockPos, moved: Boolean) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved)
        if (neighborPos == pos.relative(state.getValue(FACING))) refreshOutlet(level, pos, state)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (level is ServerLevel && state.block != newState.block && DrivingControlItem.throttleTarget(state.block))
            DrivingControlLinks.get(level).endpointRemoved(pos)
        nodeRemove(state, level, pos, newState, isMoving)
        IBE.onRemove(state, level, pos, newState)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    private fun refreshOutlet(level: Level, pos: BlockPos, state: BlockState) {
        if (!level.isClientSide) withBlockEntityDo(level, pos) {
            (it as IConnectable).updateConnection(level, pos, state.getValue(FACING))
        }
    }
}

interface MeteredGasInlet {
    fun isMeteredInlet(state: BlockState, side: Direction): Boolean
}
