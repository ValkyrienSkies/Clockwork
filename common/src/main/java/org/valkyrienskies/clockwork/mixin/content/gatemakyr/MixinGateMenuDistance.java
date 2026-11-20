package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrBlockInteraction;

@Mixin(Entity.class)
public abstract class MixinGateMenuDistance {
    @ModifyReturnValue(method = "distanceToSqr(DDD)D", at = @At("RETURN"))
    private double clockwork$gateMenuDistance(double original, double x, double y, double z) {
        if ((Object) this instanceof ServerPlayer player && GatemakyrBlockInteraction.menuPlayer() == player)
            return GatemakyrBlockInteraction.menuDistance(original, player, x, y, z);
        return original;
    }
}
