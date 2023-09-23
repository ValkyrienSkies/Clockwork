package org.valkyrienskies.clockwork.client.render

import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceProvider
import org.apache.logging.log4j.LogManager
import org.valkyrienskies.clockwork.ClockworkMod
import java.io.IOException

class ShaderReference(private val name: String, private val format: VertexFormat) {
    var shader: ShaderInstance? = null
        private set

    fun reload(provider: ResourceProvider) {
        if (shader != null) {
            shader!!.close()
            shader = null
        }
        try {
            shader = ShaderInstance(ResourceProvider { location: ResourceLocation ->
                try {
                    return@ResourceProvider provider.getResource(
                        ResourceLocation(
                            ClockworkMod.MOD_ID,
                            location.path
                        )
                    )
                } catch (e: IOException) {
                    ClockworkMod.LOGGER.warn("Shader is failing to load?", e)
                    return@ResourceProvider provider.getResource(location)
                }
            }, name, format)
        } catch (e: Exception) {
            LOGGER.error(e)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger()
    }
}