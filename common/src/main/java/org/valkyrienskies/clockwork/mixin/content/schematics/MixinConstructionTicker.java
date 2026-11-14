package org.valkyrienskies.clockwork.mixin.content.schematics;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.schematics.VdexConstruction;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public class MixinConstructionTicker {
    @Shadow @Final private BlockEntity blockEntity;
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void clockwork$holdMachine(CallbackInfo ci) {
        if (VdexConstruction.locked(blockEntity.getLevel(), blockEntity.getBlockPos())) ci.cancel();
    }
}
