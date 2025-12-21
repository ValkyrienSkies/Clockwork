package org.valkyrienskies.clockwork.platform

import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.world.item.crafting.Recipe
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCrafterBlockEntity

object GasCrafterMethods {
    @JvmStatic
    @ExpectPlatform
    fun apply(be: GasCrafterBlockEntity, recipe: Recipe<*>, test: Boolean): Boolean {
        throw AssertionError()
    }
}