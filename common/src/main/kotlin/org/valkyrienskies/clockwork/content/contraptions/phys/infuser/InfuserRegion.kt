package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag

data class InfuserRegion(val seed: BlockPos, var shipId: Long = -1, var offset: BlockPos = BlockPos.ZERO) {
    fun save() = CompoundTag().also {
        it.putLong("seed", seed.asLong()); it.putLong("ship", shipId); it.putLong("offset", offset.asLong())
    }
    companion object {
        fun load(tag: CompoundTag) = InfuserRegion(BlockPos.of(tag.getLong("seed")),
            tag.getLong("ship"), BlockPos.of(tag.getLong("offset")))
    }
}
