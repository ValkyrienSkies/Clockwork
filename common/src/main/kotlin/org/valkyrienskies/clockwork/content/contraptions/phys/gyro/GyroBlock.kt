package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlockEntity

class GyroBlock (properties: Properties) : Block(properties), IBE<GyroBlockEntity> {
    init {
        registerDefaultState(stateDefinition.any())
    }

    override fun getBlockEntityClass(): Class<GyroBlockEntity> {
        return GyroBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GyroBlockEntity> {
        return ClockworkBlockEntities.GYRO.get()
    }
}