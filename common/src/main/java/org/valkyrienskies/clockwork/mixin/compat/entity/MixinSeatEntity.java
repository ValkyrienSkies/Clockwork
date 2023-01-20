package org.valkyrienskies.clockwork.mixin.compat.entity;

import com.simibubi.create.content.contraptions.components.actors.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(SeatEntity.class)
public abstract class MixinSeatEntity extends Entity {

    public MixinSeatEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /*
TODO YES I DID IT AGAIN :mental

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
}