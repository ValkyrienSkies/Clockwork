package org.valkyrienskies.clockwork.mixin.content.schematics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.schematics.VdexConstruction;

@Mixin(ServerLevel.class)
public class MixinConstructionTicks {
    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void clockwork$blockTick(BlockPos pos, Block block, CallbackInfo ci) {
        if (VdexConstruction.locked((ServerLevel) (Object) this, pos)) ci.cancel();
    }
    @Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true)
    private void clockwork$fluidTick(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (VdexConstruction.locked((ServerLevel) (Object) this, pos)) ci.cancel();
    }
    @WrapOperation(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void clockwork$randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Operation<Void> original) {
        if (!VdexConstruction.locked(level, pos)) original.call(state, level, pos, random);
    }
}
