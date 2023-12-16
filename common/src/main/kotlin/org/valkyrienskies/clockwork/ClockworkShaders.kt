package org.valkyrienskies.clockwork

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.datafixers.util.Pair
import dev.architectury.event.events.client.ClientReloadShadersEvent
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceProvider
import java.io.IOException
import java.util.function.Consumer


object ClockworkShaders {

    private var crystal: ShaderInstance? = null
    private var scan_effect: ShaderInstance? = null

    fun crystal(): ShaderInstance {
        return crystal!!
    }

    fun scan_effect(): ShaderInstance {
        return scan_effect!!
    }

    fun init(){
        ClientReloadShadersEvent.EVENT.register{ resourceProvider: ResourceProvider, shadersSink: ClientReloadShadersEvent.ShadersSink ->
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
                        "scan_effect",
                        DefaultVertexFormat.POSITION_TEX
                    )
                ) { inst -> scan_effect = inst }
            } catch (ex: IOException) {
                System.err.println("Failed to load shader")
                ex.printStackTrace()
            }
        }
    }

}