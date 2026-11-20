package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrRaycast;
import org.valkyrienskies.mod.common.world.RaycastUtilsKt;

@Mixin(value = RaycastUtilsKt.class, remap = false)
public abstract class MixinGateRaycast {
    @Inject(target = @Desc(value = "clipIncludeShips", args = {Level.class, ClipContext.class, boolean.class, Long.class, boolean.class}, ret = BlockHitResult.class), at = @At("HEAD"), cancellable = true)
    private static void clockwork$traceGates(Level level, ClipContext ctx, boolean transform, Long skipShip, boolean skipWorld, CallbackInfoReturnable<BlockHitResult> cir) {
        BlockHitResult hit = GatemakyrRaycast.clip(level, ctx, transform, skipShip, skipWorld);
        if (hit != null) cir.setReturnValue(hit);
    }
}
