package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.uniform.DynamicUniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
public abstract class MixinGateIrisCameraUniforms {
    @Inject(method = "addDynamicUniforms", at = @At("RETURN"))
    private static void clockwork$gateExit(DynamicUniformHolder uniforms, FogMode fog, CallbackInfo ci) {
        if (GatemakyrView.isolateShaders) {
            uniforms.uniform4f(UniformUpdateFrequency.PER_FRAME, "clockworkGateExitPlane", () -> GatemakyrView.exitClip);
        }
    }
}
