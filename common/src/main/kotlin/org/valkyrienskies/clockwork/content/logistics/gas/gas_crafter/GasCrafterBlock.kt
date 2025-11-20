package org.valkyrienskies.clockwork.content.logistics.gas.gas_crafter

import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class GasCrafterBlock(properties: Properties) : Block(properties), IBE<GasCrafterBlockEntity> {
    override fun getBlockEntityClass(): Class<GasCrafterBlockEntity> {
        return GasCrafterBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GasCrafterBlockEntity?>? {
        return ClockworkBlockEntities.GAS_CRAFTER.get()
    }


}