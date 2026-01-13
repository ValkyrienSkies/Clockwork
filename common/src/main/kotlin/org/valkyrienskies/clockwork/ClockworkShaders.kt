package org.valkyrienskies.clockwork

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import dev.architectury.event.events.client.ClientReloadShadersEvent
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceProvider
import java.io.IOException
import java.util.function.Supplier
import kotlin.reflect.KFunction

object ClockworkShaders {

    private var crystal: ShaderInstance? = null
    private var heat: ShaderInstance? = null
    private var haze: ShaderInstance? = null

    private var reEntryPass1: ShaderInstance? = null
    private var reEntryPass2: ShaderInstance? = null
    private var reEntryPass3: ShaderInstance? = null

    fun crystal(): ShaderInstance {
        return crystal!!
    }

    fun heat(): ShaderInstance? {
        return heat
    }

    fun haze(): ShaderInstance? {
        return haze
    }

    fun reentry(pass: Int): () -> ShaderInstance {
        return when (pass) {
            1 -> ::reentryPassOne
            2 -> ::reentryPassTwo
            3 -> ::reentryPassThree
            else -> ::reentryPassOne
        }
    }

    private fun reentryPassOne(): ShaderInstance {
        return reEntryPass1!!
    }

    private fun reentryPassTwo(): ShaderInstance {
        return reEntryPass2!!
    }

    private fun reentryPassThree(): ShaderInstance {
        return reEntryPass3!!
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

                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "rendertype/reentry/pass_1",
                        DefaultVertexFormat.BLOCK
                    )
                ) {
                    inst -> reEntryPass1 = inst
                }

                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "rendertype/reentry/pass_2",
                        DefaultVertexFormat.BLOCK
                    )
                ) {
                    inst -> reEntryPass2 = inst
                }

                shadersSink.registerShader(
                    ShaderInstance(
                        resourceProvider,
                        "rendertype/reentry/pass_3",
                        DefaultVertexFormat.BLOCK
                    )
                ) { inst ->
                    reEntryPass3 = inst
                }

            } catch (ex: IOException) {
                //System.err.println("Failed to load shader")
                ex.printStackTrace()
            }
        }
    }

}
