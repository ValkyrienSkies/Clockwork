package org.valkyrienskies.clockwork.content.logistics.gas.heater

import com.simibubi.create.content.processing.basin.BasinBlockEntity
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock

class GasHeaterBlock(properties: Properties) : Block(properties), IBE<GasHeaterBlockEntity>, INodeBlock {

    init {
        registerDefaultState(defaultBlockState().setValue(HEAT_LEVEL, HeatLevel.NONE))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        super.createBlockStateDefinition(builder)
        builder.add(HEAT_LEVEL)
    }

    override fun getBlockEntityClass(): Class<GasHeaterBlockEntity> {
        return GasHeaterBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GasHeaterBlockEntity> {
        return ClockworkBlockEntities.GAS_HEATER.get()
    }


    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)

        if (level.isClientSide) return
        val blockEntity = level.getBlockEntity(pos.above()) as? BasinBlockEntity? ?: return
        blockEntity.notifyChangeOfContents()
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

}