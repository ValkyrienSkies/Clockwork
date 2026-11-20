package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.compat.gatemakyr.GateShaderHistory;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;

@Pseudo
@Mixin(targets = "net.irisshaders.iris.pipeline.IrisRenderingPipeline", remap = false)
public abstract class MixinGateIrisRenderTargets implements GateShaderHistory {
    @Shadow @Final private RenderTargets renderTargets;

    @Override
    public void clockwork$resetGateHistory() {
        ((GateIrisClearAccessor) (Object) renderTargets).clockwork$requireGateClear(true);
    }

    @Inject(method = "finalizeLevelRendering", at = @At("RETURN"))
    private void clockwork$playerHandDepth(CallbackInfo ci) {
        if (!GatemakyrView.isolateShaders) {
            GatemakyrView.beforeHandDepth = renderTargets.getDepthTextureNoHand().getTextureId();
            GatemakyrView.afterHandDepth = renderTargets.getDepthTextureNoTranslucents().getTextureId();
        }
    }
}
