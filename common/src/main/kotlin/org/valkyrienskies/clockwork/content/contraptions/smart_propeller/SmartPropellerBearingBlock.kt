package org.valkyrienskies.clockwork.content.contraptions.smart_propeller

import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class SmartPropellerBearingBlock(properties: Properties?) : BearingBlock(properties), IBE<SmartPropellerBearingBlockEntity> {
    override fun getBlockEntityClass(): Class<SmartPropellerBearingBlockEntity> {
        return SmartPropellerBearingBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SmartPropellerBearingBlockEntity> {
        return ClockworkBlockEntities.SMART_PROPELLER_BEARING.get()
    }
}