package org.valkyrienskies.clockwork.content.logistics.heat.creative.gas

import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlock

class CreativeGasSourceBlock(properties: Properties) : Block(properties), IBE<CreativeGasSourceBlockEntity> {

    init {
        registerDefaultState(stateDefinition.any().setValue(AltMeterBlock.POWERED, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(AltMeterBlock.POWERED)
    }

    override fun getBlockEntityClass(): Class<CreativeGasSourceBlockEntity> {
        return CreativeGasSourceBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out CreativeGasSourceBlockEntity> {
        return ClockworkBlockEntities.CREATIVE_GAS_SOURCE.get()
    }
}