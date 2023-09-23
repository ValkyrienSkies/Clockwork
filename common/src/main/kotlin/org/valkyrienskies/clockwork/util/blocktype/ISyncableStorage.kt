package org.valkyrienskies.clockwork.util.blocktype

import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.item.ItemStack

interface ISyncableStorage : WorldlyContainer {
    fun sync(storage: NonNullList<ItemStack>)

    fun getStorageInventory(): NonNullList<ItemStack>
    fun getStorageInventorySize(): Int

    fun getBlockPositionFromISS(): BlockPos
}