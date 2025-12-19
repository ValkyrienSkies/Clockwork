package org.valkyrienskies.clockwork.content.logistics.gas.redstone

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock
import org.valkyrienskies.kelvin.util.INodeBlock

class RedstoneDuctBlock(properties: Properties) : RotatedPillarBlock(properties), INodeBlock, IBE<RedstoneDuctBlockEntity> {

    init {
        registerDefaultState(defaultBlockState().setValue(POWER, 0))
    }

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult? {

        if (!player.getItemInHand(hand).isEmpty) return super.use(state, level, pos, player, hand, hit)

        if (level.isClientSide) {
            withBlockEntityDo(level, pos) {it.openScreen(player)}
        }

        return InteractionResult.SUCCESS
    }

    override fun isSignalSource(state: BlockState): Boolean {
        return true
    }

    override fun getDirectSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        return state.getSignal(level, pos, direction)
    }

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        if (state.getValue(ISensorBlock.Companion.POWER) != 0) {
            return state.getValue(ISensorBlock.Companion.POWER)
        }
        return 0
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder.add(POWER))
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        nodePlace(state, level, pos, oldState, movedByPiston)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        nodeRemove(state, level, pos, newState, movedByPiston)
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun getBlockEntityClass(): Class<RedstoneDuctBlockEntity> {
        return RedstoneDuctBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out RedstoneDuctBlockEntity?>? {
       return ClockworkBlockEntities.REDSTONE_DUCT.get()
    }

    companion object {
        val POWER = BlockStateProperties.POWER
    }
}