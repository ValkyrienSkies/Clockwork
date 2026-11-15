package org.valkyrienskies.clockwork.content.physicalities.steering

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class SteeringWheelBlock(properties: Properties) : HorizontalKineticBlock(properties), IBE<SteeringWheelBlockEntity> {
    override fun getBlockEntityClass() = SteeringWheelBlockEntity::class.java
    override fun getBlockEntityType() = ClockworkBlockEntities.STEERING_WHEEL.get()
    override fun getRotationAxis(state: BlockState) = Direction.Axis.Y
    override fun hasShaftTowards(level: LevelReader, pos: BlockPos, state: BlockState, face: Direction) = face == Direction.DOWN
    override fun getStateForPlacement(context: BlockPlaceContext) = defaultBlockState().setValue(HORIZONTAL_FACING, context.horizontalDirection.opposite)
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) = SHAPE

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
        private val SHAPE = Shapes.or(box(5.0, 0.0, 5.0, 11.0, 10.0, 11.0), box(1.0, 10.0, 1.0, 15.0, 14.0, 15.0))
    }
}
