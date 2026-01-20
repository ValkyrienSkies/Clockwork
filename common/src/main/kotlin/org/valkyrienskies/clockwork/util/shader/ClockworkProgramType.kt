package org.valkyrienskies.clockwork.util.shader

import com.google.common.collect.Maps
import com.mojang.blaze3d.shaders.Program
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

@Environment(value = EnvType.CLIENT)
enum class ClockworkProgramType(val typename: String, val extension: String, val glType: Int) {
    VERTEX("vertex", ".vsh", 35633),
    FRAGMENT("fragment", ".fsh", 35632),
    GEOMETRY("geometry", ".gsh", 36313),
    TESSELLATION_EVALUATION("tessellation", ".tesh", 36487),
    TESSELLATION_CONTROL("tessellation_control", ".tcsh", 36488),
    COMPUTE("compute", ".comp", 37305);

    val programs: MutableMap<String, Program> = Maps.newHashMap<String, Program>()

    fun toMinecraft(): Program.Type {
        return when (this) {
            VERTEX -> Program.Type.VERTEX
            FRAGMENT -> Program.Type.FRAGMENT
            else -> Program.Type.VERTEX // Placeholder for other types bc mc doesnt support them in the base program
        }
    }

    fun isMinecraftType(): Boolean {
        return this == VERTEX || this == FRAGMENT
    }
}
