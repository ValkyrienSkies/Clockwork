package org.valkyrienskies.clockwork.services

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack

interface ClockWorkPlatformHelper {
    fun createCreativeTab(id: ResourceLocation, stack: () -> ItemStack): CreativeModeTab
}