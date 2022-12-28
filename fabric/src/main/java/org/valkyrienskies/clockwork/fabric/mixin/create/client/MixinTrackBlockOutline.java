package org.valkyrienskies.clockwork.fabric.mixin.create.client;

import com.simibubi.create.content.logistics.trains.track.TrackBlockOutline;
import com.simibubi.create.content.logistics.trains.track.TrackTileEntity;
import com.simibubi.create.foundation.utility.RaycastHelper;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(TrackBlockOutline.class)
public class MixinTrackBlockOutline {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("VS2 create.client.MixinTrackBlockOutline");

    private static String printVec3(final Vec3 vec3) {
        return "Vec3[x=" + String.format("%.2f", vec3.x) + ", y=" + String.format("%.2f", vec3.y) + " z=" +
            String.format("%.2f", vec3.z) + "]";
    }

    private static String printVector3d(final Vector3d vector3d) {
        return "Vector3d[x=" + String.format("%.2f", vector3d.x) + ", y=" + String.format("%.2f", vector3d.y) + " z=" +
            String.format("%.2f", vector3d.z) + "]";
    }

    @Unique
    private static boolean isShip = false;
    @Unique
    private static BlockPos shipBlockPos;

    @Inject(
        method = "pickCurves",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"
        ), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void stuff(final CallbackInfo ci, final Minecraft mc) {
        if (mc.hitResult != null && mc.level != null && mc.hitResult.getType() == Type.BLOCK) {
            shipBlockPos = ((BlockHitResult) mc.hitResult).getBlockPos();

            final List<Vector3d>
                ships = VSGameUtilsKt.transformToNearbyShipsAndWorld(mc.level, shipBlockPos.getX(), shipBlockPos.getY(),
                shipBlockPos.getZ(), 10);
            //LOGGER.warn("ships.isEmpty() " + ships.isEmpty());
            isShip = !ships.isEmpty(); //VSGameUtilsKt.isBlockInShipyard(mc.level, shipBlockPos);
        }
    }

    @Redirect(
        method = "pickCurves()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private static Vec3 redirectedOrigin(final LocalPlayer instance, final float v) {
        final Vec3 eyePos = instance.getEyePosition(v);
        if (isShip) {
            final List<Vector3d>
                ships = VSGameUtilsKt.transformToNearbyShipsAndWorld(instance.level, eyePos.x, eyePos.y, eyePos.z, 10);
            if (ships.isEmpty()) {
                return eyePos;
            }

            final Vector3d tempVec = ships.get(0);
            return new Vec3(tempVec.x, tempVec.y, tempVec.z);
        } else {
            return eyePos;
        }
    }

    @Redirect(
        method = "pickCurves()V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/utility/RaycastHelper;getTraceTarget(Lnet/minecraft/world/entity/player/Player;DLnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private static Vec3 redirectedTarget(final Player playerIn, final double range, final Vec3 origin) {
        if (isShip) {
            return new Vec3(shipBlockPos.getX(), shipBlockPos.getY(), shipBlockPos.getZ());
        } else {
            return RaycastHelper.getTraceTarget(playerIn, range, origin);
        }
    }

    @Inject(
        method = "pickCurves",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lcom/simibubi/create/foundation/utility/WorldAttached;get(Lnet/minecraft/world/level/LevelAccessor;)Ljava/lang/Object;",
            shift = At.Shift.BY, by = 5
        ), locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static void debug(final CallbackInfo ci, final Minecraft mc, final LocalPlayer player, final Vec3 origin,
        final double maxRange,
        final double range, final Vec3 target, final Map<BlockPos, TrackTileEntity> turns) {
        //LOGGER.warn("pickCurves-- isShip?" + isShip);
        /*
        BezierPointSelection result;
        LOGGER.warn("Stage1");
        for (final TrackTileEntity te : turns.values()) {
            LOGGER.warn("Stage2");
            for (final BezierConnection bc : te.getConnections().values()) {
                LOGGER.warn("Stage3");
                if (!bc.isPrimary()) {
                    continue;
                }
                LOGGER.warn("Stage4");

                final AABB bounds = bc.getBounds();
                if (!bounds.contains(origin) && bounds.clip(origin, target)
                    .isEmpty()) {
                    continue;
                }
                LOGGER.warn("Stage5");

                final float[] stepLUT = bc.getStepLUT();
                final int segments = (int) (bc.getLength() * 2);
                AABB segmentBounds = AllShapes.TRACK_ORTHO.get(Direction.SOUTH)
                    .bounds();
                segmentBounds = segmentBounds.move(-.5, segmentBounds.getYsize() / -2, -.5);

                int bestSegment = -1;
                double bestDistance = Double.MAX_VALUE;
                double newMaxRange = maxRange;

                for (int i = 0; i < stepLUT.length - 2; i++) {
                    LOGGER.warn("Stage6");
                    final float t = stepLUT[i] * i / segments;
                    final float t1 = stepLUT[i + 1] * (i + 1) / segments;
                    final float t2 = stepLUT[i + 2] * (i + 2) / segments;

                    final Vec3 v1 = bc.getPosition(t);
                    final Vec3 v2 = bc.getPosition(t2);
                    final Vec3 diff = v2.subtract(v1);
                    final Vec3 angles = TrackRenderer.getModelAngles(bc.getNormal(t1), diff);

                    final Vec3 anchor = v1.add(diff.scale(.5));
                    Vec3 localOrigin = origin.subtract(anchor);
                    Vec3 localDirection = target.subtract(origin);
                    localOrigin = VecHelper.rotate(localOrigin, AngleHelper.deg(-angles.x), Axis.X);
                    localOrigin = VecHelper.rotate(localOrigin, AngleHelper.deg(-angles.y), Axis.Y);
                    localDirection = VecHelper.rotate(localDirection, AngleHelper.deg(-angles.x), Axis.X);
                    localDirection = VecHelper.rotate(localDirection, AngleHelper.deg(-angles.y), Axis.Y);

                    final Optional<Vec3> clip = segmentBounds.clip(localOrigin, localOrigin.add(localDirection));
                    if (clip.isEmpty()) {
                        continue;
                    }
                    LOGGER.warn("Stage7");

                    if (bestSegment != -1 && bestDistance < clip.get()
                        .distanceToSqr(0, 0.25f, 0)) {
                        continue;
                    }
                    LOGGER.warn("Stage8");

                    final double distanceToSqr = clip.get()
                        .distanceToSqr(localOrigin);
                    if (distanceToSqr > maxRange) {
                        continue;
                    }
                    LOGGER.warn("Stage9");

                    bestSegment = i;
                    newMaxRange = distanceToSqr;
                    bestDistance = clip.get()
                        .distanceToSqr(0, 0.25f, 0);

                    final BezierTrackPointLocation location = new BezierTrackPointLocation(bc.getKey(), i);
                    result = new BezierPointSelection(te, location, anchor, angles, diff.normalize());

                    LOGGER.warn("Stage10 " + result);
                }

                if (bestSegment != -1) {
                    maxRange = newMaxRange;
                }
            }
        }*/

    }
}
