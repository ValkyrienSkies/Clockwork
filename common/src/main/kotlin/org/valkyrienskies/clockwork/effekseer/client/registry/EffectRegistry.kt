package org.valkyrienskies.clockwork.effekseer.client.registry

import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.effekseer.client.loader.EffekAssetLoader
import java.util.function.BiConsumer

/**
 *
 * @author ChloePrime
 */
object EffectRegistry {
    fun get(id: ResourceLocation?): EffectDefinition? {
        return EffekAssetLoader.get()!!.get(id!!)
    }

    fun entries(): Collection<Map.Entry<ResourceLocation, EffectDefinition>> {
        return EffekAssetLoader.get()!!.entries()
    }

    fun forEach(action: BiConsumer<ResourceLocation, EffectDefinition>) {
        EffekAssetLoader.get()!!.forEach(action)
    }
}