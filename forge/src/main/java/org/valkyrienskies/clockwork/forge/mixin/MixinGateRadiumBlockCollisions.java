package org.valkyrienskies.clockwork.forge.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrSurfaceCollision;

import java.util.List;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.lithium.common.entity.movement.ChunkAwareBlockCollisionSweeper", remap = false)
public abstract class MixinGateRadiumBlockCollisions {
    @Unique private List<AABB> clockwork$openings = List.of();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void clockwork$findOpenings(Level level, Entity entity, AABB box, CallbackInfo ci) {
        if (entity != null) clockwork$openings = GatemakyrSurfaceCollision.openings(entity, box);
    }

    @ModifyArg(index = 2, method = "computeNext()Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At(value = "INVOKE",
        target = "Lme/jellysquid/mods/lithium/common/entity/movement/ChunkAwareBlockCollisionSweeper;getCollidedShape(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;III)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape clockwork$clipBacking(AABB box, VoxelShape entityShape, VoxelShape blockShape, int x, int y, int z) {
        return GatemakyrSurfaceCollision.clipBacking(blockShape, x, y, z, clockwork$openings);
    }
}
