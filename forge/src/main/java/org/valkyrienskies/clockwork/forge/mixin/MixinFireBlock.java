package org.valkyrienskies.clockwork.forge.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.content.propulsion.sugar_rocket.SugarRocketBlockEntity;

@Mixin(FireBlock.class)
public class MixinFireBlock {
    @Inject(method = "tryCatchFire", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onCaughtFire(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/LivingEntity;)V", shift = At.Shift.AFTER))
    void vs_clockwork$checkBurnOut(Level arg, BlockPos arg2, int k, RandomSource arg3, int l, Direction face, CallbackInfo ci) {
        if (arg.getBlockState(arg2).is(ClockworkBlocks.SUGAR_ROCKET.get())) {
            if (!arg.isClientSide) {
                SugarRocketBlockEntity sugarRocketBlockEntity =
                        (SugarRocketBlockEntity) arg.getBlockEntity(arg2);
                if (sugarRocketBlockEntity != null) {
                    sugarRocketBlockEntity.induceIgnition();
                }
            }
        }
    }
}
