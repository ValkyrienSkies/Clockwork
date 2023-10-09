package org.valkyrienskies.clockwork.mixin.compat.client;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import com.simibubi.create.foundation.outliner.Outline;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSClientGameUtils;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(Outline.class)
public abstract class MixinOutline {

    //todo: fix this >:(
//    @Shadow
//    protected void putVertex(final PoseStack.Pose pose, final VertexConsumer builder, final float x, final float y,
//                             final float z, final float u, final float v,
//                             final Direction normal) {
//    }
//
//    @Inject(
//            method = "putVertex(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/Vec3;FFLnet/minecraft/core/Direction;)V",
//            at = @At(value = "HEAD"), cancellable = true
//    )
//    public void injectPutVertex(final PoseStack ms, final VertexConsumer builder, final Vec3 pos, final float u,
//                                final float v,
//                                final Direction normal,
//                                final CallbackInfo ci) {
//        final Vector3d vec3d = new Vector3d(pos.x, pos.y, pos.z);
//
//        final Level level = Minecraft.getInstance().level;
//        if (level != null) {
//            final ClientShip ship = (ClientShip) VSGameUtilsKt.getShipManagingPos(level, vec3d);
//            if (ship != null) {
//                final ShipTransform transform = ship.getRenderTransform();
//                final Vector3d transformedPos = transform.getShipToWorld().transformPosition(vec3d);
//                putVertex(ms.last(), builder, (float) transformedPos.x, (float) transformedPos.y,
//                        (float) transformedPos.z, u, v, normal);
//                ci.cancel();
//            }
//        }
//    }

    @Shadow public abstract void bufferCuboid(PoseStack.Pose pose, VertexConsumer consumer, Vector3f minPos, Vector3f maxPos, Vector4f color, int lightmap, boolean disableNormals);

    @Shadow public abstract void bufferQuad(PoseStack.Pose pose, VertexConsumer consumer, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3, Vector4f color, float minU, float minV, float maxU, float maxV, int lightmap, Vector3f normal);

    @Redirect(
            method = "bufferCuboidLine(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/Vec3;Lcom/mojang/math/Vector3d;Lcom/mojang/math/Vector3d;FLcom/mojang/math/Vector4f;IZ)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/jozufozu/flywheel/util/transform/TransformStack;translate(DDD)Ljava/lang/Object;")
    )
    private Object redirectTranslate(final TransformStack instance, final double x, final double y, final double z) {
        VSClientGameUtils.transformRenderIfInShipyard((PoseStack) instance, x, y, z);
        return instance;
    }

    @Redirect(
            method = "bufferCuboidLine(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/math/Vector3f;Lnet/minecraft/core/Direction;FFLcom/mojang/math/Vector4f;IZ)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/outliner/Outline;bufferCuboid(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/math/Vector3f;Lcom/mojang/math/Vector3f;Lcom/mojang/math/Vector4f;IZ)V")
    )
    private void redirectBufferCuboid(Outline instance, PoseStack.Pose pose, VertexConsumer consumer, Vector3f minPos, Vector3f maxPos, Vector4f color, int lightmap, boolean disableNormals) {
        Vector3f realMin = minPos.copy();
        Vector3f realMax = maxPos.copy();
        final Level level = Minecraft.getInstance().level;
        if (level != null) {
            final ClientShip ship = (ClientShip) VSGameUtilsKt.getShipManagingPos(level, realMin);
            if (ship != null) {
                final ShipTransform transform = ship.getRenderTransform();
                Vector3d realMinD = new Vector3d(realMin.x(), realMin.y(), realMin.z());
                Vector3d realMaxD = new Vector3d(realMax.x(), realMax.y(), realMax.z());
                transform.getShipToWorld().transformPosition(realMinD);
                transform.getShipToWorld().transformPosition(realMaxD);
                realMin.set((float) realMinD.x, (float) realMinD.y, (float) realMinD.z);
                realMax.set((float) realMaxD.x, (float) realMaxD.y, (float) realMaxD.z);
            }
        }

        this.bufferCuboid(pose, consumer, realMin, realMax, color, lightmap, disableNormals);
    }

    @Redirect(
            method = "bufferQuad(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/math/Vector3f;Lcom/mojang/math/Vector3f;Lcom/mojang/math/Vector3f;Lcom/mojang/math/Vector3f;Lcom/mojang/math/Vector4f;ILcom/mojang/math/Vector3f;)V", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/outliner/Outline;bufferQuad(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/math/Vector3f;Lcom/mojang/math/Vector3f;Lcom/mojang/math/Vector3f;Lcom/mojang/math/Vector3f;Lcom/mojang/math/Vector4f;FFFFILcom/mojang/math/Vector3f;)V")
    )
    private void redirectBufferQuad(Outline instance, PoseStack.Pose pose, VertexConsumer consumer, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3, Vector4f color, float minU, float minV, float maxU, float maxV, int lightmap, Vector3f normal) {
        Vector3f real0 = pos0.copy();
        Vector3f real1 = pos1.copy();
        Vector3f real2 = pos2.copy();
        Vector3f real3 = pos3.copy();
        final Level level = Minecraft.getInstance().level;
        if (level != null) {
            final ClientShip ship = (ClientShip) VSGameUtilsKt.getShipManagingPos(level, real0);
            if (ship != null) {
                final ShipTransform transform = ship.getRenderTransform();
                Vector3d real0D = new Vector3d(real0.x(), real0.y(), real0.z());
                Vector3d real1D = new Vector3d(real1.x(), real1.y(), real1.z());
                Vector3d real2D = new Vector3d(real2.x(), real2.y(), real2.z());
                Vector3d real3D = new Vector3d(real3.x(), real3.y(), real3.z());

                transform.getShipToWorld().transformPosition(real0D);
                transform.getShipToWorld().transformPosition(real1D);
                transform.getShipToWorld().transformPosition(real2D);
                transform.getShipToWorld().transformPosition(real3D);

                real0.set((float) real0D.x, (float) real0D.y, (float) real0D.z);
                real1.set((float) real1D.x, (float) real1D.y, (float) real1D.z);
                real2.set((float) real2D.x, (float) real2D.y, (float) real2D.z);
                real3.set((float) real3D.x, (float) real3D.y, (float) real3D.z);
            }
        }

        bufferQuad(pose, consumer, real0, real1, real2, real3, color, 0, 0, 1, 1, lightmap, normal);
    }

}
