package org.valkyrienskies.clockwork.content.logistics.gas.storage.tank

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock
import org.valkyrienskies.kelvin.api.*
import org.valkyrienskies.kelvin.api.nodes.TankDuctNode


class DuctTankBlock(properties: Properties) : Block(properties), INodeBlock, IBE<DuctTankBlockEntity> {



    override fun createNode(pos: DuctNodePos, network: DuctNetwork<ServerLevel>): DuctNode {
        return TankDuctNode(pos, NodeBehaviorType.TANK, network, volume = 0.05, maxPressure = 16375049.0, maxTemperature = 1478.0, size = 9.0)
    }



    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }


    override fun getBlockEntityClass(): Class<DuctTankBlockEntity> {
        return  DuctTankBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DuctTankBlockEntity> {
        return ClockworkBlockEntities.DUCT_TANK.get()
    }




}