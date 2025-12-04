package org.valkyrienskies.clockwork.content.logistics.gas.crafter

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class GasCrafterBlock(properties: Properties) : Block(properties), IBE<GasCrafterBlockEntity> {
    override fun getBlockEntityClass(): Class<GasCrafterBlockEntity> {
        return GasCrafterBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GasCrafterBlockEntity?>? {
        return ClockworkBlockEntities.GAS_CRAFTER.get()
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighborBlock: Block, neighborPos: BlockPos, movedByPiston: Boolean) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
    }
}