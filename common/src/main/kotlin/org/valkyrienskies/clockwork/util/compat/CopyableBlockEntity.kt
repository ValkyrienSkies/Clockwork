package org.valkyrienskies.clockwork.util.compat

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity

/**
 * Should be used when additional actions are needed to copy/move block entity
 */
interface CopyableBlockEntity {
    /**
     * Is called instead of [BlockEntity.saveWithId]
     *
     * Should save enough info to move constraints, etc
     *
     * Look into [PhysBearingBlockEntity] for example
     * @return
     */
    fun copyWrite(): CompoundTag

    /**
     * Is called instead of [BlockEntity.load]
     *
     * Look into [PhysBearingBlockEntity] for example
     *
     * @param tag
     */
    fun copyRead(tag: CompoundTag)
}