package org.valkyrienskies.clockwork.mixin.content.sugar_rocket;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.content.propulsion.sugar_rocket.SugarRocketBlockEntity;

@Mixin(SmallFireball.class)
public class MixinFireCharge {

    @Inject(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/SmallFireball;getOwner()Lnet/minecraft/world/entity/Entity;", shift = At.Shift.AFTER), cancellable = true)
    void preOnHitBlock(BlockHitResult result, CallbackInfo ci) {
        if (((SmallFireball) (Object) this).level() != null) {
            Level level = ((SmallFireball) (Object) this).level();
            if (!level.isClientSide) {
                ServerLevel serverLevel = (ServerLevel) level;
                if (serverLevel.getBlockState(result.getBlockPos()).is(ClockworkBlocks.SUGAR_ROCKET.get())) {
                    SugarRocketBlockEntity sugarRocketBlockEntity =
                        (SugarRocketBlockEntity) serverLevel.getBlockEntity(result.getBlockPos());
                    if (sugarRocketBlockEntity != null) {
                        ci.cancel();
                    }
                }
            }
        }
    }
}
