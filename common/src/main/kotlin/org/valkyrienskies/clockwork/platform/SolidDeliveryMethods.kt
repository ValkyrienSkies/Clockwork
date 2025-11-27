package org.valkyrienskies.clockwork.platform

import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlockEntity

object SolidDeliveryMethods {
    @JvmStatic
    @ExpectPlatform
    fun extractFrom(level: Level, be: DeliveryCannonBlockEntity): ItemStack {
        throw AssertionError()
    }
}