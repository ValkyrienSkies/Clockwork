package org.valkyrienskies.clockwork.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrBlockInteraction;

@Mixin(ServerPlayerGameMode.class)
public abstract class MixinGateMiningReach {
    @ModifyExpressionValue(method = "handleBlockBreakAction", at = {
        @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D")
    })
    private double clockwork$gateReach(double original) {
        return GatemakyrBlockInteraction.distanceSquared(original);
    }
}
