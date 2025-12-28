package org.valkyrienskies.clockwork.content.logistics.gas.valve

import com.simibubi.create.AllShapes
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.createmod.catnip.data.Iterate
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Axis
import net.minecraft.network.chat.Component
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.api.edges.ApertureDuctEdge
import org.valkyrienskies.kelvin.util.INodeBlock

class ValveDuctBlock(properties: Properties?) : DirectionalAxisKineticBlock(properties), INodeBlock, IBE<ValveDuctBlockEntity>, IHaveDuctStats {

    var edge: ApertureDuctEdge? = null


    override fun getShape(state: BlockState, p_220053_2_: BlockGetter, p_220053_3_: BlockPos, p_220053_4_: CollisionContext): VoxelShape {
        return AllShapes.FLUID_VALVE[getDuctAxis(state)]
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)

        val axis = getDuctAxis(state)
        for (dir in listOf(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE), Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE))) {
            withBlockEntityDo(level, pos) { it.updateConnection(it.level!!, pos, dir) }
        }
    }

    override fun onRemove(pState: BlockState, pLevel: Level, pPos: BlockPos, pNewState: BlockState, pIsMoving: Boolean) {
        nodeRemove(pState, pLevel, pPos, pNewState, pIsMoving)
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving)
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        val state = level.getBlockState(self)
        if (state.block !is ValveDuctBlock) return false
        if (direction.axis != getDuctAxis(state)) return false

        return super.canConnectTo(self, other, direction, level)
    }


    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighborBlock: Block, neighborPos: BlockPos, movedByPiston: Boolean) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)

        val direction = Direction.fromDelta(neighborPos.x-pos.x,neighborPos.y-pos.y,neighborPos.z-pos.z) ?: return
        if (direction.axis == state.getValue(BlockStateProperties.FACING).axis) withBlockEntityDo(level, pos) {
            it.updateConnection(it.level!!, pos, direction)
        }
    }

    override fun getBlockEntityClass(): Class<ValveDuctBlockEntity> {
        return ValveDuctBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out ValveDuctBlockEntity> {
        return ClockworkBlockEntities.VALVE_DUCT.get()
    }

    override fun getInternalVolume(): Double {
        return 0.0
    }

    override fun getMaximumPressure(): Double {
        return 0.0
    }

    override fun getMaximumTemperature(): Double {
        return 0.0
    }

    override fun getAdditionalInfoLines(): List<Component> {
        return listOf(Component.translatable("vs_clockwork.valve_duct.function").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC))
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
