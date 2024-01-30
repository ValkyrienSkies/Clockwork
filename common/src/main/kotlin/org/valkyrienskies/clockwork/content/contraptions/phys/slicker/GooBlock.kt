package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class GooBlock(properties: Properties) : Block(properties), IBE<GooBlockEntity> {
    override fun getBlockEntityClass(): Class<GooBlockEntity> {
        return GooBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GooBlockEntity> {
        return ClockworkBlockEntities.GOO_BLOCK.get()
    }
}