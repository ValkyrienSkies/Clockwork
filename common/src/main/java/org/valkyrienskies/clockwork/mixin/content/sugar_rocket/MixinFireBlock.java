package org.valkyrienskies.clockwork.mixin.content.sugar_rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.ClockworkBlocks;
import org.valkyrienskies.clockwork.content.propulsion.sugar_rocket.SugarRocketBlock;
import org.valkyrienskies.clockwork.content.propulsion.sugar_rocket.SugarRocketBlockEntity;

import java.util.Random;

@Mixin(FireBlock.class)
public class MixinFireBlock {
    @Inject(method = "checkBurnOut", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;", shift = At.Shift.AFTER))
    void vs_clockwork$checkBurnOut(Level level, BlockPos pos, int chance, Random random, int age, CallbackInfo ci) {
        if (level.getBlockState(pos).is(ClockworkBlocks.SUGAR_ROCKET.get())) {
            if (!level.isClientSide) {
                SugarRocketBlockEntity sugarRocketBlockEntity =
                    (SugarRocketBlockEntity) level.getBlockEntity(pos);
                if (sugarRocketBlockEntity != null) {
                    sugarRocketBlockEntity.induceIgnition();
                }
            }
        }
    }

}
