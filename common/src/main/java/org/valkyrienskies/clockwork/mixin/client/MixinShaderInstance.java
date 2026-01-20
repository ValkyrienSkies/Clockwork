package org.valkyrienskies.clockwork.mixin.client;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.FileUtil;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkMod;
import org.valkyrienskies.clockwork.ClockworkModClient;
import org.valkyrienskies.clockwork.mixinduck.MixinShaderInstanceDuck;
import org.valkyrienskies.clockwork.util.shader.ClockworkProgramType;
import org.valkyrienskies.clockwork.util.shader.ClockworkShaderProgram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Set;

@Mixin(ShaderInstance.class)
public class MixinShaderInstance implements MixinShaderInstanceDuck {

    @Unique
    private ClockworkShaderProgram cw$geometryProgram = null;
    @Unique
    private ClockworkShaderProgram cw$tessellationControlProgram = null;
    @Unique
    private ClockworkShaderProgram cw$tessellationEvaluationProgram = null;
    @Unique
    private ClockworkShaderProgram cw$computeProgram = null;

    @Override
    public ClockworkShaderProgram getGeometryProgram() {
        return cw$geometryProgram;
    }

    @Override
    public ClockworkShaderProgram getTessellationEvaluationProgram() {
        return cw$tessellationEvaluationProgram;
    }

    @Override
    public ClockworkShaderProgram getTessellationControlProgram() {
        return cw$tessellationControlProgram;
    }

    @Override
    public ClockworkShaderProgram getComputeProgram() {
        return cw$computeProgram;
    }

    @Override
    public Program cw$getOrCreate(ResourceProvider resourceProvider, ClockworkProgramType programType, String name) throws IOException {
        Program program2;
        Program program = programType.getPrograms().get(name);
        if (program == null) {
            String string = "shaders/core/" + name + programType.getExtension();
            Resource resource = resourceProvider.getResourceOrThrow(new ResourceLocation(ClockworkMod.MOD_ID, string));
            try (InputStream inputStream = resource.open()) {
                final String string2 = FileUtil.getFullResourcePath(string);
                program2 = ClockworkShaderProgram.compileShaderExtended(programType, name, inputStream, resource.sourcePackId(), new GlslPreprocessor() {
                    private final Set<String> importedPaths = Sets.newHashSet();

                    @Override
                    public String applyImport(boolean useFullPath, String directory) {
                        String string;
                        block9:
                        {
                            directory = FileUtil.normalizeResourcePath((useFullPath ? string2 : "shaders/include/") + directory);
                            if (!this.importedPaths.add(directory)) {
                                return null;
                            }
                            ResourceLocation resourceLocation = new ResourceLocation(directory);
                            BufferedReader reader = null;
                            try {
                                reader = resourceProvider.openAsReader(resourceLocation);
                            } catch (IOException e) {
                                ClockworkModClient.getMIXIN_LOGGER().error("Could not load shader program {}: {}", (Object) name, (Object) e.getMessage());
                                return "#error " + e.getMessage();
                            }
                            try {
                                string = IOUtils.toString(reader);
                                if (reader == null) break block9;
                            } catch (Throwable throwable) {
                                try {
                                    if (reader != null) {
                                        try {
                                            ((Reader) reader).close();
                                        } catch (Throwable throwable2) {
                                            throwable.addSuppressed(throwable2);
                                        }
                                    }
                                    throw throwable;
                                } catch (IOException iOException) {
                                    ClockworkModClient.getMIXIN_LOGGER().error("Could not open GLSL import {}: {}", (Object) directory, (Object) iOException.getMessage());
                                    return "#error " + iOException.getMessage();
                                }
                            }
                            try {
                                ((Reader) reader).close();
                            } catch (IOException e) {
                                ClockworkModClient.getMIXIN_LOGGER().error("Could not close GLSL import {}: {}", (Object) directory, (Object) e.getMessage());
                                return "#error " + e.getMessage();
                            }
                        }
                        return string;
                    }
                });
            }
        } else {
            program2 = program;
        }
        return program2;

    }

    @Inject(
            method = "attachToProgram",
            at = @At("TAIL")
    )
    private void afterAttachToProgram(CallbackInfo ci) {
        if (this.cw$geometryProgram != null) {
            this.cw$geometryProgram.attachToShader((Shader) (Object) this);
        }
        if (this.cw$tessellationControlProgram != null) {
            this.cw$tessellationControlProgram.attachToShader((Shader) (Object) this);
        }
        if (this.cw$tessellationEvaluationProgram != null) {
            this.cw$tessellationEvaluationProgram.attachToShader((Shader) (Object) this);
        }
        if (this.cw$computeProgram != null) {
            this.cw$computeProgram.attachToShader((Shader) (Object) this);
        }
    }

    @Inject(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;getOrCreate(Lnet/minecraft/server/packs/resources/ResourceProvider;Lcom/mojang/blaze3d/shaders/Program$Type;Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/Program;",
            ordinal = 1,
            shift = At.Shift.AFTER)
    )
    private void onInit(ResourceProvider resourceProvider, String name, VertexFormat vertexFormat, CallbackInfo ci) {
        try {
            this.cw$geometryProgram = (ClockworkShaderProgram) this.cw$getOrCreate(resourceProvider, ClockworkProgramType.GEOMETRY, name);
        } catch (IOException e) {
            ClockworkModClient.getMIXIN_LOGGER().error("Could not create geometry shader program {}: {}", (Object) name, (Object) e.getMessage());
        }
        try {
            this.cw$tessellationControlProgram = (ClockworkShaderProgram) this.cw$getOrCreate(resourceProvider, ClockworkProgramType.TESSELLATION_CONTROL, name);
        } catch (IOException e) {
            ClockworkModClient.getMIXIN_LOGGER().error("Could not create tessellation shader program {}: {}", (Object) name, (Object) e.getMessage());
        }
        try {
            this.cw$tessellationEvaluationProgram = (ClockworkShaderProgram) this.cw$getOrCreate(resourceProvider, ClockworkProgramType.TESSELLATION_EVALUATION, name);
        } catch (IOException e) {
            ClockworkModClient.getMIXIN_LOGGER().error("Could not create tessellation shader program {}: {}", (Object) name, (Object) e.getMessage());
        }

        try {
            this.cw$computeProgram = (ClockworkShaderProgram) this.cw$getOrCreate(resourceProvider, ClockworkProgramType.COMPUTE, name);
        } catch (IOException e) {
            ClockworkModClient.getMIXIN_LOGGER().error("Could not create compute shader program {}: {}", (Object) name, (Object) e.getMessage());
        }
    }
}
