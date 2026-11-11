package org.valkyrienskies.clockwork.mixin.content.lubricant;

import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantMaterials;
import org.valkyrienskies.core.internal.physics.blockstates.VsiBlockState;
import org.valkyrienskies.mod.common.config.MassDatapackResolver;

@Mixin(value = MassDatapackResolver.class, remap = false)
public class MixinMassDatapackResolver {
    @Inject(method = "getBlockStateData", at = @At("RETURN"), cancellable = true)
    private void clockwork$lubricatedMaterials(CallbackInfoReturnable<Collection<VsiBlockState>> cir) {
        cir.setReturnValue(LubricantMaterials.register(cir.getReturnValue()));
    }
}
