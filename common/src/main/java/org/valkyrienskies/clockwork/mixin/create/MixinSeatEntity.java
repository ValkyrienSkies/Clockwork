package org.valkyrienskies.clockwork.mixin.create;

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
    @Unique
    private Level injectedLevel;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V", at = @At("TAIL"))
    private void injectHead(Level world, BlockPos pos, CallbackInfo ci) {
        firstPos = pos;
        injectedLevel = world;
    }

/* TODO not working rn fix

    @ModifyArgs(method = "setPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;<init>(DDD)V"))
    private void modNewVec3(Args args) {
        modPos(args);
    }

    @ModifyArgs(method = "setPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"))
    private void modSetPos(Args args) {
        modPos(args);
    }

    @Unique
    private void modPos(Args args) {
        if (firstPos != null && injectedLevel != null) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(injectedLevel, firstPos);
            if (ship != null) {
                args.set(0, (double) firstPos.getX() + .5);
                args.set(1, (double) firstPos.getY());
                args.set(2, (double) firstPos.getZ() + .5);
            }
        }
    }
 */

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