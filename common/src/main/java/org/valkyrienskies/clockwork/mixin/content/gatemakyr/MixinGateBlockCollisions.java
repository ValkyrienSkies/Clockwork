package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrSurfaceCollision;

import java.util.List;
import java.util.function.BiFunction;

@Mixin(BlockCollisions.class)
public abstract class MixinGateBlockCollisions {
    @Unique private List<AABB> clockwork$openings = List.of();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void clockwork$findOpenings(CollisionGetter level, Entity entity, AABB box, boolean suffocating, BiFunction<?, ?, ?> result, CallbackInfo ci) {
        if (entity != null) clockwork$openings = GatemakyrSurfaceCollision.openings(entity, box);
    }

    @WrapOperation(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape clockwork$clipBacking(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context, Operation<VoxelShape> original) {
        VoxelShape shape = original.call(state, level, pos, context);
        return GatemakyrSurfaceCollision.clipBacking(shape, pos.getX(), pos.getY(), pos.getZ(), clockwork$openings);
    }
}
