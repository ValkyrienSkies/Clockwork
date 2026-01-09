package org.valkyrienskies.clockwork.mixin.content.sugar_rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.propulsion.sugar_rocket.SugarRocketBlockEntity;

@Mixin(targets = "net/minecraft/core/dispenser/DispenseItemBehavior$18")
public abstract class MixinDispenseItemBehavior extends OptionalDispenseItemBehavior {

    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/dispenser/DispenseItemBehavior$18;setSuccess(Z)V", shift = At.Shift.AFTER))
    void lightSugarRocket(BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        ServerLevel level = source.getLevel();
        Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
        BlockPos blockPos = source.getPos().relative(direction);

        if (level.getBlockEntity(blockPos) instanceof SugarRocketBlockEntity be) {
            be.induceIgnition();
            setSuccess(true);
        }
    }
}
