package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.simibubi.create.AllShapes
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.apache.commons.lang3.ObjectUtils.Null
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.GasHeatLevel
import org.valkyrienskies.clockwork.content.logistics.gas.IEdgeBlock
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock.Companion.DIR_TO_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctConnectionType
import org.valkyrienskies.clockwork.kelvin.api.*
import org.valkyrienskies.clockwork.kelvin.api.edges.PumpDuctEdge
import org.valkyrienskies.clockwork.kelvin.api.nodes.PumpDuctNode
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createEdgeType
import org.valkyrienskies.mod.common.util.toJOMLD

class PumpDuctBlock(properties: Properties): DirectionalKineticBlock(properties), IBE<PumpDuctBlockEntity>, ICogWheel, IEdgeBlock {

    var edge: PumpDuctEdge? = null

    init {
        registerDefaultState(super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false).setValue(IHeatableBlock.GAS_HEAT_LEVEL, GasHeatLevel.COOL))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(IHeatableBlock.GAS_HEAT_LEVEL)
        builder.add(BlockStateProperties.WATERLOGGED)
        super.createBlockStateDefinition(builder)
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(BlockStateProperties.FACING).axis
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return AllShapes.SIX_VOXEL_POLE.get(state.getValue(BlockStateProperties.FACING).axis)
    }

    override fun getBlockEntityClass(): Class<PumpDuctBlockEntity> {
        return PumpDuctBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out PumpDuctBlockEntity> {
        return ClockworkBlockEntities.PUMP_DUCT.get()
    }


    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)

        handlePumpConnection(level, pos, state)
    }

    override fun onRemove(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pNewState: BlockState,
        pIsMoving: Boolean
    ) {
        if (edge != null) ClockworkMod.getKelvin().removeEdge(edge!!.nodeA, edge!!.nodeB)
        edge = null

        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving)
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(BlockStateProperties.WATERLOGGED))
        {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        }

        if (direction.axis == state.getValue(BlockStateProperties.FACING).axis) handlePumpConnection(level, currentPos, state)

        return state
    }


    fun handlePumpConnection(level: LevelAccessor, pos: BlockPos, state: BlockState) {
        val facing = state.getValue(BlockStateProperties.FACING)

        val frontPos = pos.relative(facing)
        val backPos = pos.relative(facing.opposite)

        val front = level.getBlockState(frontPos)
        val back = level.getBlockState(backPos)

        if (front.block !is INodeBlock || back.block !is INodeBlock) return

        if (!(front.block as INodeBlock).canConnectTo(frontPos,pos,facing,level) ||
            !(back.block as INodeBlock).canConnectTo(backPos,pos,facing.opposite,level)) return

        if (edge != null) ClockworkMod.getKelvin().removeEdge(edge!!.nodeA, edge!!.nodeB)
        edge = null

        edge = PumpDuctEdge(backPos.toJOMLD(), frontPos.toJOMLD(), frontPos.toJOMLD())

        ClockworkMod.getKelvin().addEdge(frontPos.toJOMLD(), backPos.toJOMLD(), edge!!)
    }

    override fun connectedTo(pos: BlockPos): Boolean {
        return edge == null || pos.toJOMLD() == edge!!.nodeA || pos.toJOMLD() == edge!!.nodeB
    }

    override fun tryConnectEdge(level: Level, pos: BlockPos) {
        if (edge != null) return
        handlePumpConnection(level, pos, level.getBlockState(pos))

    }

    override fun tryDisconnectEdge(level: Level, pos: BlockPos) {
        if (edge == null) return
        ClockworkMod.getKelvin().removeEdge(edge!!.nodeA, edge!!.nodeB)
        edge = null
    }


}