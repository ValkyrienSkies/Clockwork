package org.valkyrienskies.clockwork.content.contraptions.phys.speed_gauge

import com.simibubi.create.content.kinetics.base.KineticBlock
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.gui.ScreenOpener
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.phys.BlockHitResult

class SpeedGaugeBlock(properties: Properties?): BaseEntityBlock(properties), IBE<SpeedGaugeBlockEntity> {

    override fun getBlockEntityClass(): Class<SpeedGaugeBlockEntity> {
        return SpeedGaugeBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SpeedGaugeBlockEntity> {
        return ClockworkBlockEntities.SPEED_GAUGE.get()
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!player.isShiftKeyDown) {
            if (level.isClientSide) {
                withBlockEntityDo(level, pos) { te: SpeedGaugeBlockEntity ->
                    displayScreen(te, player)
                }
            }
        }
        return InteractionResult.SUCCESS
    }

    @Environment(value = EnvType.CLIENT)
    private fun displayScreen(te: SpeedGaugeBlockEntity, player: Player) {
        if (player is LocalPlayer) ScreenOpener.open(SpeedGaugeScreen(te))
    }

    init {
        this.registerDefaultState(
            ((stateDefinition.any() as BlockState).setValue(
                HorizontalDirectionalBlock.FACING,
                Direction.NORTH
            ) as BlockState)
        )
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(
            HorizontalDirectionalBlock.FACING,
            context.horizontalDirection.opposite
        ) as BlockState
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(
            HorizontalDirectionalBlock.FACING,
            rotation.rotate(state.getValue(HorizontalDirectionalBlock.FACING) as Direction)
        ) as BlockState
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.rotate(mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING) as Direction))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(
            *arrayOf<Property<*>>(
                HorizontalDirectionalBlock.FACING,
                POWERED
            )
        )
    }


    @OptIn(ExperimentalStdlibApi::class)
    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        for (direction in Direction.entries) {
            level.updateNeighborsAt(pos.relative(direction), this)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (isMoving) {
            return
        }
        for (direction in Direction.entries) {
            level.updateNeighborsAt(pos.relative(direction), this)
        }
    }

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        return if (state.getValue(POWERED)) {
            15
        } else 0
    }

    override fun isSignalSource(state: BlockState): Boolean {
        return true
    }

    companion object {
        val POWERED: BooleanProperty = BlockStateProperties.POWERED
    }

}