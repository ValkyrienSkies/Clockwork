package org.valkyrienskies.clockwork.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.properties.IShipActiveChunksSet;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.internal.collision.VsiConvexPolygonc;
import org.valkyrienskies.core.internal.collision.VsiEntityPolygonCollider;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.EntityShipCollisionUtils;
import org.valkyrienskies.mod.util.BugFixUtil;

import java.util.ArrayList;
import java.util.List;

@Mixin(EntityShipCollisionUtils.class)
public abstract class MixinEntityShipCollisionUtils {

    @Shadow
    @Final
    private static VsiEntityPolygonCollider collider;

    @Inject(method = "getShipPolygonsCollidingWithEntity", at = @At("HEAD"), cancellable = true)
    private void clockwork$optimizeGetShipPolygonsCollidingWithEntity(
        final Entity entity,
        final Vec3 movement,
        final AABB entityBoundingBox,
        final Level world,
        final CallbackInfoReturnable<List<VsiConvexPolygonc>> cir
    ) {
        final AABB entityBoxWithMovement = entityBoundingBox.expandTowards(movement);
        final AABBd entityBoxWithMovementJoml = new AABBd(
            entityBoxWithMovement.minX, entityBoxWithMovement.minY, entityBoxWithMovement.minZ,
            entityBoxWithMovement.maxX, entityBoxWithMovement.maxY, entityBoxWithMovement.maxZ
        );

        final AABBd entityBoundingBoxExtended = new AABBd(entityBoxWithMovementJoml);
        final String dimensionId = VSGameUtilsKt.getDimensionId(world);

        final Iterable<LoadedShip> intersectingShips =
            VSGameUtilsKt.getShipObjectWorld(world).getLoadedShips().getIntersecting(entityBoundingBoxExtended, dimensionId);

        final ArrayList<VsiConvexPolygonc> collidingPolygons = new ArrayList<>();
        final AABBd entityBoundingBoxInShipCoordinatesMutable = new AABBd();
        final AABBd blockBoxMutable = new AABBd();
        final Vector3d tmpTransformPos = new Vector3d();

        for (final LoadedShip shipObject : intersectingShips) {
            final ShipTransform shipTransform = shipObject.getTransform();
            final Matrix4dc shipToWorld = shipTransform.getShipToWorld();
            final long shipId = shipObject.getId();

            transformAabb(entityBoxWithMovementJoml, shipTransform.getWorldToShip(), entityBoundingBoxInShipCoordinatesMutable,
                tmpTransformPos);
            final AABBdc entityBoundingBoxInShipCoordinates = entityBoundingBoxInShipCoordinatesMutable;

            final AABBic shipAABB = shipObject.getShipAABB();
            if (shipAABB != null && shipAABB.isValid()) {
                if (!intersects(entityBoundingBoxInShipCoordinates, shipAABB)) {
                    continue;
                }
            }

            final AABB entityBoundingBoxInShipCoordinatesMinecraft = new AABB(
                entityBoundingBoxInShipCoordinates.minX(),
                entityBoundingBoxInShipCoordinates.minY(),
                entityBoundingBoxInShipCoordinates.minZ(),
                entityBoundingBoxInShipCoordinates.maxX(),
                entityBoundingBoxInShipCoordinates.maxY(),
                entityBoundingBoxInShipCoordinates.maxZ()
            );

            if (BugFixUtil.INSTANCE.isCollisionBoxTooBig(entityBoundingBoxInShipCoordinatesMinecraft)) {
                continue;
            }

            if (!intersectsAnyActiveChunk(shipObject.getActiveChunksSet(), entityBoundingBoxInShipCoordinates)) {
                continue;
            }

            final Iterable<VoxelShape> shipBlockCollisionStream =
                world.getBlockCollisions(entity, entityBoundingBoxInShipCoordinatesMinecraft);

            shipBlockCollisionStream.forEach(voxelShape -> voxelShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
                blockBoxMutable.minX = minX;
                blockBoxMutable.minY = minY;
                blockBoxMutable.minZ = minZ;
                blockBoxMutable.maxX = maxX;
                blockBoxMutable.maxY = maxY;
                blockBoxMutable.maxZ = maxZ;
                final VsiConvexPolygonc shipPolygon = collider.createPolygonFromAABB(blockBoxMutable, shipToWorld, shipId);
                collidingPolygons.add(shipPolygon);
            }));
        }

        cir.setReturnValue(collidingPolygons);
    }

    private static boolean intersects(final AABBdc box, final AABBic shipAABB) {
        return box.maxX() >= shipAABB.minX() && box.minX() <= shipAABB.maxX() &&
            box.maxY() >= shipAABB.minY() && box.minY() <= shipAABB.maxY() &&
            box.maxZ() >= shipAABB.minZ() && box.minZ() <= shipAABB.maxZ();
    }

    private static void transformAabb(final AABBd box, final Matrix4dc matrix, final AABBd dest, final Vector3d tmp) {
        final double inMinX = box.minX;
        final double inMinY = box.minY;
        final double inMinZ = box.minZ;
        final double inMaxX = box.maxX;
        final double inMaxY = box.maxY;
        final double inMaxZ = box.maxZ;

        double outMinX = Double.POSITIVE_INFINITY;
        double outMinY = Double.POSITIVE_INFINITY;
        double outMinZ = Double.POSITIVE_INFINITY;
        double outMaxX = Double.NEGATIVE_INFINITY;
        double outMaxY = Double.NEGATIVE_INFINITY;
        double outMaxZ = Double.NEGATIVE_INFINITY;

        // Eight corners of the AABB
        matrix.transformPosition(inMinX, inMinY, inMinZ, tmp);
        double tx = tmp.x, ty = tmp.y, tz = tmp.z;
        if (tx < outMinX) outMinX = tx;
        if (ty < outMinY) outMinY = ty;
        if (tz < outMinZ) outMinZ = tz;
        if (tx > outMaxX) outMaxX = tx;
        if (ty > outMaxY) outMaxY = ty;
        if (tz > outMaxZ) outMaxZ = tz;

        matrix.transformPosition(inMinX, inMinY, inMaxZ, tmp);
        tx = tmp.x;
        ty = tmp.y;
        tz = tmp.z;
        if (tx < outMinX) outMinX = tx;
        if (ty < outMinY) outMinY = ty;
        if (tz < outMinZ) outMinZ = tz;
        if (tx > outMaxX) outMaxX = tx;
        if (ty > outMaxY) outMaxY = ty;
        if (tz > outMaxZ) outMaxZ = tz;

        matrix.transformPosition(inMinX, inMaxY, inMinZ, tmp);
        tx = tmp.x;
        ty = tmp.y;
        tz = tmp.z;
        if (tx < outMinX) outMinX = tx;
        if (ty < outMinY) outMinY = ty;
        if (tz < outMinZ) outMinZ = tz;
        if (tx > outMaxX) outMaxX = tx;
        if (ty > outMaxY) outMaxY = ty;
        if (tz > outMaxZ) outMaxZ = tz;

        matrix.transformPosition(inMinX, inMaxY, inMaxZ, tmp);
        tx = tmp.x;
        ty = tmp.y;
        tz = tmp.z;
        if (tx < outMinX) outMinX = tx;
        if (ty < outMinY) outMinY = ty;
        if (tz < outMinZ) outMinZ = tz;
        if (tx > outMaxX) outMaxX = tx;
        if (ty > outMaxY) outMaxY = ty;
        if (tz > outMaxZ) outMaxZ = tz;

        matrix.transformPosition(inMaxX, inMinY, inMinZ, tmp);
        tx = tmp.x;
        ty = tmp.y;
        tz = tmp.z;
        if (tx < outMinX) outMinX = tx;
        if (ty < outMinY) outMinY = ty;
        if (tz < outMinZ) outMinZ = tz;
        if (tx > outMaxX) outMaxX = tx;
        if (ty > outMaxY) outMaxY = ty;
        if (tz > outMaxZ) outMaxZ = tz;

        matrix.transformPosition(inMaxX, inMinY, inMaxZ, tmp);
        tx = tmp.x;
        ty = tmp.y;
        tz = tmp.z;
        if (tx < outMinX) outMinX = tx;
        if (ty < outMinY) outMinY = ty;
        if (tz < outMinZ) outMinZ = tz;
        if (tx > outMaxX) outMaxX = tx;
        if (ty > outMaxY) outMaxY = ty;
        if (tz > outMaxZ) outMaxZ = tz;

        matrix.transformPosition(inMaxX, inMaxY, inMinZ, tmp);
        tx = tmp.x;
        ty = tmp.y;
        tz = tmp.z;
        if (tx < outMinX) outMinX = tx;
        if (ty < outMinY) outMinY = ty;
        if (tz < outMinZ) outMinZ = tz;
        if (tx > outMaxX) outMaxX = tx;
        if (ty > outMaxY) outMaxY = ty;
        if (tz > outMaxZ) outMaxZ = tz;

        matrix.transformPosition(inMaxX, inMaxY, inMaxZ, tmp);
        tx = tmp.x;
        ty = tmp.y;
        tz = tmp.z;
        if (tx < outMinX) outMinX = tx;
        if (ty < outMinY) outMinY = ty;
        if (tz < outMinZ) outMinZ = tz;
        if (tx > outMaxX) outMaxX = tx;
        if (ty > outMaxY) outMaxY = ty;
        if (tz > outMaxZ) outMaxZ = tz;

        dest.minX = outMinX;
        dest.minY = outMinY;
        dest.minZ = outMinZ;
        dest.maxX = outMaxX;
        dest.maxY = outMaxY;
        dest.maxZ = outMaxZ;
    }

    private static boolean intersectsAnyActiveChunk(final IShipActiveChunksSet activeChunksSet, final AABBdc aabb) {
        final int minX = (Mth.floor(aabb.minX() - 1.0E-7) - 1) >> 4;
        final int maxX = (Mth.floor(aabb.maxX() + 1.0E-7) + 1) >> 4;
        final int minZ = (Mth.floor(aabb.minZ() - 1.0E-7) - 1) >> 4;
        final int maxZ = (Mth.floor(aabb.maxZ() + 1.0E-7) + 1) >> 4;

        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                if (activeChunksSet.contains(chunkX, chunkZ)) {
                    return true;
                }
            }
        }
        return false;
    }
}
