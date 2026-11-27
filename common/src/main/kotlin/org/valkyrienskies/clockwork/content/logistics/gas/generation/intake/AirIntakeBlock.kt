package org.valkyrienskies.clockwork.content.logistics.gas.generation.intake

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctPipeNode
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctPlacementHelper
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.clockwork.util.gui.ProductionInfo
import org.valkyrienskies.clockwork.util.gui.ProductionMethod
import org.valkyrienskies.clockwork.util.gui.ProductionType
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNodePos

class AirIntakeBlock(properties: Properties) : DirectionalBlock(properties), IBE<AirIntakeBlockEntity>, IDuct, IHaveDuctStats {
    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val preferred = DuctPlacementHelper.getPreferredFacing(context)
        val facing = DuctPlacementHelper.resolveDirectionalFacing(
            preferredSide = preferred,
            nearestLookingDirection = context.nearestLookingDirection,
            sneaking = context.player?.isShiftKeyDown == true,
            portOnFacingOpposite = true
        )
        return defaultBlockState().setValue(FACING, facing)
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        rotate(state, mirror.getRotation(state.getValue(FACING)))

    override fun getBlockEntityClass() = AirIntakeBlockEntity::class.java

    override fun getBlockEntityType() = ClockworkBlockEntities.AIR_INTAKE.get()

    override fun getInternalVolume() = ClockworkConfig.SERVER.ducts.ductVolme

    override fun createNode(pos: DuctNodePos) =
        DuctPipeNode(pos, volume = getInternalVolume(), maxPressure = getMaximumPressure(), maxTemperature = getMaximumTemperature())

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean =
        direction == level.getBlockState(self).getValue(FACING).opposite

    override fun getAdditionalInfoLines() = listOf(Component.translatable("vs_clockwork.air_intake.function"))

    override fun getProductionStats() = mapOf(
        KelvinMod.asResourceLocation("air") to ProductionInfo(
            ProductionMethod.OTHER, ProductionType.CONDITIONAL, Component.translatable("vs_clockwork.air_intake.condition")
        )
    )

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
        updateOutlet(state, level, pos)
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighbor: Block, neighborPos: BlockPos, moved: Boolean) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved)
        if (neighborPos == pos.relative(state.getValue(FACING).opposite)) updateOutlet(state, level, pos)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        IBE.onRemove(state, level, pos, newState)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    private fun updateOutlet(state: BlockState, level: Level, pos: BlockPos) {
        if (!level.isClientSide) withBlockEntityDo(level, pos) {
            it.updateConnection(level, pos, state.getValue(FACING).opposite)
        }
    }
}
