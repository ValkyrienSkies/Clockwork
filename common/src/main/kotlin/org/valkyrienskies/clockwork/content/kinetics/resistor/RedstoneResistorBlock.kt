package org.valkyrienskies.clockwork.content.kinetics.resistor

import com.simibubi.create.content.kinetics.RotationPropagator
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.ticks.TickPriority
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class RedstoneResistorBlock(properties: Properties) :
    AbstractEncasedShaftBlock(properties),
    IBE<SplitShaftBlockEntity> {
    init {
        registerDefaultState(defaultBlockState().setValue(POWERED, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(POWERED)
        super.createBlockStateDefinition(builder)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        return super.getStateForPlacement(context)!!.setValue(
            POWERED,
            context.level.hasNeighborSignal(context.clickedPos)
        )
    }

    override fun neighborChanged(
        state: BlockState,
        worldIn: Level,
        pos: BlockPos,
        blockIn: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        if (worldIn.isClientSide) return
        val previouslyPowered = state.getValue(POWERED)
        if (previouslyPowered != worldIn.hasNeighborSignal(pos)) {
            detachKinetics(worldIn, pos, true)
            worldIn.setBlock(pos, state.cycle(POWERED), 2)
        }
    }

    override fun getBlockEntityClass(): Class<SplitShaftBlockEntity> {
        return SplitShaftBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SplitShaftBlockEntity> {
        return ClockworkBlockEntities.REDSTONE_RESISTOR.get()
    }

    fun detachKinetics(worldIn: Level, pos: BlockPos?, reAttachNextTick: Boolean) {
        val te = worldIn.getBlockEntity(pos)
        if (te == null || te !is KineticBlockEntity) return
        RotationPropagator.handleRemoved(worldIn, pos, te as KineticBlockEntity?)

        // Re-attach next tick
        if (reAttachNextTick) worldIn.scheduleTick(pos, this, 0, TickPriority.EXTREMELY_HIGH)
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        val te = level.getBlockEntity(pos)
        if (te == null || te !is KineticBlockEntity) return
        RotationPropagator.handleAdded(level, pos, te)
    }

    companion object {
        val POWERED = BlockStateProperties.POWERED
    }
}
