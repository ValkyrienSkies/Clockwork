package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrView;

@Pseudo
@Mixin(targets = "dev.engine_room.flywheel.backend.engine.uniform.FrameUniforms", remap = false)
public abstract class MixinGateFlywheelCullData {
    @ModifyConstant(method = "writeCullData", constant = @Constant(floatValue = 0.05f), require = 0)
    private static float clockwork$skipPortalOcclusion(float near) {
        return GatemakyrView.target != null && !GatemakyrView.isolateShaders ? Float.MAX_VALUE : near;
    }
}
