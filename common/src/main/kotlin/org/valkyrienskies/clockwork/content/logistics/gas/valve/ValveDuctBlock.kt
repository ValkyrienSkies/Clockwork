package org.valkyrienskies.clockwork.content.logistics.gas.valve

import com.simibubi.create.AllShapes
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.createmod.catnip.data.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Axis
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.edges.ApertureDuctEdge
import org.valkyrienskies.kelvin.util.IEdgeBlock
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
class ValveDuctBlock(properties: Properties?) : DirectionalAxisKineticBlock(properties), IEdgeBlock, IBE<ValveDuctBlockEntity> {

    var edge: ApertureDuctEdge? = null


    override fun getShape(state: BlockState, p_220053_2_: BlockGetter, p_220053_3_: BlockPos, p_220053_4_: CollisionContext): VoxelShape {
        return AllShapes.FLUID_VALVE[getDuctAxis(state)]
    }

    override fun canConnectTo(level: Level, from: BlockPos,to: BlockPos): Boolean {
        val state = level.getBlockState(from) ?: return false
        val awful = to.subtract(from)
        val direction = Direction.fromDelta(awful.x, awful.y, awful.z) ?: return false

        return direction.axis == getDuctAxis(state)


    }

    override fun tryConnectEdge(level: Level, pos: BlockPos) {
        if (edge != null) return
        handleAperatureConnection(level, pos, level.getBlockState(pos))

    }

    override fun tryDisconnectEdge(level: Level, pos: BlockPos) {
        if (edge == null) return
        ClockworkMod.getKelvin().removeEdge(edge!!.nodeA, edge!!.nodeB)
        edge = null
    }

    fun handleAperatureConnection(level: LevelAccessor, pos: BlockPos, state: BlockState) {
        if (level !is ServerLevel) return
        val axis = getDuctAxis(state)


        val frontPos = pos.relative(axis, 1)
        val backPos = pos.relative(axis, -1)

        val awful = frontPos.subtract(pos)
        val facing = Direction.fromDelta(awful.x, awful.y, awful.z) ?: return


        val front = level.getBlockState(frontPos)
        val back = level.getBlockState(backPos)

        if (front.block !is INodeBlock || back.block !is INodeBlock) return

        if (!(front.block as INodeBlock).canConnectTo(frontPos,pos,facing,level) ||
            !(back.block as INodeBlock).canConnectTo(backPos,pos,facing.opposite,level)) return

        if (edge != null) ClockworkMod.getKelvin().removeEdge(edge!!.nodeA, edge!!.nodeB)
        edge = null

        val backDuctPos = (level.getBlockEntity(backPos) as? INodeBlockEntity)?.getDuctNodePosition() ?: return
        val frontDuctPos = (level.getBlockEntity(frontPos) as? INodeBlockEntity)?.getDuctNodePosition() ?: return

        edge = ApertureDuctEdge(ConnectionType.APERTURE,backDuctPos, frontDuctPos, aperture = 1.0)

        ClockworkMod.getKelvin().addEdge(frontDuctPos, backDuctPos, edge!!)
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (direction.axis == getDuctAxis(state)) handleAperatureConnection(level, currentPos, state)

        return state
    }

    override fun getBlockEntityClass(): Class<ValveDuctBlockEntity> {
        return ValveDuctBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out ValveDuctBlockEntity> {
        return ClockworkBlockEntities.VALVE_DUCT.get()
    }

    companion object {
        fun getDuctAxis(state: BlockState): Axis {
            check(state.block is ValveDuctBlock) { "Provided BlockState is for a different block." }
            val facing = state.getValue(FACING)
            var alongFirst = !state.getValue(AXIS_ALONG_FIRST_COORDINATE)
            for (axis in Iterate.axes) {
                if (axis === facing.axis) continue
                if (!alongFirst) {
                    alongFirst = true
                    continue
                }
                return axis
            }
            throw IllegalStateException("Impossible axis.")
        }
    }
}
