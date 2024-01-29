package org.valkyrienskies.clockwork.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.stream.Stream;

@Mixin(Entity.class)
public class MixinEntity {

    @Shadow @Final private Set<TagKey<Fluid>> fluidOnEyes;

    @WrapOperation(method = "updateFluidOnEyes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState clockwork$submarine(Level level, BlockPos pos, Operation<FluidState> operation){

        Entity entity = (Entity) (Object) this;
        if (entity instanceof Player player) {
            if (player.isShiftKeyDown()) {
                this.fluidOnEyes.clear();
                return Fluids.EMPTY.defaultFluidState();//operation.call(level, pos);
            }
        }
        return operation.call(level, pos);
    }

    @WrapOperation(method = "updateFluidOnEyes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getTags()Ljava/util/stream/Stream;"))
    private Stream<TagKey<Fluid>> d(FluidState instance, Operation<Stream<TagKey<Fluid>>> original){
        //System.out.println(instance);
        return original.call(instance);
    }

    @Inject(method = "isInWater", at = @At("RETURN"))
    private void s(CallbackInfoReturnable<Boolean> cir){
        Entity entity = (Entity) (Object) this;
        if (isInAirPocket(entity)) {
            cir.setReturnValue(false);
        } else {
            cir.setReturnValue(cir.getReturnValue());
        }
    }

    @WrapOperation(method = "isUnderWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInWater()Z"))
    private boolean s(Entity instance, Operation<Boolean> original){
        if (isInAirPocket(instance)) {
            return false;
        } else {
            return original.call(instance);
        }
    }
}
