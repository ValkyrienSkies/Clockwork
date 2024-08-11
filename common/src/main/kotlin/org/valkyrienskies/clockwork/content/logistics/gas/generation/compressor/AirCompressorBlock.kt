package org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor

import com.simibubi.create.content.kinetics.base.KineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.duct.INodeBlock
import org.valkyrienskies.clockwork.kelvin.api.DuctNetwork
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.NodeBehaviorType
import org.valkyrienskies.clockwork.kelvin.api.nodes.PipeDuctNode

class AirCompressorBlock(properties: Properties?) : KineticBlock(properties), INodeBlock, IBE<AirCompressorBlockEntity> {

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        _onPlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        _onRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getRotationAxis(state: BlockState?): Direction.Axis {
        return Direction.Axis.Y
    }

    override fun createNode(pos: DuctNodePos, network: DuctNetwork): PipeDuctNode {
        return PipeDuctNode(pos, NodeBehaviorType.PIPE, network, volume = 0.05, maxPressure = 16375049.0, maxTemperature = 1478.0)
    }

    override fun getBlockEntityClass(): Class<AirCompressorBlockEntity> {
        return AirCompressorBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out AirCompressorBlockEntity> {
        return ClockworkBlockEntities.AIR_COMPRESSOR.get()
    }

    override fun hasShaftTowards(world: LevelReader?, pos: BlockPos?, state: BlockState?, face: Direction): Boolean {
        return face == Direction.DOWN
    }

}