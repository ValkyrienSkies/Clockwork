package org.valkyrienskies.clockwork.fabric.mixin.create;

import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionCollider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.primitives.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mixin(ContraptionCollider.class)
public abstract class MixinContraptionCollider {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("Clockwork.MixinContraptionCollider");

    @Unique
    private static AbstractContraptionEntity contraptionEnt;

    @Unique
    private static AABB EntityGetBoundingBox(Entity instance) {
        AABB tempAabb = instance.getBoundingBox();
        Ship ship = VSGameUtilsKt.getShipManagingPos(instance.getCommandSenderWorld(),contraptionEnt.position());
        if(ship!=null){
            AABBd temp = new AABBd();
            temp.set(VectorConversionsMCKt.toJOML(tempAabb)).transform(ship.getWorldToShip());
            tempAabb = VectorConversionsMCKt.toMinecraft(temp);
        }
        return tempAabb;
    }

    @Unique
    private static Vec3 EntityPosition(Entity instance){
        Vec3 tempVec = instance.position();
        Ship ship = VSGameUtilsKt.getShipManagingPos(instance.getCommandSenderWorld(),contraptionEnt.position());
        if(ship!=null){
            Vector3d translatedPos = ship.getTransform().getWorldToShip().transformPosition(VectorConversionsMCKt.toJOML(tempVec));
            tempVec = VectorConversionsMCKt.toMinecraft(translatedPos);
        }
        return tempVec;
    }

    @Inject(
            method = "collideEntities",
            at=@At("HEAD"),remap = false
    )
    private static void injectHead(AbstractContraptionEntity contraptionEntity, CallbackInfo ci){
        contraptionEnt = contraptionEntity;
    }
    @Redirect(
            method = "collideEntities",
            at = @At(value="INVOKE",target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V")
    )
    private static void redirectSetPos(Entity instance, double x, double y, double z) {
        Ship ship = VSGameUtilsKt.getShipManagingPos(instance.getCommandSenderWorld(),contraptionEnt.position());
        if(ship!=null){
            Vector3d newPos = new Vector3d(x,y,z);
            ship.getTransform().getShipToWorld().transformPosition(x,y,z,newPos);
            if(instance.position().distanceTo(VectorConversionsMCKt.toMinecraft(newPos))<20){
                instance.setPos(newPos.x,newPos.y,newPos.z);
            } else
                LOGGER.warn("Warning setPosDistance too high ignoring setPos request");
        } else
            instance.setPos(x,y,z);
    }
    @Redirect(
            method = "collideEntities",
            at = @At(value="INVOKE",target = "Lcom/simibubi/create/content/contraptions/components/structureMovement/AbstractContraptionEntity;getBoundingBox()Lnet/minecraft/world/phys/AABB;")
    )
    private static AABB redirectContraptionGetBoundingBox(AbstractContraptionEntity instance) {
        return VSGameUtilsKt.transformAabbToWorld(instance.getCommandSenderWorld(),instance.getBoundingBox());
    }
    @Redirect(
            method = "collideEntities",
            at = @At(value="INVOKE",target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;")
    )
    private static AABB redirectEntityGetBoundingBox(Entity instance) {
        return EntityGetBoundingBox(instance);
    }
    @Redirect(
            method = "collideEntities",
            at = @At(value="INVOKE",target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;")
    )
    private static Vec3 redirectEntityPosition(Entity instance) {
        return EntityPosition(instance);
    }

    @Redirect(
            method = "getWorldToLocalTranslation(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lcom/simibubi/create/foundation/collision/Matrix3d;F)Lnet/minecraft/world/phys/Vec3;",
            at = @At(value="INVOKE",target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;")
    )
    private static AABB redirectEntityGetBoundingBox2(Entity instance) {
        return EntityGetBoundingBox(instance);
    }

    @Redirect(
            method = "getWorldToLocalTranslation(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lcom/simibubi/create/foundation/collision/Matrix3d;F)Lnet/minecraft/world/phys/Vec3;",
            at = @At(value="INVOKE",target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;")
    )
    private static Vec3 redirectEntityPosition2(Entity instance) {
        return EntityPosition(instance);
    }
}