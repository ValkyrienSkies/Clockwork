package org.valkyrienskies.clockwork.fabric.content.curiosities.tools.gravitron;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
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
import org.apache.commons.lang3.ObjectUtils;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipIdKt;
import org.valkyrienskies.core.apigame.constraints.*;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import org.valkyrienskies.physics_api.constraints.ConstraintAndId;

public class GravitronItem extends Item {

    public GravitronItem(Properties properties) {
        super(properties);
    }


    boolean Grabbing = false;
    Player CurrentPlayer;
    ServerLevel CurrentLevel;
    Vector3d HeldBlockPos;
    Vector2d PlayerGrabbedRotation; // Pitch , Yaw

    Vector3d ShipGrabbedPos;
    Quaterniondc ShipGrabbedRot;

    Integer ShipID;
    Integer WorldShipID;
    Integer AttachConstraintID;
    Integer RotateConstraintID;
    Integer DampPosConstraintID;
    Integer DampRotConstraintID;


    @Override
    public InteractionResult useOn(UseOnContext context) {

        if(Grabbing) {
            dropShip();
        } else {
            grabShip(context);
        }

        return super.useOn(context);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {

        if(isSelected && ShipID != null && Grabbing) {
            tickConstraints();
        } else {
            dropShip();
        }

        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(Grabbing) {
            dropShip();
        }
        return super.use(level, player, usedHand);
    }

    void grabShip(UseOnContext context) {
        resetVars();
        Grabbing = true;

        CurrentPlayer = context.getPlayer();
        CurrentLevel = (ServerLevel) context.getLevel();
        HeldBlockPos = VectorConversionsMCKt.toJOML(context.getClickLocation());
        PlayerGrabbedRotation = new Vector2d(CurrentPlayer.xRotO, CurrentPlayer.yRotO);
        
        Ship ship = VSGameUtilsKt.getShipObjectManagingPos(CurrentLevel, context.getClickedPos());
        ShipGrabbedPos = ship.getShipTransform().getShipToWorld().transformPosition(HeldBlockPos);
        ShipGrabbedRot = ship.getShipTransform().getShipToWorldRotation();

    }
    void dropShip() {
        Grabbing = false;

        breakGrabConstraints();
        resetVars();

    }

    Double getShipSize(Integer ShipID){
        if(ShipID != null && CurrentLevel != null){

            Ship thisship = VSGameUtilsKt.getShipObjectWorld(CurrentLevel).getLoadedShips().getById(ShipID);
            Vector3d MinVector = new Vector3d(thisship.getShipAABB().minX(),thisship.getShipAABB().minY(),thisship.getShipAABB().minZ());
            Vector3d MaxVector = new Vector3d(thisship.getShipAABB().maxX(),thisship.getShipAABB().maxY(),thisship.getShipAABB().maxZ());

            return MinVector.sub(MaxVector).length() + 0.75;
        } else {
            dropShip();
            return 1.0;
        }

    }

    void resetVars() {
        Grabbing = false;
        
        CurrentPlayer = null;
        CurrentLevel = null;
        HeldBlockPos = null;
        PlayerGrabbedRotation = null;

        ShipGrabbedPos = null;
        ShipGrabbedRot = null;

        ShipID = null;
        AttachConstraintID = null;
        RotateConstraintID = null;
        DampPosConstraintID = null;
        DampRotConstraintID = null;
    }

    void makeConstraints(Vector3d Position, Vector3d Location, Quaterniond Rotation) {

        Double AttachmentCompliance = 1e-5;
        Double AttachmentMaxForce = 1e10;
        Double AttachmentFixedDistance = 0.0;
        VSAttachmentConstraint AttachmentConstraint = new VSAttachmentConstraint(
                ShipID, WorldShipID, AttachmentCompliance, Location, Position,
                AttachmentMaxForce, AttachmentFixedDistance );

        Double RotationCompliance = 1e-6;
        Double RotationMaxForce = 1e10;
        VSFixedOrientationConstraint RotationConstraint = new VSFixedOrientationConstraint(
                ShipID, WorldShipID, RotationCompliance, new Quaterniond(), Rotation,
                RotationMaxForce );

        Double PosDampingCompliance = 0.0;
        Double PosDampingMaxForce = 0.0;
        Double PosDampingEff = 100.0;
        VSPosDampingConstraint PosDampingConstraint = new VSPosDampingConstraint(
                ShipID, WorldShipID, PosDampingCompliance, Location, Position,
                PosDampingMaxForce, PosDampingEff );

        Double RotDampingCompliance = 0.0;
        Double RotDampingMaxForce = 0.0;
        Double RotDampingEff = 100.0;
        VSRotDampingConstraint RotDampingConstraint = new VSRotDampingConstraint(
                ShipID, WorldShipID, RotDampingCompliance, new Quaterniond(), Rotation,
                RotDampingMaxForce, RotDampingEff, VSRotDampingAxes.ALL_AXES );

        //Drop and re grab the Constraints
        breakGrabConstraints();

        AttachConstraintID =    VSGameUtilsKt.getShipObjectWorld(CurrentLevel).createNewConstraint(RotationConstraint);
        RotateConstraintID =    VSGameUtilsKt.getShipObjectWorld(CurrentLevel).createNewConstraint(AttachmentConstraint);
        DampPosConstraintID =   VSGameUtilsKt.getShipObjectWorld(CurrentLevel).createNewConstraint(PosDampingConstraint);
        DampRotConstraintID =   VSGameUtilsKt.getShipObjectWorld(CurrentLevel).createNewConstraint(RotDampingConstraint);
    }

    void breakGrabConstraints() {
        breakConstraint(AttachConstraintID);
        breakConstraint(RotateConstraintID);
        breakConstraint(DampPosConstraintID);
        breakConstraint(DampRotConstraintID);
    }
    void breakConstraint(Integer ID) {
        if (ID != null) {
            VSGameUtilsKt.getShipObjectWorld(CurrentLevel).removeConstraint(ID);
        }
    }

    void tickConstraints() {
        if(ShipID != null && AttachConstraintID != null && RotateConstraintID != null
                && DampPosConstraintID != null && DampRotConstraintID != null)
        {

            Ship tempShip = VSGameUtilsKt.getShipObjectWorld(CurrentLevel).getLoadedShips().getById(ShipID);

                // Update Rot Values
            Vector2d PlayerCurrentRotation = new Vector2d(CurrentPlayer.xRotO, CurrentPlayer.yRotO);

            Quaterniond ogPlayerRot = playerRotToQuaternion(PlayerGrabbedRotation.x,PlayerGrabbedRotation.y).normalize();
            Quaterniond newPlayerRot = playerRotToQuaternion(PlayerCurrentRotation.x, PlayerCurrentRotation.y).normalize();
            Quaterniond deltaPlayerRot = newPlayerRot.mul(ogPlayerRot.conjugate(), new Quaterniond()).normalize();
            Quaterniond newRot = deltaPlayerRot.mul(ShipGrabbedRot, new Quaterniond()).normalize();

            // Update Pos Values
            ShipGrabbedPos = VectorConversionsMCKt.toJOML(CurrentPlayer.position()).add(0.0, CurrentPlayer.getEyeHeight(), 0.0) .add(VectorConversionsMCKt.toJOML(CurrentPlayer.getLookAngle()).normalize().mul(getShipSize(ShipID)));
            Vector3d posOffset = new Vector3d(HeldBlockPos).sub(tempShip.getTransform().getPositionInShip());
            Vector3d posGlobalOffset = tempShip.getTransform().getShipToWorld().transformDirection(posOffset, new Vector3d());


            makeConstraints(new Vector3d(ShipGrabbedPos).sub(posGlobalOffset), new Vector3d(HeldBlockPos).sub(posGlobalOffset), newRot);

        } else {dropShip();}
    }

    Quaterniond playerRotToQuaternion(Double pitch, Double yaw) {
        return new Quaterniond().rotateY(Math.toRadians(-yaw)).rotateX(Math.toRadians(pitch));
    }

}
