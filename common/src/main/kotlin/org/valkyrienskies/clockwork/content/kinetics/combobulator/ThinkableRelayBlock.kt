package org.valkyrienskies.clockwork.content.kinetics.combobulator

import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlock.Companion.CONNECTIONS
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlock.Companion.connects

class ThinkableRelayBlock(properties: Properties) : WrenchableDirectionalBlock(properties), IBE<ThinkableRelayBlockEntity> {
    init {
        var state = defaultBlockState().setValue(FACING, Direction.DOWN)
        CONNECTIONS.values.forEach { state = state.setValue(it, false) }
        registerDefaultState(state)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        CONNECTIONS.values.forEach { builder.add(it) }
    }

    private fun connected(state: BlockState, level: LevelAccessor, pos: BlockPos): BlockState {
        var result = state
        CONNECTIONS.forEach { (side, property) -> result = result.setValue(property, connects(level.getBlockState(pos.relative(side)))) }
        return result
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        connected(defaultBlockState().setValue(FACING, context.clickedFace.opposite), context.level, context.clickedPos)

    override fun updateShape(state: BlockState, direction: Direction, neighbor: BlockState, level: LevelAccessor,
                             pos: BlockPos, neighborPos: BlockPos): BlockState =
        state.setValue(CONNECTIONS.getValue(direction), connects(neighbor))

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, moved: Boolean) {
        super.onPlace(state, level, pos, oldState, moved)
        if (level.isClientSide) return
        if (oldState.block === this) {
            if (oldState.getValue(FACING) != state.getValue(FACING))
                withBlockEntityDo(level, pos) { it.clearOutputs(oldState.getValue(FACING)) }
            return
        }
        val next = connected(state, level, pos)
        if (next != state) level.setBlock(pos, next, UPDATE_CLIENTS)
    }

    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        if (!player.mayBuild()) return InteractionResult.PASS
        if (!context.level.isClientSide) {
            context.level.setBlockAndUpdate(context.clickedPos, state.setValue(FACING, context.clickedFace))
            player.displayClientMessage(Component.translatable("thinkable.vs_clockwork.relay.input_set",
                Component.translatable("thinkable.vs_clockwork.face.${context.clickedFace.name.lowercase()}")), true)
        }
        return InteractionResult.sidedSuccess(context.level.isClientSide)
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        var result = state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
        CONNECTIONS.forEach { (side, property) -> result = result.setValue(CONNECTIONS.getValue(rotation.rotate(side)), state.getValue(property)) }
        return result
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        var result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
        CONNECTIONS.forEach { (side, property) -> result = result.setValue(CONNECTIONS.getValue(mirror.mirror(side)), state.getValue(property)) }
        return result
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        if (state.block !== newState.block) withBlockEntityDo(level, pos) { it.clearOutputs() }
        IBE.onRemove(state, level, pos, newState)
        super.onRemove(state, level, pos, newState, moved)
    }

    override fun getBlockEntityClass(): Class<ThinkableRelayBlockEntity> = ThinkableRelayBlockEntity::class.java
    override fun getBlockEntityType(): BlockEntityType<out ThinkableRelayBlockEntity> = ClockworkBlockEntities.THINKABLE_RELAY.get()
}
