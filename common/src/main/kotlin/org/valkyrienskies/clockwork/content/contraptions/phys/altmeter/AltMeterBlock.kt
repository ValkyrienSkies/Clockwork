package org.valkyrienskies.clockwork.content.contraptions.phys.altmeter

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
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class AltMeterBlock(properties: Properties) : Block(properties), IBE<AltMeterBlockEntity> {
    init {
        registerDefaultState(stateDefinition.any().setValue(POWERED, false))
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
            if (level.isClientSide) withBlockEntityDo(level, pos) { te: AltMeterBlockEntity ->
                displayScreen(te, player)
            }
        }
        return InteractionResult.SUCCESS
    }

    @Environment(value = EnvType.CLIENT)
    private fun displayScreen(te: AltMeterBlockEntity, player: Player) {
        if (player is LocalPlayer) ScreenOpener.open(AltMeterScreen(te))
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

    override fun getBlockEntityClass(): Class<AltMeterBlockEntity> {
        return AltMeterBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out AltMeterBlockEntity> {
        return ClockworkBlockEntities.ALT_METER.get()
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(POWERED)
    }

    companion object {
        val POWERED: BooleanProperty = BlockStateProperties.POWERED
    }
}
