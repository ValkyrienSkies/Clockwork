package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.gl.program.ProgramUniforms", remap = false)
public abstract class MixinGateIrisUniforms {
    @ModifyExpressionValue(method = "update", at = @At(value = "INVOKE",
        target = "Lnet/irisshaders/iris/uniforms/SystemTimeUniforms$FrameCounter;getAsInt()I"))
    private int clockwork$gateUniformUpdate(int frame) {
        return GatemakyrView.isolateShaders ? Integer.MIN_VALUE + GatemakyrView.captureSerial : frame;
    }
}
