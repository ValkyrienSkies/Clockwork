package org.valkyrienskies.clockwork.util.shader

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.preprocessor.GlslPreprocessor
import com.mojang.blaze3d.shaders.Program
import com.mojang.blaze3d.systems.RenderSystem
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.jvm.Throws

class ClockworkShaderProgram(val cwType: ClockworkProgramType, id: Int, name: String) : Program(cwType.toMinecraft(), id, name) {
    companion object {
        @JvmStatic
        fun compileShaderExtended(cwType: ClockworkProgramType, name: String, shaderData: InputStream, sourceName: String, preprocessor: GlslPreprocessor): Program {
            if (cwType.isMinecraftType()) {
                return compileShader(cwType.toMinecraft(), name, shaderData, sourceName, preprocessor)
            }

        RenderSystem.assertOnRenderThread();
        val i = compileCWShaderInternal(cwType, name, shaderData, sourceName, preprocessor);
        val program = ClockworkShaderProgram(cwType, i, name);
        cwType.programs.put(name, program);
        return program;
        }

        @JvmStatic
        @Throws(IOException::class)
        protected fun compileCWShaderInternal(type: ClockworkProgramType, name: String, shaderData: InputStream, sourceName: String, preprocessor: GlslPreprocessor): Int {
            val string = IOUtils.toString(shaderData, StandardCharsets.UTF_8)
            if (string == null) {
                throw IOException("Could not load program " + type.typename)
            }
            val i = GlStateManager.glCreateShader(type.glType)
            GlStateManager.glShaderSource(i, preprocessor.process(string))
            GlStateManager.glCompileShader(i)
            if (GlStateManager.glGetShaderi(i, 35713) == 0) {
                val string2 = StringUtils.trim(GlStateManager.glGetShaderInfoLog(i, 32768))
                throw IOException("Couldn't compile " + type.typename + " program (" + sourceName + ", " + name + ") : " + string2)
            }
            return i
        }
    }

}
