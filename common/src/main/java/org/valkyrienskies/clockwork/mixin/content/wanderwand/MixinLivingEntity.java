package org.valkyrienskies.clockwork.mixin.content.wanderwand;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.ClockworkModClient;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;shouldDiscardFriction()Z"))
    private boolean clockwork$keepBindMomentum(boolean original) {
        // The rope applies its own light tangential drag after travel. Keep vanilla movement,
        // gravity and collision, including ordinary friction when grounded or obstructed.
        return original || (Object) this instanceof LocalPlayer player &&
                ClockworkModClient.getWANDERWAND_EFFECT_RENDERER().usesSwingDrag(player);
    }
}
