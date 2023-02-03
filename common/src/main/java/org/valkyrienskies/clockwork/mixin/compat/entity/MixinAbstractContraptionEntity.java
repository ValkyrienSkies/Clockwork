package org.valkyrienskies.clockwork.mixin.compat.entity;

import com.simibubi.create.content.contraptions.components.actors.SeatEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(AbstractContraptionEntity.class)
public abstract class MixinAbstractContraptionEntity extends Entity {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("Clockwork.MixinAbstractContraptionEntity");
    @Shadow(remap = false)
    protected Contraption contraption;

    @Shadow
    public abstract Vec3 getPassengerPosition(Entity passenger, float partialTicks);

    @Shadow
    public abstract Vec3 applyRotation(Vec3 localPos, float partialTicks);

    @Shadow
    public abstract Vec3 getAnchorVec();

    @Shadow
    public abstract Vec3 getPrevAnchorVec();

    public MixinAbstractContraptionEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Redirect(method = "moveCollidedEntitiesOnDisassembly", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"))
    private void redirectSetPos(Entity instance, double x, double y, double z) {
        Vector3d result = transformPosition(instance, x, y, z);
        if (instance.position().distanceTo(VectorConversionsMCKt.toMinecraft(result)) < 20) {
            instance.setPos(result.x, result.y, result.z);
        } else LOGGER.warn("Warning distance too high ignoring setPos request");
    }

    @Redirect(method = "moveCollidedEntitiesOnDisassembly", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;teleportTo(DDD)V"))
    private void redirectTeleportTo(Entity instance, double x, double y, double z) {
        Vector3d result = transformPosition(instance, x, y, z);
        if (instance.position().distanceTo(VectorConversionsMCKt.toMinecraft(result)) < 20) {
            instance.teleportTo(result.x, result.y, result.z);
        } else LOGGER.warn("Warning distance too high ignoring teleportTo request");
    }

    private Vector3d transformPosition(Entity instance, double x, double y, double z) {

        Ship ship = VSGameUtilsKt.getShipManagingPos(instance.getCommandSenderWorld(), this.contraption.anchor);
        Vector3d newPos = new Vector3d(x, y, z);
        if (ship != null) {
            ship.getTransform().getShipToWorld().transformPosition(x, y, z, newPos);
        }
        return newPos;
    }

    @Override
    public void positionRider(@NotNull Entity passenger) {
        if (!hasPassenger(passenger))
            return;
        Vec3 transformedVector = getPassengerPosition(passenger, 1);
        if (transformedVector == null)
            return;
        Vec3 riderPos = new Vec3(transformedVector.x, transformedVector.y + SeatEntity.getCustomEntitySeatOffset(passenger) - 1 / 8f, transformedVector.z);

        Ship ship = VSGameUtilsKt.getShipManagingPos(passenger.level,riderPos.x,riderPos.y,riderPos.z);
        if(VSGameUtilsKt.isBlockInShipyard(passenger.level,riderPos.x,riderPos.y,riderPos.z) && ship!=null){
            Vector3d tempVec = VectorConversionsMCKt.toJOML(riderPos);
            ship.getShipToWorld().transformPosition(tempVec,tempVec);
            riderPos = VectorConversionsMCKt.toMinecraft(tempVec);
        }
        passenger.setPos(riderPos);
    }

    @Inject(method = "toGlobalVector(Lnet/minecraft/world/phys/Vec3;FZ)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), cancellable = true)
    private void redirectToGlobalVector(Vec3 localVec, final float partialTicks, final boolean prevAnchor,
                                        final CallbackInfoReturnable<Vec3> cir) {
        if (partialTicks != 1 && !prevAnchor) {
            final Vec3 anchor = getAnchorVec();
            final Vec3 oldAnchor = getPrevAnchorVec();
            final Vec3 lerpedAnchor =
                    new Vec3(
                            Mth.lerp(partialTicks, oldAnchor.x, anchor.x),
                            Mth.lerp(partialTicks, oldAnchor.y, anchor.y),
                            Mth.lerp(partialTicks, oldAnchor.z, anchor.z)
                    );
            final Vec3 rotationOffset = VecHelper.getCenterOf(BlockPos.ZERO);
            localVec = localVec.subtract(rotationOffset);
            localVec = applyRotation(localVec, partialTicks);
            localVec = localVec.add(rotationOffset)
                    .add(lerpedAnchor);
            cir.setReturnValue(localVec);
        }
    }
}