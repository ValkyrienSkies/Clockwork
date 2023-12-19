package org.valkyrienskies.clockwork.util.render

import it.unimi.dsi.fastutil.objects.ObjectArraySet
import net.minecraft.client.renderer.RenderType


object BlockRenderTypeRegistry {

    private val renderLayers: MutableSet<RenderType> = ObjectArraySet()
    private var registryLocked = false

    fun registerRenderLayer(layer: RenderType) {
        check(!registryLocked) {
            String.format(
                "RenderLayer %s was added too late.",
                layer
            )
        }
        renderLayers.add(layer)
    }

    fun getLayers(): Set<RenderType> {
        registryLocked = true
        return renderLayers
    }


}