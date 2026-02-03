package org.valkyrienskies.clockwork.content.logistics.gas.docking_vent;

import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkShapes
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock.Companion.DIR_TO_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctConnectionType
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctPipeNode
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.NodeBehaviorType
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.util.INodeBlock

class DockingVentBlock(properties: Properties) : FaceAttachedHorizontalDirectionalBlock(properties),
    INodeBlock, IBE<DockingVentBlockEntity>, SimpleWaterloggedBlock, IHaveDuctStats {

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
        return ClockworkShapes.DOCKING_VENT.get(getConnectedDirection(state))
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        nodePlace(state, level, pos, oldState, movedByPiston)
    }

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        movedByPiston: Boolean
    ) {
        IBE.onRemove(state, level, pos, newState)
    }


    override fun getBlockEntityClass(): Class<DockingVentBlockEntity> {
        return DockingVentBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DockingVentBlockEntity> {
        return ClockworkBlockEntities.DOCKING_VENT.get()
    }

    override fun canSurvive(pState: BlockState, pLevel: LevelReader, pPos: BlockPos): Boolean {
        return canBlockAttach(pLevel, pPos, getConnectedDirection(pState).opposite)
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        if (direction != getConnectedDirection(level.getBlockState(self)).opposite) return false
        return super.canConnectTo(self, other, direction, level)
    }

    override fun getInternalVolume(): Double {
        return ClockworkConfig.SERVER.ductVolme
    }

    override fun createNode(pos: DuctNodePos): DuctNode {
        return DuctPipeNode(pos = pos, volume = getInternalVolume(), maxPressure = 16375049.0, maxTemperature = 1478.0)
    }

    companion object {

        @JvmStatic
        fun canBlockAttach(reader: LevelReader, pos: BlockPos, direction: Direction): Boolean {
            val blockpos = pos.relative(direction)
            return reader.getBlockState(blockpos).block is IDuct
        }

        @JvmStatic
        fun getFacing(blockState: BlockState) : Direction {
            return getConnectedDirection(blockState)
        }
    }
}
