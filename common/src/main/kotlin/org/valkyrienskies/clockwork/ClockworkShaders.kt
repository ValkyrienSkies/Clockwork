package org.valkyrienskies.clockwork

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import dev.architectury.event.events.client.ClientReloadShadersEvent
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceProvider
import java.io.IOException

object ClockworkShaders {

    private var crystal: ShaderInstance? = null
    private var heat: ShaderInstance? = null
    private var haze: ShaderInstance? = null

    fun crystal(): ShaderInstance {
        return crystal!!
    }

    fun heat(): ShaderInstance? {
        return heat
    }

    fun haze(): ShaderInstance? {
        return haze
    }

    fun init() {
        ClientReloadShadersEvent.EVENT.register { resourceProvider: ResourceProvider, shadersSink: ClientReloadShadersEvent.ShadersSink ->
            try {
                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "crystal",
                        DefaultVertexFormat.NEW_ENTITY
                    )
                ) { inst -> crystal = inst }

                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "heat",
                        DefaultVertexFormat.NEW_ENTITY
                    )
                ) { inst -> heat = inst }

                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "haze",
                        DefaultVertexFormat.NEW_ENTITY
                    )
                ) { inst -> haze = inst }

            } catch (ex: IOException) {
                System.err.println("Failed to load shader")
                ex.printStackTrace()
            }
        }
    }

}