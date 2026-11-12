package org.valkyrienskies.clockwork.content.kinetics.combobulator

import com.simibubi.create.foundation.block.IBE
import net.createmod.catnip.gui.ScreenOpener
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class CombobulatorBlock(properties: Properties) : HorizontalDirectionalBlock(properties), IBE<CombobulatorBlockEntity> {
    init {
        var state = defaultBlockState().setValue(FACING, Direction.NORTH)
        CONNECTIONS.values.forEach { state = state.setValue(it, false) }
        registerDefaultState(state)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
        CONNECTIONS.values.forEach { builder.add(it) }
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        var state = defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
        CONNECTIONS.forEach { (direction, property) ->
            state = state.setValue(property, connects(context.level.getBlockState(context.clickedPos.relative(direction))))
        }
        return state
    }

    override fun updateShape(state: BlockState, direction: Direction, neighbor: BlockState, level: LevelAccessor,
                             pos: BlockPos, neighborPos: BlockPos): BlockState =
        state.setValue(CONNECTIONS.getValue(direction), connects(neighbor))

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, moved: Boolean) {
        super.onPlace(state, level, pos, oldState, moved)
        if (oldState.block === this || level.isClientSide) return
        var connected = state
        CONNECTIONS.forEach { (direction, property) ->
            connected = connected.setValue(property, connects(level.getBlockState(pos.relative(direction))))
        }
        if (connected != state) level.setBlock(pos, connected, UPDATE_CLIENTS)
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        var result = state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
        CONNECTIONS.forEach { (direction, property) ->
            result = result.setValue(CONNECTIONS.getValue(rotation.rotate(direction)), state.getValue(property))
        }
        return result
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        var result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
        CONNECTIONS.forEach { (direction, property) ->
            result = result.setValue(CONNECTIONS.getValue(mirror.mirror(direction)), state.getValue(property))
        }
        return result
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult,
    ): InteractionResult {
        if (player.getItemInHand(hand).item is ThinkableItem) return InteractionResult.PASS
        if (!player.mayBuild()) return InteractionResult.PASS
        if (level.isClientSide) withBlockEntityDo(level, pos, ::openScreen)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    @Environment(EnvType.CLIENT)
    private fun openScreen(blockEntity: CombobulatorBlockEntity) {
        ScreenOpener.open(CombobulatorScreen(blockEntity))
    }

    override fun isSignalSource(state: BlockState): Boolean = true

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int =
        (level.getBlockEntity(pos) as? CombobulatorBlockEntity)
            ?.facePower(direction.opposite, state.getValue(FACING)) ?: 0

    override fun getDirectSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int =
        getSignal(state, level, pos, direction)

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moved: Boolean) {
        val powered = if (state.block !== newState.block && !level.isClientSide) {
            Direction.entries.filter { getSignal(state, level, pos, it.opposite) > 0 }
        } else emptyList()
        IBE.onRemove(state, level, pos, newState)
        super.onRemove(state, level, pos, newState, moved)
        for (face in powered) level.updateNeighborsAt(pos.relative(face), this)
    }

    override fun getBlockEntityClass(): Class<CombobulatorBlockEntity> = CombobulatorBlockEntity::class.java

    override fun getBlockEntityType(): BlockEntityType<out CombobulatorBlockEntity> =
        ClockworkBlockEntities.COMBOBULATOR.get()

    companion object {
        val CONNECTIONS = Direction.entries.associateWith { BooleanProperty.create(it.serializedName) }
        fun connects(state: BlockState) = state.block is CombobulatorBlock || state.block is ThinkableRelayBlock
    }
}
