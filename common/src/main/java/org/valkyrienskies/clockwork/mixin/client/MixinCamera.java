package org.valkyrienskies.clockwork.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public class MixinCamera {

    @Shadow private BlockGetter level;

    @WrapOperation(method = "getFluidInCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockGetter;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"))
    private FluidState clockwork$submarineCamera(BlockGetter instance, BlockPos pos, Operation<FluidState> original) {
        var v = this.level.getFluidState(pos);
        if (v.is(FluidTags.WATER)) {
            //return Fluids.EMPTY.defaultFluidState();
        }
        return original.call(instance, pos);
    }
}
