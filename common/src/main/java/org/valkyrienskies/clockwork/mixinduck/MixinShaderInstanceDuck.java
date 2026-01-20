package org.valkyrienskies.clockwork.mixinduck;

import com.mojang.blaze3d.shaders.Program;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.valkyrienskies.clockwork.util.shader.ClockworkProgramType;
import org.valkyrienskies.clockwork.util.shader.ClockworkShaderProgram;

import java.io.IOException;

public interface MixinShaderInstanceDuck {

    public ClockworkShaderProgram getGeometryProgram();

    public ClockworkShaderProgram getTessellationEvaluationProgram();

    public ClockworkShaderProgram getTessellationControlProgram();

    public ClockworkShaderProgram getComputeProgram();

    public Program cw$getOrCreate(final ResourceProvider resourceProvider, ClockworkProgramType type, String name) throws IOException;
}
