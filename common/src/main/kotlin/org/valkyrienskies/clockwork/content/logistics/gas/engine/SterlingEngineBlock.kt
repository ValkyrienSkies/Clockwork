package org.valkyrienskies.clockwork.content.logistics.gas.engine

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel
import com.simibubi.create.foundation.block.IBE
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctPipeNode
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.INodeBlock

class SterlingEngineBlock(properties: Properties) :
    DirectionalKineticBlock(properties), IBE<SterlingEngineBlockEntity>, ICogWheel, IHaveDuctStats, INodeBlock {

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).axis
    }

    override fun getBlockEntityClass(): Class<SterlingEngineBlockEntity> {
        return SterlingEngineBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SterlingEngineBlockEntity> {
        return ClockworkBlockEntities.STERLING_ENGINE.get()
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)

        val facing = state.getValue(FACING)
        for (direction in listOf(facing, facing.opposite)) {
            withBlockEntityDo(level, pos) { it.updateConnection(it.level!!, pos, direction) }
        }
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        val state = level.getBlockState(self)
        if (state.block !is SterlingEngineBlock) return false
        if (direction.axis != state.getValue(FACING).axis) return false
        return super.canConnectTo(self, other, direction, level)
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        neighborBlock: Block,
        neighborPos: BlockPos,
        movedByPiston: Boolean
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)

        val direction = Direction.fromDelta(neighborPos.x - pos.x, neighborPos.y - pos.y, neighborPos.z - pos.z) ?: return
        if (direction.axis == state.getValue(FACING).axis) {
            withBlockEntityDo(level, pos) {
                it.updateConnection(it.level!!, pos, direction)
            }
        }
    }

    override fun createNode(pos: DuctNodePos): DuctNode {
        return DuctPipeNode(pos = pos, volume = getInternalVolume(), maxPressure = 16375049.0, maxTemperature = 1478.0)
    }

    override fun getInternalVolume(): Double {
        return ClockworkConfig.SERVER.ductVolme
    }

    override fun getMaximumPressure(): Double {
        return 0.0
    }

    override fun getMaximumTemperature(): Double {
        return 0.0
    }

    override fun getAdditionalInfoLines(): List<Component> {
        return listOf(
            Component.translatable("vs_clockwork.sterling_engine.function1").withStyle(ChatFormatting.GRAY).withStyle(
                ChatFormatting.ITALIC
            ),
            Component.translatable("vs_clockwork.sterling_engine.function2").withStyle(ChatFormatting.GRAY).withStyle(
                ChatFormatting.ITALIC
            )
        )
    }
}
