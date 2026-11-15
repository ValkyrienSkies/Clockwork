package org.valkyrienskies.clockwork.content.physicalities.steering

import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.foundation.block.IBE
import net.createmod.catnip.math.VoxelShaper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class SteeringWheelBlock(properties: Properties) : HorizontalDirectionalBlock(properties), IBE<SteeringWheelBlockEntity>, IWrenchable {
    init { registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)) }
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) { builder.add(FACING) }
    override fun getBlockEntityClass() = SteeringWheelBlockEntity::class.java
    override fun getBlockEntityType() = ClockworkBlockEntities.STEERING_WHEEL.get()
    override fun getStateForPlacement(context: BlockPlaceContext) = defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    override fun rotate(state: BlockState, rotation: Rotation): BlockState = state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    override fun mirror(state: BlockState, mirror: Mirror): BlockState = state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) = SHAPE.get(state.getValue(FACING))

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (player.isSpectator || !player.getItemInHand(hand).isEmpty || player.isShiftKeyDown) return InteractionResult.PASS
        val wheel = level.getBlockEntity(pos) as? SteeringWheelBlockEntity ?: return InteractionResult.PASS
        if (level.isClientSide) SteeringWheelClient.use(pos)
        else wheel.use(player)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, next: BlockState, moved: Boolean) {
        IBE.onRemove(state, level, pos, next)
        super.onRemove(state, level, pos, next, moved)
    }

    companion object {
        private val SHAPE = VoxelShaper.forHorizontal(Shapes.or(
            box(4.0, 0.0, 3.0, 12.0, 8.0, 12.0),
            box(2.0, 3.0, -1.0, 14.0, 7.0, 4.0),
            box(2.0, 7.0, 2.0, 14.0, 12.0, 8.0)
        ), Direction.NORTH)
    }
}
