package org.valkyrienskies.clockwork.content.logistics.gas.duct_bearing;

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkShapes
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.kelvin.util.INodeBlock

class DuctBearingBlock(properties: Properties) : FaceAttachedHorizontalDirectionalBlock(properties), INodeBlock, IBE<DuctBearingBlockEntity> {

    init {
        registerDefaultState(defaultBlockState()
            .setValue(FACE, AttachFace.FLOOR)
            .setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block?, BlockState?>) {
        super.createBlockStateDefinition(pBuilder.add(FACE, FACING))
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape? {
        return ClockworkShapes.DUCT_BEARING.get(getConnectedDirection(state))
    }

    override fun getBlockEntityClass(): Class<DuctBearingBlockEntity> {
        return DuctBearingBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DuctBearingBlockEntity> {
        return ClockworkBlockEntities.DUCT_BEARING.get()
    }

    override fun canSurvive(pState: BlockState, pLevel: LevelReader, pPos: BlockPos): Boolean {
        return canBlockAttach(pLevel, pPos, getConnectedDirection(pState).getOpposite())
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        if (direction != getConnectedDirection(level.getBlockState(self)).opposite) return false
        return super.canConnectTo(self, other, direction, level)
    }

    companion object {

        @JvmStatic
        fun canBlockAttach(reader: LevelReader, pos: BlockPos, direction: Direction): Boolean {
            val blockpos = pos.relative(direction)
            println(reader.getBlockState(blockpos).block)
            return reader.getBlockState(blockpos).block is IDuct
        }
    }


}
