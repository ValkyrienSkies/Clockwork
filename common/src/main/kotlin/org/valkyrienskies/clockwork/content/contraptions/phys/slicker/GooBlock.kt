package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class GooBlock(properties: Properties) : Block(properties.noOcclusion()), IBE<GooBlockEntity> {
    override fun getBlockEntityClass(): Class<GooBlockEntity> {
        return GooBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GooBlockEntity> {
        return ClockworkBlockEntities.GOO_BLOCK.get()
    }
}