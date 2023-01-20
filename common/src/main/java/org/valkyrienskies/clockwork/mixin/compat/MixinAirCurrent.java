package org.valkyrienskies.clockwork.mixin.compat;

import com.simibubi.create.content.contraptions.components.fan.AirCurrent;
import com.simibubi.create.content.contraptions.components.fan.IAirCurrentSource;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import org.valkyrienskies.mod.common.world.RaycastUtilsKt;

import java.util.Iterator;
import java.util.List;

@Mixin(AirCurrent.class)
public abstract class MixinAirCurrent {

    @Unique
    private final float maxAcceleration = 5;
    @Shadow
    @Final
    public IAirCurrentSource source;
    @Unique
    private Vec3 transformedFlow = Vec3.ZERO;
    @Unique
    private float acceleration;
    @Unique
    private Ship ship;

    @Redirect(method = "getFlowLimit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;clipWithInteractionOverride(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private static BlockHitResult redirectClip(Level instance, Vec3 vec3, Vec3 vec32, BlockPos blockPos, VoxelShape voxelShape, BlockState blockState) {
        return RaycastUtilsKt.clipIncludeShips(instance, new ClipContext(vec3, vec32, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, null));
    }

    @Redirect(method = "findEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> redirectFindEntities(Level instance, Entity entity, AABB aabb) {
        return instance.getEntities(entity, VSGameUtilsKt.transformAabbToWorld(instance, aabb));
    }

    @Redirect(method = "tickAffectedEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;intersects(Lnet/minecraft/world/phys/AABB;)Z"))
    private boolean redirectIntersects(AABB instance, AABB other) {
        Level level = this.source.getAirCurrentWorld();
        if (level != null) {
            Iterator<Ship> ships = VSGameUtilsKt.getShipsIntersecting(level, instance).iterator();
            if (ships.hasNext()) {
                AABBd result = new AABBd();
                VectorConversionsMCKt.toJOML(instance).transform(ships.next().getTransform().getWorldToShip(), result);
                instance = VectorConversionsMCKt.toMinecraft(result);
            }
        }
        return instance.intersects(other);
    }

    @Inject(
            method = "tickAffectedEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void harvester(Level world, Direction facing, CallbackInfo ci, Iterator<Entity> iterator, Entity entity, Vec3 center, Vec3i flow, float sneakModifier, float speed, double entityDistance, float acceleration) {
        ship = VSGameUtilsKt.getShipManagingPos(world, source.getAirCurrentPos());
        if (ship != null) {
            Vector3d tempVec = new Vector3d();
            ship.getTransform().getShipToWorld().transformDirection(flow.getX(), flow.getY(), flow.getZ(), tempVec);
            transformedFlow = VectorConversionsMCKt.toMinecraft(tempVec);
        }
        this.acceleration = acceleration;
    }

    @Redirect(method = "tickAffectedEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V")
    )
    private void redirectSetDeltaMovement(Entity instance, Vec3 motion) {
        if (ship != null) {
            Vec3 previousMotion = instance.getDeltaMovement();
            double xIn = Mth.clamp(transformedFlow.x * acceleration - previousMotion.x, -maxAcceleration, maxAcceleration);
            double yIn = Mth.clamp(transformedFlow.y * acceleration - previousMotion.y, -maxAcceleration, maxAcceleration);
            double zIn = Mth.clamp(transformedFlow.z * acceleration - previousMotion.z, -maxAcceleration, maxAcceleration);
            instance.setDeltaMovement(previousMotion.add(new Vec3(xIn, yIn, zIn).scale(1 / 8f)));
        } else {
            instance.setDeltaMovement(motion);
        }
    }

    @Redirect(method = "tickAffectedEntities", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/utility/VecHelper;getCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"), allow = 1)
    private Vec3 redirectGetCenterOf(Vec3i pos) {
        Vec3 result = VecHelper.getCenterOf(pos);
        if (this.source.getAirCurrentWorld() != null) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(this.source.getAirCurrentWorld(), result);
            if (ship != null) {
                Vector3d tempVec = new Vector3d();
                ship.getTransform().getShipToWorld().transformPosition(result.x, result.y, result.z, tempVec);
                result = VectorConversionsMCKt.toMinecraft(tempVec);
            }
        }
        return result;
    }
}