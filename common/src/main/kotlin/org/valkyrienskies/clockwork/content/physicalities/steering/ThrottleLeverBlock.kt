package org.valkyrienskies.clockwork.content.physicalities.steering

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.schematics.VdexConstruction

class ThrottleLeverBlock(properties: Properties) : HorizontalDirectionalBlock(properties), IBE<ThrottleLeverBlockEntity> {
    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWER, 0))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, POWER)
    }

    override fun getBlockEntityClass() = ThrottleLeverBlockEntity::class.java
    override fun getBlockEntityType() = ClockworkBlockEntities.THROTTLE_LEVER.get()
    override fun getStateForPlacement(context: BlockPlaceContext) =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    override fun rotate(state: BlockState, rotation: Rotation) = state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    override fun mirror(state: BlockState, mirror: Mirror) = rotate(state, mirror.getRotation(state.getValue(FACING)))
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        if (state.getValue(FACING).axis == Direction.Axis.Z) SHAPE_Z else SHAPE_X

    override fun isSignalSource(state: BlockState) = true

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction) =
        state.getValue(POWER)

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.setPlacedBy(level, pos, state, placer, stack)
        (stack.item as? DrivingControlItem)?.linkSelected(level, pos, placer, stack)
    }

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (!player.getItemInHand(hand).isEmpty || !player.mayBuild() || !level.mayInteract(player, pos))
            return InteractionResult.PASS
        if (level is ServerLevel && !VdexConstruction.locked(level, pos)) {
            withBlockEntityDo(level, pos) { it.adjust(if (player.isShiftKeyDown) -5 else 5) }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, next: BlockState, moved: Boolean) {
        if (level is ServerLevel && state.block != next.block) DrivingControlLinks.get(level).endpointRemoved(pos)
        IBE.onRemove(state, level, pos, next)
        super.onRemove(state, level, pos, next, moved)
    }

    companion object {
        val POWER = BlockStateProperties.POWER
        private val SHAPE_Z = box(3.0, 0.0, 1.0, 13.0, 14.0, 15.0)
        private val SHAPE_X = box(1.0, 0.0, 3.0, 15.0, 14.0, 13.0)
    }
}
