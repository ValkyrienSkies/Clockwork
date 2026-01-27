package org.valkyrienskies.clockwork.content.logistics.gas.storage.tank

import com.simibubi.create.api.connectivity.ConnectivityHandler
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.core.util.squared
import org.valkyrienskies.kelvin.KelvinMod.KELVINLOGGER
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.NodeBehaviorType
import org.valkyrienskies.kelvin.api.nodes.TankDuctNode
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos


class DuctTankBlock(properties: Properties) : Block(properties), INodeBlock, IBE<DuctTankBlockEntity>, IHaveDuctStats {
    init {
        registerDefaultState(
            defaultBlockState()
                .setValue(TOP, true)
                .setValue(BOTTOM, true)
                .setValue(LARGE, false)
        )
    }

//    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
//        println(state.getValue(TOP))
//    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder.add(TOP).add(BOTTOM).add(LARGE))
    }

    override fun nodePlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        if (level.isClientSide) return //withBlockEntityDo(level, pos) { be -> ClockworkModClient.getKelvin().addNode(be.getDuctNodePosition(), createTankNode(be.getDuctNodePosition(), -1.0)) }
        if (state.isAir || state.block !is INodeBlock) return

        withBlockEntityDo(level, pos) { blockEntity ->
            val size = blockEntity.width.squared() * blockEntity.height
            ClockworkMod.getKelvin().addNode(blockEntity.getDuctNodePosition(), createTankNode(blockEntity.getDuctNodePosition(), size.toDouble()))
        }
    }

    override fun nodeRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            ClockworkMod.getKelvin().removeNode(pos.toDuctNodePos(level.dimension().location()))
        } else {
            ClockworkModClient.getKelvin().removeNode(pos.toDuctNodePos(level.dimension().location()))
        }
    }

    override fun createNode(pos: DuctNodePos): DuctNode {
        KELVINLOGGER.warn("Duct Tank createNode() called. This shouldn't happen!!!")
        return super.createNode(pos)
    }

    fun createTankNode(pos: DuctNodePos, size: Double): DuctNode {
        return TankDuctNode(pos, NodeBehaviorType.TANK, volume = size, maxPressure = 16375049.0, maxTemperature = 1478.0, heatConductivity = 1687.5, heatCapacity = 44.9)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        (level.getBlockEntity(pos) as? DuctTankBlockEntity)?.queueConnectivityUpdate()
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (state.hasBlockEntity() && (state.block !== newState.block || !newState.hasBlockEntity())) {
            val blockEntity = level.getBlockEntity(pos) as? DuctTankBlockEntity ?: return
            level.removeBlockEntity(pos)
            ConnectivityHandler.splitMulti(blockEntity)
        }
    }

    override fun getBlockEntityClass(): Class<DuctTankBlockEntity> {
        return DuctTankBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DuctTankBlockEntity> {
        return ClockworkBlockEntities.DUCT_TANK.get()
    }

    companion object {
        val TOP: BooleanProperty = BooleanProperty.create("top")
        val BOTTOM: BooleanProperty = BooleanProperty.create("bottom")
        val LARGE: BooleanProperty = BooleanProperty.create("large")
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        return true
    }
}
