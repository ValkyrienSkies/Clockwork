package org.valkyrienskies.clockwork.content.propulsion.sugar_rocket

import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.utility.UniqueLinkedList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class SugarRocketBlock(properties: Properties) : DirectionalBlock(properties), IBE<SugarRocketBlockEntity>, IWrenchable {
    override fun getBlockEntityClass(): Class<SugarRocketBlockEntity> {
        return SugarRocketBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SugarRocketBlockEntity> {
        return ClockworkBlockEntities.SUGAR_ROCKET.get()
    }

    override fun getRenderShape(pState: BlockState): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    fun getAxialPositions(pos: BlockPos, ignoreAxis: Direction.Axis): List<BlockPos> {
        val list = mutableListOf<BlockPos>()
        for (axis in Direction.Axis.entries) {
            if (axis == ignoreAxis) {
                continue
            }
            list.add(pos.relative(Direction.get(Direction.AxisDirection.POSITIVE, axis)))
            list.add(pos.relative(Direction.get(Direction.AxisDirection.NEGATIVE, axis)))
        }
        return list
    }

    fun triggerAdjacent(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean {
        var ignoreAxis = state.getValue(DirectionalBlock.FACING).axis
        val triggerPositions = UniqueLinkedList<BlockPos>()
        triggerPositions.addAll(getAxialPositions(pos, ignoreAxis))
        val referenceFacing = state.getValue(DirectionalBlock.FACING)
        val visited = mutableSetOf<BlockPos>()
        while (triggerPositions.isNotEmpty()) {
            val newpos = triggerPositions.poll()
            if (visited.contains(newpos)) {
                continue
            }
            visited.add(newpos)
            val blockEntity = level.getBlockEntity(newpos) as? SugarRocketBlockEntity ?: continue
            val blockState = level.getBlockState(newpos)
            if (referenceFacing != blockState.getValue(FACING)) {
                continue
            }
            withBlockEntityDo(level, newpos) { blockEntity ->
                blockEntity.isBurning = true
            }
            triggerPositions.addAll(getAxialPositions(newpos, ignoreAxis))
        }
        return visited.size > 0
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        if (level.getBlockEntity(pos.relative(state.getValue(DirectionalBlock.FACING))) !is SugarRocketBlockEntity || level.getBlockState(pos.relative(state.getValue(DirectionalBlock.FACING))).getValue(DirectionalBlock.FACING) != state.getValue(DirectionalBlock.FACING)) {
            withBlockEntityDo(level, pos) { blockEntity ->
                blockEntity.hasNextBlock = false
            }
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        val hasNextBlock = level.getBlockEntity(pos.relative(state.getValue(DirectionalBlock.FACING))) is SugarRocketBlockEntity && level.getBlockState(pos.relative(state.getValue(DirectionalBlock.FACING))).getValue(DirectionalBlock.FACING) == state.getValue(DirectionalBlock.FACING)
        withBlockEntityDo(level, pos) { blockEntity ->
            blockEntity.hasNextBlock = hasNextBlock
        }
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val stack = player.getItemInHand(hand)
            if (stack.`is`(Items.SUGAR)) {
                withBlockEntityDo(level, pos) { blockEntity ->
                    if (blockEntity.sugarCooldown == 0) {
                        blockEntity.addSugar(1)
                        stack.shrink(1)
                    }
                }
                return InteractionResult.SUCCESS
            } else if (stack.`is`(Items.FLINT_AND_STEEL)) {
                triggerAdjacent(level as ServerLevel, pos, state)
                stack.hurtAndBreak(1, player, { stack.shrink(1) })
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0f, 1.0f)
                return InteractionResult.SUCCESS

            } else if (stack.`is`(Items.FIRE_CHARGE)) {
                triggerAdjacent(level as ServerLevel, pos, state)
                stack.shrink(1)
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f)
                return InteractionResult.SUCCESS
            }
        }
        return super.use(state, level, pos, player, hand, hit)
    }
}