package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import java.util.Optional;
import java.util.function.Function;
import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.compat.gatemakyr.GateShaderPass;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;

@Pseudo
@Mixin(value = PipelineManager.class, remap = false)
public abstract class MixinGateIrisPipeline {
    @Shadow @Final private Function<NamespacedId, WorldRenderingPipeline> pipelineFactory;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void clockwork$enableGateCapture(CallbackInfo ci) {
        GatemakyrView.shaderCaptureSupported = true;
    }

    @Inject(method = "preparePipeline", at = @At("HEAD"), cancellable = true)
    private void clockwork$prepareGate(NamespacedId dimension, CallbackInfoReturnable<WorldRenderingPipeline> cir) {
        if (GatemakyrView.isolateShaders) cir.setReturnValue(GateShaderPass.prepare(dimension, pipelineFactory));
    }

    @Inject(method = "getPipelineNullable", at = @At("HEAD"), cancellable = true)
    private void clockwork$gatePipeline(CallbackInfoReturnable<WorldRenderingPipeline> cir) {
        if (GatemakyrView.isolateShaders && GateShaderPass.current() != null) cir.setReturnValue(GateShaderPass.current());
    }

    @Inject(method = "getPipeline", at = @At("HEAD"), cancellable = true)
    private void clockwork$findGatePipeline(CallbackInfoReturnable<Optional<WorldRenderingPipeline>> cir) {
        if (GatemakyrView.isolateShaders && GateShaderPass.current() != null) cir.setReturnValue(Optional.of(GateShaderPass.current()));
    }

    @Inject(method = "destroyPipeline", at = @At("HEAD"))
    private void clockwork$releaseGate(CallbackInfo ci) {
        GateShaderPass.clear();
    }
}
