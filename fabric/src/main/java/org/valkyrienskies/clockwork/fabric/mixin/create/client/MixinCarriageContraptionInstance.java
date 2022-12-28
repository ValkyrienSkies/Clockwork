package org.valkyrienskies.clockwork.fabric.mixin.create.client;

import static org.joml.Matrix4dc.PROPERTY_IDENTITY;

import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.util.AnimationTickHolder;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.simibubi.create.content.logistics.trains.entity.BogeyInstance;
import com.simibubi.create.content.logistics.trains.entity.Carriage;
import com.simibubi.create.content.logistics.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.logistics.trains.entity.CarriageContraptionInstance;
import com.simibubi.create.foundation.utility.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
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
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("VS2 create.client.MixinCarriageContraptionInstance");

    @Final
    @Shadow
    PoseStack ms;

    @Shadow
    private Couple<BogeyInstance> bogeys;

    @Shadow
    private Carriage carriage;

    @Unique
    private double fullMetalAlchemist(final double a, final double b, final double c) {
        return a * b + c;
    }

    @Unique
    private Matrix4d trans(final Matrix4d matrix4d, final double x, final double y, final double z) {
        if ((matrix4d.properties() & PROPERTY_IDENTITY) != 0) {
            return matrix4d.translation(x, y, z);
        }
        matrix4d.m30(
            fullMetalAlchemist(matrix4d.m00(), x,
                fullMetalAlchemist(matrix4d.m10(), y, fullMetalAlchemist(matrix4d.m20(), z, matrix4d.m30()))));
        matrix4d.m31(
            fullMetalAlchemist(matrix4d.m01(), x,
                fullMetalAlchemist(matrix4d.m11(), y, fullMetalAlchemist(matrix4d.m21(), z, matrix4d.m31()))));
        matrix4d.m32(
            fullMetalAlchemist(matrix4d.m02(), x,
                fullMetalAlchemist(matrix4d.m12(), y, fullMetalAlchemist(matrix4d.m22(), z, matrix4d.m32()))));
        matrix4d.m33(
            fullMetalAlchemist(matrix4d.m03(), x,
                fullMetalAlchemist(matrix4d.m13(), y, fullMetalAlchemist(matrix4d.m23(), z, matrix4d.m33()))));

        //matrix4d.assume(matrix4d.properties() & ~(PROPERTY_PERSPECTIVE | PROPERTY_IDENTITY));
        return matrix4d;
    }

    @Unique
    private static MaterialManager matManage;

    @ModifyArgs(
        method = "init", remap = false,
        at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/utility/Couple;mapNotNullWithParam(Ljava/util/function/BiFunction;Ljava/lang/Object;)Lcom/simibubi/create/foundation/utility/Couple;")

    )
    private void injectHead(final Args args) {
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
        final Vector3f instancePosition =
            ((CarriageContraptionInstance) (Object) this).getInstancePosition(partialTicks);

        final Level level = ((CarriageContraptionInstance) (Object) this).world;
        //final BlockPos position = ((CarriageContraptionInstance) (Object) this).getWorldPosition();
        //LOGGER.warn("Checking at vector3f " + vector3f);
        //LOGGER.warn("Checking at instancePosition " + instancePosition);
        final ClientShip ship =
            (ClientShip) VSGameUtilsKt.getShipObjectManagingPos(level, vector3f.x(), vector3f.y(), vector3f.z());

        final LocalPlayer player = Minecraft.getInstance().player;

        if (ship != null) {
            boolean crouchTest = false;
            if (player != null) {
                crouchTest = player.isCrouching();
            }
            final CarriageContraptionEntity carriageContraptionEntity = carriage.anyAvailableEntity();
            final Vector3d origin = VectorConversionsMCKt.toJOMLD(matManage.getOriginCoordinate());
            final Vec3 pos = carriageContraptionEntity.position();
            final Vector3d lerped =
                new Vector3d(
                    Mth.lerp(partialTicks, carriageContraptionEntity.xOld, pos.x),// - origin.x,
                    Mth.lerp(partialTicks, carriageContraptionEntity.yOld, pos.y),// - origin.y,
                    Mth.lerp(partialTicks, carriageContraptionEntity.zOld, pos.z)// - origin.z
                );
            /*
            LOGGER.warn("Checking carriageContraptionEntity lerpedVec3 [" +
                lerped.x + "," +
                lerped.y + "," +
                lerped.z +
                "] " + lerped + " partialTicks " + partialTicks);*/

            final ShipTransform transform = ship.getRenderTransform();
            final Vector3d a = new Vector3d(0, 0, 0);
            ship.getRenderTransform().getShipToWorld()
                .transformPosition(lerped.x, lerped.y, lerped.z, a);
            //LOGGER.warn("Checking at where-everAis " + a);

            final Vector3d vector3d = new Vector3d(vector3f.x(), vector3f.y(), vector3f.z());
            //LOGGER.warn("vector3d " + vector3d + " x " + vector3d.x + " y " + vector3d.y + " z " + vector3d.z);
            //LOGGER.warn("vector3f " + vector3f + " x " + vector3f.x() + " y " + vector3f.y() + " z " + vector3f.z());
            Matrix4d renderMatrix = new Matrix4d();
            renderMatrix.translate(origin.mul(-1));
            renderMatrix.mul(transform.getShipToWorld());
            renderMatrix = trans(renderMatrix, lerped.x, lerped.y, lerped.z);
            //renderMatrix.translate(vector3d);

            //.translate(vector3f.x(), vector3f.y(), vector3f.z());
            //renderMatrix.rotate()
            final Matrix4f mat4f = VectorConversionsMCKt.toMinecraft(renderMatrix);
            //LOGGER.warn("mat4f " + mat4f + "\ncrouched " + crouchTest);
            ((PoseStack) instance).last().pose().multiply(mat4f);
            if (crouchTest) {
                instance.translate(new Vector3f(0.5f, 0, -0.5f));
            }

            //VectorConversionsMCKt.multiply((PoseStack) instance, renderMatrix);

            //VSClientGameUtils.transformRenderIfInShipyard((PoseStack) instance, vector3f.x(), vector3f.y(),vector3f.z());
        } else {
            instance.translate(vector3f);
        }
        return null;
    }

}
