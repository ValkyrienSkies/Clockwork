package org.valkyrienskies.clockwork.fabric.content.curiosities.tools.gravitron;


import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.constraints.*;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import org.valkyrienskies.physics_api.constraints.AttachmentConstraint;


public class GravitronItem extends Item {
    public GravitronItem(Properties properties) {
        super(properties);
    }

    boolean grabbing = false;

    Vector3d HeldBlockPos;
    Vector2d PlayerGrabbedRotation; // Pitch , Yaw

    Vector3d ShipGrabbedPos;
    Quaterniondc ShipGrabbedRot;

    Long shipID;
    Integer positionConstraintID;
    Integer rotationConstraintID;
    Integer positionDampeningConstraintID;
    Integer rotationDampeningConstraintID;

    // || ITEM FUNCTIONS || //

    @Override
    public InteractionResult useOn(UseOnContext context) {
        grabShip(context);
            return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        //dropShip();
        return super.use(level, player, usedHand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if(isSelected){
            updateShip(level, entity);
        } else {
            dropShip(level);
        }

        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    // || SHIP FUNCTIONS || //

    // called first to put the ship into the players grasp
    void grabShip(UseOnContext context) {
        grabbing = true;

        shipID = VSGameUtilsKt.getShipObjectManagingPos(context.getLevel(), context.getClickedPos()).getId();
        Ship ship = VSGameUtilsKt.getShipObjectWorld(context.getLevel()).getLoadedShips().getById(shipID);

        HeldBlockPos = ship.getTransform().getShipToWorld().transformPosition(VectorConversionsMCKt.toJOML(context.getClickLocation()));
        PlayerGrabbedRotation = new Vector2d(-context.getPlayer().xRotO, -context.getPlayer().yRotO);

        //ShipGrabbedPos = ship.getShipTransform().getShipToWorld().transformPosition(HeldBlockPos);
        ShipGrabbedPos = VectorConversionsMCKt.toJOML(context.getClickLocation());
        ShipGrabbedRot = ship.getShipTransform().getShipToWorldRotation();

    }

    // sets down the ship
    void dropShip(Level level) {
        grabbing = false;

        if (level != null && !level.isClientSide){
            ServerLevel sLevel = (ServerLevel) level;

            delConstraint(sLevel, positionConstraintID);
            delConstraint(sLevel, positionDampeningConstraintID);
            delConstraint(sLevel, rotationConstraintID);
            delConstraint(sLevel, rotationDampeningConstraintID);
        }


        shipID = null;
    }

    void updateShip(Level level, Entity entity) {
        if(grabbing) {
            if(shipID != null) {

                if(level.isClientSide) { return; }

                Ship ship = VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips().getById(shipID);
                ServerLevel sLevel = (ServerLevel) level;

                Long worldShipID = VSGameUtilsKt.getShipObjectWorld(sLevel).getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId(sLevel));


                if (ship != null) {

                    // Update Rot Values
                    Vector2d PlayerCurrentRotation = new Vector2d(entity.xRotO, entity.yRotO);

                    Quaterniond ogPlayerRot = playerRotToQuaternion(PlayerCurrentRotation.x, PlayerCurrentRotation.y).normalize();
                    Quaterniond newPlayerRot = playerRotToQuaternion(PlayerGrabbedRotation.x, PlayerGrabbedRotation.y).normalize();
                    Quaterniond deltaPlayerRot = newPlayerRot.mul(ogPlayerRot.conjugate(), new Quaterniond()).normalize();
                    Quaterniond Rotation = deltaPlayerRot.mul(ShipGrabbedRot, new Quaterniond()).normalize();

                    // Update Pos Values
                    HeldBlockPos = VectorConversionsMCKt.toJOML(entity.position()).add(0.0, entity.getEyeHeight(), 0.0).add(VectorConversionsMCKt.toJOML(entity.getLookAngle()).normalize().mul(getShipSize(ship)));
                    Vector3d posOffset = new Vector3d(ShipGrabbedPos).sub(ship.getTransform().getPositionInShip());
                    Vector3d posGlobalOffset = ship.getTransform().getShipToWorld().transformDirection(posOffset, new Vector3d());


                    Vector3d Location = new Vector3d(ShipGrabbedPos).sub(posOffset);
                    Vector3d Position = new Vector3d(HeldBlockPos).sub(posGlobalOffset);


                    Double AttachmentCompliance = 1e-7;
                    Double AttachmentMaxForce = 1e10;
                    Double AttachmentFixedDistance = 0.0;
                    VSAttachmentConstraint AttachmentConstraint = new VSAttachmentConstraint(
                            shipID, worldShipID, AttachmentCompliance, Location, Position,
                            AttachmentMaxForce, AttachmentFixedDistance );

                    Double RotationCompliance = 1e-8;
                    Double RotationMaxForce = 1e10;
                    VSFixedOrientationConstraint RotationConstraint = new VSFixedOrientationConstraint(
                            shipID, worldShipID, RotationCompliance, new Quaterniond(), Rotation,
                            RotationMaxForce );

                    Double PosDampingCompliance = 0.0;
                    Double PosDampingMaxForce = 0.0;
                    Double PosDampingEff = 100.0;
                    VSPosDampingConstraint PosDampingConstraint = new VSPosDampingConstraint(
                            shipID, worldShipID, PosDampingCompliance, Location, Position,
                            PosDampingMaxForce, PosDampingEff );

                    Double RotDampingCompliance = 0.0;
                    Double RotDampingMaxForce = 0.0;
                    Double RotDampingEff = 100.0;
                    VSRotDampingConstraint RotDampingConstraint = new VSRotDampingConstraint(
                            shipID, worldShipID, RotDampingCompliance, new Quaterniond(), Rotation,
                            RotDampingMaxForce, RotDampingEff, VSRotDampingAxes.ALL_AXES );

                    //Drop and re grab the Constraints

                    System.out.println(Location);
                    System.out.println(Position);
                    System.out.println();

                    positionConstraintID            = VSGameUtilsKt.getShipObjectWorld(sLevel).createNewConstraint(AttachmentConstraint);
                    rotationConstraintID            = VSGameUtilsKt.getShipObjectWorld(sLevel).createNewConstraint(RotationConstraint);
                    positionDampeningConstraintID   = VSGameUtilsKt.getShipObjectWorld(sLevel).createNewConstraint(PosDampingConstraint);
                    rotationDampeningConstraintID   = VSGameUtilsKt.getShipObjectWorld(sLevel).createNewConstraint(RotDampingConstraint);


                }
            } else { dropShip(level); }
        }
    }

    // || MATH FUNCTIONS || //

    void delConstraint(ServerLevel level, Integer ID) {
        if (ID != null) {
            VSGameUtilsKt.getShipObjectWorld(level).removeConstraint(ID);
        }
    }

    Double getShipSize(Ship thisship){

        if(thisship != null) {
            Vector3d MinVector = new Vector3d(thisship.getShipAABB().minX(),thisship.getShipAABB().minY(),thisship.getShipAABB().minZ());
            Vector3d MaxVector = new Vector3d(thisship.getShipAABB().maxX(),thisship.getShipAABB().maxY(),thisship.getShipAABB().maxZ());
            return MinVector.sub(MaxVector).length() + 0.75;
        } else return 0.0;

    }



    Quaterniond playerRotToQuaternion(Double pitch, Double yaw) {
        return new Quaterniond().rotateY(Math.toRadians(yaw)).rotateX(Math.toRadians(pitch));
    }
}