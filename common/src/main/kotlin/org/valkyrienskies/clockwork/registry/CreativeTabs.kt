package org.valkyrienskies.clockwork.registry

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.services.ClockWorkPlatformHelper
import java.util.ServiceLoader

class CreativeTabs {
    companion object {
        fun create(id: ResourceLocation, stack: () -> ItemStack): CreativeModeTab {
            return ServiceLoader.load(ClockWorkPlatformHelper::class.java)
                .findFirst()
                .get()
                .createCreativeTab(id, stack)
        }
    }
}