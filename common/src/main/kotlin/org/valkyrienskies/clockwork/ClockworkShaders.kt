package org.valkyrienskies.clockwork

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.datafixers.util.Pair
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceProvider
import org.valkyrienskies.clockwork.client.render.ShaderReference
import java.io.IOException
import java.util.function.Consumer


object ClockworkShaders {
    /*
    private var crystal: ShaderInstance? = null

    @JvmStatic
    @Throws(IOException::class)
    fun init(
        resourceManager: ResourceProvider,
        registrations: Consumer<Pair<ShaderInstance, Consumer<ShaderInstance>>>
    ) {
        registrations.accept(
            Pair.of(
                ShaderInstance(resourceManager, "clockwork__crystal", DefaultVertexFormat.NEW_ENTITY),
                Consumer { inst -> crystal = inst }
            )
        )
    }

    fun crystal(): ShaderInstance {
        return crystal!!
    }

     */




    private val SHADERS: MutableList<ShaderReference> = ArrayList<ShaderReference>()
    val SCAN_EFFECT: ShaderReference = shader("scan_effect", DefaultVertexFormat.POSITION_TEX)
    val CRYSTAL_EFFECT: ShaderReference = shader("clockwork__crystal", DefaultVertexFormat.NEW_ENTITY)
    private fun shader(shader: String, format: VertexFormat): ShaderReference {
        val result = ShaderReference(shader, format)
        SHADERS.add(result)
        return result
    }

    fun reloadShaders(resources: ResourceProvider) {
        SHADERS.forEach(
            Consumer { shaderReference: ShaderReference ->
                shaderReference.reload(
                    resources
                )
            }
        )
    }
}