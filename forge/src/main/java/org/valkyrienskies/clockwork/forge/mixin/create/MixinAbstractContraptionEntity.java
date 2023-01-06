package org.valkyrienskies.clockwork.forge.mixin.create;

import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.Contraption;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionCollider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3d;
import org.joml.primitives.AABBd;
import org.lwjgl.system.CallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(AbstractContraptionEntity.class)
public abstract class MixinAbstractContraptionEntity {
    @Shadow
    protected Contraption contraption;
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("Clockwork.MixinAbstractContraptionEntity");

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
}