package org.valkyrienskies.clockwork.fabric.mixin.create.client;

import static org.joml.Matrix4dc.PROPERTY_IDENTITY;
import static org.joml.Matrix4dc.PROPERTY_PERSPECTIVE;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.util.AnimationTickHolder;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.simibubi.create.content.logistics.trains.entity.Carriage;
import com.simibubi.create.content.logistics.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.logistics.trains.entity.CarriageContraptionInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(CarriageContraptionInstance.class)
public class MixinCarriageContraptionInstance {
    @Shadow
    private Carriage carriage;

    /**
    * the Matrix4d call ends up doing funky type conversion because of built-in Math.fma
    * so using this one from JOML (which itself calls to built in fma instead of just doing the math)
     */
    @Unique
    private double fma(final double a, final double b, final double c) {
        return a * b + c;
    }

    @Unique
    private Matrix4d trans(final Matrix4d matrix4d, final double x, final double y, final double z) {
        if ((matrix4d.properties() & PROPERTY_IDENTITY) != 0) {
            return matrix4d.translation(x, y, z);
        }
        matrix4d.m30(fma(matrix4d.m00(), x,fma(matrix4d.m10(), y, fma(matrix4d.m20(), z, matrix4d.m30()))));
        matrix4d.m31(fma(matrix4d.m01(), x,fma(matrix4d.m11(), y, fma(matrix4d.m21(), z, matrix4d.m31()))));
        matrix4d.m32(fma(matrix4d.m02(), x,fma(matrix4d.m12(), y, fma(matrix4d.m22(), z, matrix4d.m32()))));
        matrix4d.m33(fma(matrix4d.m03(), x,fma(matrix4d.m13(), y, fma(matrix4d.m23(), z, matrix4d.m33()))));

        matrix4d.assume(matrix4d.properties() & ~(PROPERTY_PERSPECTIVE | PROPERTY_IDENTITY));
        return matrix4d;
    }

    @Unique
    private static MaterialManager matManage;

    @ModifyArgs(
        method = "init", remap = false,
        at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/utility/Couple;mapNotNullWithParam(Ljava/util/function/BiFunction;Ljava/lang/Object;)Lcom/simibubi/create/foundation/utility/Couple;")

    )
    private void harvestMaterialManager(final Args args) {
        matManage = args.get(1);
        args.set(0, args.get(0));
        args.set(1, args.get(1));
    }

    @Redirect(
        method = "beginFrame", at = @At(value = "INVOKE",
        target = "Lcom/jozufozu/flywheel/util/transform/TransformStack;translate(Lcom/mojang/math/Vector3f;)Ljava/lang/Object;")
    )
    private Object redirectTranslate(final TransformStack instance, final Vector3f vector3f) {

        final float partialTicks = AnimationTickHolder.getPartialTicks();
        final Level level = ((CarriageContraptionInstance)(Object)this).world;
        final ClientShip ship =
            (ClientShip)VSGameUtilsKt.getShipObjectManagingPos(level, vector3f.x(), vector3f.y(), vector3f.z());

        if (ship != null) {
            final CarriageContraptionEntity carriageContraptionEntity = carriage.anyAvailableEntity();
            final Vector3d origin = VectorConversionsMCKt.toJOMLD(matManage.getOriginCoordinate());
            final Vec3 pos = carriageContraptionEntity.position();
            final Vector3d newPosition =
                new Vector3d(
                    Mth.lerp(partialTicks, carriageContraptionEntity.xOld, pos.x),
                    Mth.lerp(partialTicks, carriageContraptionEntity.yOld, pos.y),
                    Mth.lerp(partialTicks, carriageContraptionEntity.zOld, pos.z)
                );
            final ShipTransform transform = ship.getRenderTransform();
            Matrix4d renderMatrix = new Matrix4d()
                    .translate(origin.mul(-1))
                    .mul(transform.getShipToWorld());
            Matrix4f mat4f = VectorConversionsMCKt.toMinecraft(
                    trans(renderMatrix, newPosition.x, newPosition.y, newPosition.z));
            ((PoseStack) instance).last().pose().multiply(mat4f);
        } else {
            instance.translate(vector3f);
        }
        return null;
    }
}
