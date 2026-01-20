package org.valkyrienskies.clockwork.mixin.client;

import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Shader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.mixinduck.MixinShaderInstanceDuck;

@Mixin(ProgramManager.class)
public class MixinProgramManager {
    @Inject(method = "releaseProgram", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Program;close()V", ordinal = 1))
    private static void onReleaseProgram(Shader shader, CallbackInfo ci) {
        if (shader instanceof MixinShaderInstanceDuck duck) {
            Program geo = duck.getGeometryProgram();
            if (geo != null) {
                geo.close();
            }
            Program tess = duck.getTessellationEvaluationProgram();
            if (tess != null) {
                tess.close();
            }
            Program tessCtrl = duck.getTessellationControlProgram();
            if (tessCtrl != null) {
                tessCtrl.close();
            }
            Program compute = duck.getComputeProgram();
            if (compute != null) {
                compute.close();
            }
        }
    }
}
