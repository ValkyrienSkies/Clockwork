package org.valkyrienskies.clockwork.content.contraptions.phys.hinge

import com.simibubi.create.api.contraption.transformable.TransformableBlock
import com.simibubi.create.content.contraptions.StructureTransform
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class HingeBlock(properties: Properties, val attachment: Boolean = false) : WrenchableDirectionalBlock(properties), IBE<HingeBlockEntity>, TransformableBlock {
    init { registerDefaultState(defaultBlockState().setValue(ROLL, 0).setValue(ASSEMBLED, false)) }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder.add(ROLL, ASSEMBLED))
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val facing = context.clickedFace
        return defaultBlockState().setValue(FACING, facing)
            .setValue(ROLL, if (facing.axis == Direction.Axis.Y && context.horizontalDirection.axis == Direction.Axis.X) 1 else 0)
    }

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild() || player.isSpectator || hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty)
            return InteractionResult.PASS
        if (!level.isClientSide && player is ServerPlayer) {
            val error = HingeAssembly.use(player.serverLevel(), pos)
            if (error != null) player.displayClientMessage(Component.translatable("message.vs_clockwork.hinge.$error"), true)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        if (!player.mayBuild() || player.isSpectator) return InteractionResult.FAIL
        if (state.getValue(ASSEMBLED) || attachment) {
            if (player is ServerPlayer) HingeAssembly.reconnect(player, context.clickedPos, context.itemInHand)
            return InteractionResult.SUCCESS
        }
        if (context.clickedFace.axis == HingeGeometry.axis(state).axis) return InteractionResult.SUCCESS
        if (context.clickedFace.axis == state.getValue(FACING).axis) {
            if (!context.level.isClientSide) context.level.setBlock(context.clickedPos, state.cycle(ROLL), 3)
            return InteractionResult.SUCCESS
        }
        return super.onWrenched(state, context)
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState = oriented(state,
        rotation.rotate(state.getValue(FACING)), rotation.rotate(HingeGeometry.axis(state)))

    override fun transform(state: BlockState, transform: StructureTransform): BlockState = oriented(state,
        transform.rotateFacing(state.getValue(FACING)), transform.rotateFacing(HingeGeometry.axis(state)))

    private fun oriented(state: BlockState, facing: Direction, axis: Direction): BlockState {
        val result = state.setValue(FACING, facing)
        return (0..3).map { result.setValue(ROLL, it) }.first { HingeGeometry.axis(it) == axis }
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
        shapes.getValue(Triple(state.getValue(FACING), state.getValue(ROLL),
            if (attachment) 2 else if (state.getValue(ASSEMBLED)) 1 else 0))

    override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
        if (attachment && state.getValue(ASSEMBLED)) Shapes.empty() else getShape(state, level, pos, context)

    override fun getBlockEntityClass() = HingeBlockEntity::class.java
    override fun getBlockEntityType() = ClockworkBlockEntities.HINGE.get()

    companion object {
        val FACING = BlockStateProperties.FACING
        val ROLL: IntegerProperty = IntegerProperty.create("roll", 0, 3)
        val ASSEMBLED: BooleanProperty = BooleanProperty.create("assembled")
        private val shapes = buildMap {
            val base = listOf(
                doubleArrayOf(2.0, 0.0, 2.0, 14.0, 3.0, 14.0),
                doubleArrayOf(4.0, 3.0, 4.0, 12.0, 12.0, 12.0))
            val head = listOf(
                doubleArrayOf(2.0, 13.0, 2.0, 14.0, 16.0, 14.0),
                doubleArrayOf(3.0, 6.0, 5.0, 13.0, 13.0, 11.0))
            for (facing in Direction.values()) for (roll in 0..3) for (part in 0..2) {
                val q = HingeGeometry.orientation(facing, roll)
                val boxes = when (part) { 1 -> base; 2 -> head; else -> base + head }
                val shape = boxes.fold(Shapes.empty()) { shape, bounds ->
                    val points = buildList {
                        for (x in listOf(bounds[0], bounds[3])) for (y in listOf(bounds[1], bounds[4])) for (z in listOf(bounds[2], bounds[5]))
                            add(q.transform(Vector3d(x - 8, y - 8, z - 8)).add(8.0, 8.0, 8.0))
                    }
                    Shapes.or(shape, box(points.minOf { it.x }, points.minOf { it.y }, points.minOf { it.z },
                        points.maxOf { it.x }, points.maxOf { it.y }, points.maxOf { it.z }))
                }
                put(Triple(facing, roll, part), shape.optimize())
            }
        }
    }
}
