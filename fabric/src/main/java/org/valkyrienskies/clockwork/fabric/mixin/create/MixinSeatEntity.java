package org.valkyrienskies.clockwork.fabric.mixin.create;

import com.simibubi.create.content.contraptions.components.actors.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(SeatEntity.class)
public abstract class MixinSeatEntity {
    @Unique
    private BlockPos firstPos;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At("TAIL"))
    private void injectHead(Level world, BlockPos pos, CallbackInfo ci) {
        firstPos = pos;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState redirectGetBlockState(Level instance, BlockPos pos) {
        if (firstPos != null) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(instance, firstPos);
            if (ship != null) {
                return instance.getBlockState(firstPos);
            }
        }
        return instance.getBlockState(pos);
    }
}