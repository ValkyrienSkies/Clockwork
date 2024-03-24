package org.valkyrienskies.clockwork.content.logistics.heat.creative.source

import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlock

class CreativeHeatSourceBlock(properties: Properties) : Block(properties), IBE<CreativeHeatSourceBlockEntity> {

    init {
        registerDefaultState(stateDefinition.any().setValue(AltMeterBlock.POWERED, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(AltMeterBlock.POWERED)
    }

    override fun getBlockEntityClass(): Class<CreativeHeatSourceBlockEntity> {
        return CreativeHeatSourceBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out CreativeHeatSourceBlockEntity> {
        return ClockworkBlockEntities.CREATIVE_HEAT_SOURCE.get()
    }

}