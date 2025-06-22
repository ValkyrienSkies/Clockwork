package org.valkyrienskies.clockwork.content.logistics.gas.storage.tank

import com.simibubi.create.AllBlocks
import com.simibubi.create.api.connectivity.ConnectivityHandler
import com.simibubi.create.content.fluids.tank.FluidTankBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.NodeBehaviorType
import org.valkyrienskies.kelvin.api.nodes.TankDuctNode


class DuctTankBlock(properties: Properties) : Block(properties), INodeBlock, IBE<DuctTankBlockEntity> {


    init {
        registerDefaultState(
            defaultBlockState().setValue(FluidTankBlock.TOP, true)
                .setValue(FluidTankBlock.BOTTOM, true))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder.add(TOP).add(BOTTOM))
    }



    override fun createNode(pos: DuctNodePos): DuctNode {
        return TankDuctNode(pos, NodeBehaviorType.TANK, volume = 0.05, maxPressure = 16375049.0, maxTemperature = 1478.0, size = 9.0)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
        (level.getBlockEntity(pos) as? DuctTankBlockEntity)?.queueConnectivityUpdate()
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)


        val blockEntity = (level.getBlockEntity(pos) as? DuctTankBlockEntity) ?: return super.onRemove(state, level, pos, newState, isMoving)
        //ConnectivityHandler.splitMulti(blockEntity)

        super.onRemove(state, level, pos, newState, isMoving)
    }


    override fun getBlockEntityClass(): Class<DuctTankBlockEntity> {
        return  DuctTankBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DuctTankBlockEntity> {
        return ClockworkBlockEntities.DUCT_TANK.get()
    }

    companion object {
        val TOP: BooleanProperty = BooleanProperty.create("top")
        val BOTTOM: BooleanProperty = BooleanProperty.create("bottom")
    }


}