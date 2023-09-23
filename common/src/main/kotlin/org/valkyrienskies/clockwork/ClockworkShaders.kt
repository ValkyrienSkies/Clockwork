package org.valkyrienskies.clockwork

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.server.packs.resources.ResourceProvider
import org.valkyrienskies.clockwork.client.render.ShaderReference
import java.util.function.Consumer

object ClockworkShaders {
    private val SHADERS: MutableList<ShaderReference> = ArrayList<ShaderReference>()
    val SCAN_EFFECT: ShaderReference = shader("scan_effect", DefaultVertexFormat.POSITION_TEX)
    private fun shader(shader: String, format: VertexFormat): ShaderReference {
        val result = ShaderReference(shader, format)
        SHADERS.add(result)
        return result
    }

    fun reloadShaders(resources: ResourceProvider) {
        SHADERS.forEach(Consumer<ShaderReference> { shaderReference: ShaderReference ->
            shaderReference.reload(
                resources
            )
        })
    }
}