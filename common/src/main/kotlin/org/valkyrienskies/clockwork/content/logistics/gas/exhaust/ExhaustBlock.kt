package org.valkyrienskies.clockwork.content.logistics.gas.exhaust

import com.simibubi.create.AllShapes
import com.simibubi.create.foundation.block.IBE
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkShapes
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.util.blocktype.ConnectedWingAlike
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats

class ExhaustBlock(properties: Properties) : DirectionalBlock(properties), IBE<ExhaustBlockEntity>, IDuct, IHaveDuctStats {

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING)
        super.createBlockStateDefinition(builder)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {

        val nearestLookingDirection = context.nearestLookingDirection
        return defaultBlockState().setValue(
            FACING, if (context.player != null && context.player!!
                    .isShiftKeyDown
            ) nearestLookingDirection else nearestLookingDirection.opposite
        )
    }

    override fun getShape(
        state: BlockState,
        worldIn: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape? {
        return ClockworkShapes.EXHAUST.get(
            when (state.getValue(FACING)) {
                Direction.EAST, Direction.WEST -> Direction.Axis.X
                Direction.UP, Direction.DOWN -> Direction.Axis.Y
                Direction.NORTH, Direction.SOUTH -> Direction.Axis.Z
            }
        )
    }

    override fun rotate(state: BlockState, rot: Rotation): BlockState {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)))
    }

    override fun mirror(state: BlockState, mirrorIn: Mirror): BlockState {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)))
    }

    override fun getBlockEntityClass(): Class<ExhaustBlockEntity> {
        return ExhaustBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out ExhaustBlockEntity> {
        return ClockworkBlockEntities.EXHAUST.get()
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        nodePlace(state, level, pos, oldState, movedByPiston)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        nodeRemove(state, level, pos, newState, movedByPiston)
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        if (direction != level.getBlockState(self).getValue(FACING).opposite) return false
        return super.canConnectTo(self, other, direction, level)
    }

    override fun getInternalVolume(): Double {
        return 0.25
    }

    override fun getAdditionalInfoLines(): List<Component> {
        return listOf(
            Component.translatable("vs_clockwork.gas_exhaust.function1").withStyle(ChatFormatting.GRAY).withStyle(
            ChatFormatting.ITALIC),

            Component.translatable("vs_clockwork.gas_exhaust.function2").withStyle(ChatFormatting.GRAY).withStyle(
            ChatFormatting.ITALIC)

            )
    }


}
