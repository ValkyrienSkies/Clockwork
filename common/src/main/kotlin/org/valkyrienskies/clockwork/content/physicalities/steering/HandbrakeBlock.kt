package org.valkyrienskies.clockwork.content.physicalities.steering

import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
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
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.schematics.VdexConstruction
import org.valkyrienskies.mod.common.toWorldCoordinates

class HandbrakeBlock(properties: Properties) : HorizontalDirectionalBlock(properties), IBE<HandbrakeBlockEntity> {
    init { registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LATCHED, false).setValue(PULLED, false)) }
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) { builder.add(FACING, LATCHED, PULLED) }
    override fun getBlockEntityClass() = HandbrakeBlockEntity::class.java
    override fun getBlockEntityType() = ClockworkBlockEntities.HANDBRAKE.get()
    override fun getStateForPlacement(context: BlockPlaceContext) = defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    override fun rotate(state: BlockState, rotation: Rotation) = state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
    override fun mirror(state: BlockState, mirror: Mirror) = rotate(state, mirror.getRotation(state.getValue(FACING)))
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) =
        if (state.getValue(FACING).axis == Direction.Axis.Z) SHAPE_Z else SHAPE_X

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.setPlacedBy(level, pos, state, placer, stack)
        (stack.item as? DrivingControlItem)?.linkSelected(level, pos, placer, stack)
    }

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (!player.getItemInHand(hand).isEmpty || player.isSpectator) return InteractionResult.PASS
        if (level is ServerLevel) {
            level.setBlock(pos, state.cycle(LATCHED), UPDATE_CLIENTS)
            (level.getBlockEntity(pos) as? HandbrakeBlockEntity)?.refresh(level)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, next: BlockState, moved: Boolean) {
        if (level is ServerLevel && state.block != next.block) DrivingControlLinks.get(level).endpointRemoved(pos)
        IBE.onRemove(state, level, pos, next)
        super.onRemove(state, level, pos, next, moved)
    }

    companion object {
        val LATCHED = BlockStateProperties.LOCKED
        val PULLED = BlockStateProperties.POWERED
        private val SHAPE_Z = box(4.0, 0.0, 2.0, 12.0, 12.0, 14.0)
        private val SHAPE_X = box(2.0, 0.0, 4.0, 14.0, 12.0, 12.0)
    }
}

class HandbrakeBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos, state) {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}
    override fun tick() {
        super.tick()
        (level as? ServerLevel)?.let(::refresh)
    }

    internal fun refresh(world: ServerLevel) {
        val links = DrivingControlLinks.get(world)
        val latched = blockState.getValue(HandbrakeBlock.LATCHED)
        links.setParked(blockPos, latched)
        val pulled = latched || links.controller(world, blockPos)?.handbrakeHeld == true
        if (pulled == blockState.getValue(HandbrakeBlock.PULLED) || VdexConstruction.locked(world, blockPos)) return
        world.setBlock(blockPos, blockState.setValue(HandbrakeBlock.PULLED, pulled), Block.UPDATE_CLIENTS)
        val point = world.toWorldCoordinates(blockPos.center)
        world.playSound(null, point.x, point.y, point.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, .6f, if (pulled) .55f else .45f)
        if (pulled) links.engageTargets(world, blockPos)
    }
}
