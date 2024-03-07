package org.valkyrienskies.clockwork

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import dev.architectury.event.events.client.ClientReloadShadersEvent
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceProvider
import java.io.IOException


object ClockworkShaders {

    private var crystal: ShaderInstance? = null
    private var heat: ShaderInstance? = null
    private var haze: ShaderInstance? = null
    private var scan_effect: ShaderInstance? = null

    fun crystal(): ShaderInstance {
        return crystal!!
    }

    fun heat(): ShaderInstance? {
        return heat
    }

    fun haze(): ShaderInstance? {
        return haze
    }

    fun scan_effect(): ShaderInstance {
        return scan_effect!!
    }

    fun init(){
        ClientReloadShadersEvent.EVENT.register(::register)

    }

    fun register(resourceProvider: ResourceManager?, shadersSink: ClientReloadShadersEvent.ShadersSink?) {
        shadersSink!!.registerShader(
            ShaderInstance(
                resourceProvider!!,
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

        shadersSink.registerShader(
            ShaderInstance(
                resourceProvider,
                "scan_effect",
                DefaultVertexFormat.POSITION_TEX
            )
        ) { inst -> scan_effect = inst }
    }
}