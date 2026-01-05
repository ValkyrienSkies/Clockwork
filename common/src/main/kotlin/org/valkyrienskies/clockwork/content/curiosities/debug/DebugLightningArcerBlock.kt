package org.valkyrienskies.clockwork.content.curiosities.debug

import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class DebugLightningArcerBlock(properties: Properties) : Block(properties), IBE<DebugLightningArcerBlockEntity> {
    override fun getBlockEntityClass(): Class<DebugLightningArcerBlockEntity> {
        return DebugLightningArcerBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DebugLightningArcerBlockEntity> {
        return ClockworkBlockEntities.DEBUG_LIGHTNING_ARCER.get()
    }
}
