package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {"net.irisshaders.iris.uniforms.SystemTimeUniforms$FrameCounter", "net.irisshaders.iris.uniforms.SystemTimeUniforms$Timer"}, remap = false)
public abstract class MixinGateIrisFrameTime {
    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true)
    private void clockwork$keepPlayerFrameTime(CallbackInfo ci) {
        if (GatemakyrView.isolateShaders) ci.cancel();
    }
}
