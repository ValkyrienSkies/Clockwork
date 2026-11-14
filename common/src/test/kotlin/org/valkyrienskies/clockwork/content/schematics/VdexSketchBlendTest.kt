package org.valkyrienskies.clockwork.content.schematics

import com.google.gson.JsonParser
import com.mojang.blaze3d.shaders.BlendMode
import net.minecraft.client.renderer.ShaderInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14

class VdexSketchBlendTest {
    @Test
    fun `blueprint shaders preserve the vanilla blend cache key used by vignette rendering`() {
        val vanilla = BlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL14.GL_FUNC_ADD)
        for (name in listOf("sketch", "ghost", "ink")) {
            val path = "/assets/minecraft/shaders/core/clockwork_blueprint_$name.json"
            val definition = requireNotNull(javaClass.getResourceAsStream(path)).bufferedReader().use {
                JsonParser.parseReader(it).asJsonObject
            }
            assertEquals(vanilla, ShaderInstance.parseBlendNode(definition.getAsJsonObject("blend")), name)
        }
    }
}
