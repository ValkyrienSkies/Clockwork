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
import org.joml.Math;
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
        final Level level = ((CarriageContraptionInstance) (Object) this).world;
        final ClientShip ship =
                (ClientShip) VSGameUtilsKt.getShipObjectManagingPos(level, vector3f.x(), vector3f.y(), vector3f.z());

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
                    .mul(transform.getShipToWorld())
                    .translate(newPosition);
            Matrix4f mat4f = VectorConversionsMCKt.toMinecraft(renderMatrix);
            ((PoseStack) instance).last().pose().multiply(mat4f);
        } else {
            instance.translate(vector3f);
        }
        return null;
    }
}