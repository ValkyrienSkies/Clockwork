package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron;


import com.simibubi.create.foundation.item.CustomArmPoseItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.valkyrienskies.clockwork.platform.CWItem;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.constraints.*;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


public class GravitronItem extends CWItem implements CustomArmPoseItem {

    private static class GravitronState {
        boolean grabbing = false;
        boolean shouldDrop = false;
        Vector3d HeldBlockPos;
        Vector2d PlayerGrabbedRotation; // Pitch , Yaw
        Vector3d ShipGrabbedPos;
        Quaterniondc ShipGrabbedRot;
        Long shipID;
        Integer positionConstraintID;
        Integer rotationConstraintID;
        Integer positionDampeningConstraintID;
        Integer rotationDampeningConstraintID;
        Integer grabCD = 0;
    }


    private ConcurrentMap<UUID, GravitronState> gravitrons = new ConcurrentHashMap<>();

    private GravitronState getState(Player player) {
        return gravitrons.computeIfAbsent(player.getUUID(), k -> new GravitronState());
    }

    public GravitronItem(Properties properties) {
        super(properties);
    }

    // || ITEM FUNCTIONS || //

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null) {
            GravitronState s = getState(context.getPlayer());
            if (s.shipID == null && !context.getPlayer().getCooldowns().isOnCooldown(this) && !s.grabbing) {
                grabShip(s, context);
            }
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        GravitronState s = getState(player);
        if (s.shipID != null && s.grabCD == 0 && s.grabbing) {
            s.shouldDrop = true;
        }

        return super.use(level, player, usedHand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!(entity instanceof  Player p)) {
            return;
        }

        GravitronState s = getState(p);

        if (isSelected && !s.shouldDrop) {
            updateShip(s, level, p);
        } else {
            dropShip(s, level);
        }
        if (s.grabCD > 0) {
            s.grabCD--;
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    // || SHIP FUNCTIONS || //

    // called first to put the ship into the players grasp
    void grabShip(GravitronState s, UseOnContext context) {
        s.grabbing = true;
        context.getPlayer().getCooldowns().addCooldown(this, 20);
        s.grabCD = 20;
        if (VSGameUtilsKt.getShipObjectManagingPos(context.getLevel(), context.getClickedPos()) == null) {
            return;
        }
        s.shipID = VSGameUtilsKt.getShipObjectManagingPos(context.getLevel(), context.getClickedPos()).getId();
        Ship ship = VSGameUtilsKt.getShipObjectWorld(context.getLevel()).getLoadedShips().getById(s.shipID);

        s.HeldBlockPos = ship.getTransform().getShipToWorld().transformPosition(VectorConversionsMCKt.toJOML(context.getClickLocation()));
        s.PlayerGrabbedRotation = new Vector2d(-context.getPlayer().xRotO, -context.getPlayer().yRotO);

        //ShipGrabbedPos = ship.getShipTransform().getShipToWorld().transformPosition(HeldBlockPos);
        s.ShipGrabbedPos = VectorConversionsMCKt.toJOML(context.getClickLocation());
        s.ShipGrabbedRot = ship.getTransform().getShipToWorldRotation();

    }

    // sets down the ship
    void dropShip(GravitronState s, Level level) {
        s.grabbing = false;

        if (level != null && !level.isClientSide) {
            ServerLevel sLevel = (ServerLevel) level;

            delConstraint(sLevel, s.positionConstraintID);
            delConstraint(sLevel, s.positionDampeningConstraintID);
            delConstraint(sLevel, s.rotationConstraintID);
            delConstraint(sLevel, s.rotationDampeningConstraintID);
            s.shipID = null;
            s.positionConstraintID = null;
            s.rotationConstraintID = null;
            s.positionDampeningConstraintID = null;
            s.rotationDampeningConstraintID = null;
            s.shouldDrop = false;
        }
    }

    // ONLY IN DEBUG SHOULD THIS BE USED
    void printRemovedConstraints(Integer... constraints) {
        for (Integer constraint : constraints) {
            if (constraint != null) {
                System.out.println("Removed " + constraint);
            }
        }
    }

    void updateShip(GravitronState s, Level level, Entity entity) {
        if (s.grabbing) {
            if (s.shipID != null) {

                if (!(level instanceof ServerLevel sLevel)) {
                    return;
                }

                LoadedServerShip ship = VSGameUtilsKt.getShipObjectWorld(sLevel).getLoadedShips().getById(s.shipID);

                Long worldShipID = VSGameUtilsKt.getShipObjectWorld(sLevel).getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId(sLevel));


                if (ship != null) {
                    double mass = ship.getInertiaData().getMass();

                    // Update Rot Values
                    Vector2d PlayerCurrentRotation = new Vector2d(entity.xRotO, entity.yRotO);

                    Quaterniond ogPlayerRot = playerRotToQuaternion(PlayerCurrentRotation.x, PlayerCurrentRotation.y).normalize();
                    Quaterniond newPlayerRot = playerRotToQuaternion(s.PlayerGrabbedRotation.x, s.PlayerGrabbedRotation.y).normalize();
                    Quaterniond deltaPlayerRot = newPlayerRot.mul(ogPlayerRot.conjugate(), new Quaterniond()).normalize();
                    Quaterniond Rotation = deltaPlayerRot.mul(s.ShipGrabbedRot, new Quaterniond()).normalize();

                    // Update Pos Values
                    s.HeldBlockPos = VectorConversionsMCKt.toJOML(entity.position()).add(0.0, entity.getEyeHeight(), 0.0).add(VectorConversionsMCKt.toJOML(entity.getLookAngle()).normalize().mul(getShipSize(ship)));
                    Vector3d posOffset = new Vector3d(s.ShipGrabbedPos).sub(ship.getTransform().getPositionInShip());
                    Vector3d posGlobalOffset = ship.getTransform().getShipToWorld().transformDirection(posOffset, new Vector3d());


                    Vector3d Location = new Vector3d(s.ShipGrabbedPos).sub(posOffset);
                    Vector3d Position = new Vector3d(s.HeldBlockPos).sub(posGlobalOffset);


                    double AttachmentCompliance = 1e-6 / Math.sqrt(mass);
                    double AttachmentMaxForce = 1e10;
                    double AttachmentFixedDistance = 0.0;
                    VSAttachmentConstraint AttachmentConstraint = new VSAttachmentConstraint(
                        s.shipID, worldShipID, AttachmentCompliance, Location, Position,
                        AttachmentMaxForce, AttachmentFixedDistance);

                    double RotationCompliance = 1e-7 / Math.sqrt(mass);
                    double RotationMaxForce = 1e10;
                    VSFixedOrientationConstraint RotationConstraint = new VSFixedOrientationConstraint(
                        s.shipID, worldShipID, RotationCompliance, new Quaterniond(), Rotation,
                        RotationMaxForce);

                    double PosDampingCompliance = 0.0;
                    double PosDampingMaxForce = 0.0;
                    double PosDampingEff = 100.0;
                    VSPosDampingConstraint PosDampingConstraint = new VSPosDampingConstraint(
                        s.shipID, worldShipID, PosDampingCompliance, Location, Position,
                        PosDampingMaxForce, PosDampingEff);

                    double RotDampingCompliance = 0.0;
                    double RotDampingMaxForce = 0.0;
                    double RotDampingEff = 100.0;
                    VSRotDampingConstraint RotDampingConstraint = new VSRotDampingConstraint(
                        s.shipID, worldShipID, RotDampingCompliance, new Quaterniond(), Rotation,
                        RotDampingMaxForce, RotDampingEff, VSRotDampingAxes.ALL_AXES);

                    //Drop and re grab the Constraints

                    System.out.println(Location);
                    System.out.println(Position);
                    System.out.println();

                    delConstraint(sLevel, s.positionConstraintID);
                    delConstraint(sLevel, s.positionDampeningConstraintID);
                    delConstraint(sLevel, s.rotationConstraintID);
                    delConstraint(sLevel, s.rotationDampeningConstraintID);

                    s.positionConstraintID = VSGameUtilsKt.getShipObjectWorld(sLevel).createNewConstraint(AttachmentConstraint);
                    s.rotationConstraintID = VSGameUtilsKt.getShipObjectWorld(sLevel).createNewConstraint(RotationConstraint);
                    s.positionDampeningConstraintID = VSGameUtilsKt.getShipObjectWorld(sLevel).createNewConstraint(PosDampingConstraint);
                    s.rotationDampeningConstraintID = VSGameUtilsKt.getShipObjectWorld(sLevel).createNewConstraint(RotDampingConstraint);


                }
            } else {
                dropShip(s, level);
            }
        }
    }

    // || MATH FUNCTIONS || //

    void delConstraint(ServerLevel level, Integer ID) {
        if (ID != null) {
            VSGameUtilsKt.getShipObjectWorld(level).removeConstraint(ID);
        }
    }

    double getShipSize(Ship thisship) {
        if (thisship != null) {
            Vector3d MinVector = new Vector3d(thisship.getShipAABB().minX(), thisship.getShipAABB().minY(), thisship.getShipAABB().minZ());
            Vector3d MaxVector = new Vector3d(thisship.getShipAABB().maxX(), thisship.getShipAABB().maxY(), thisship.getShipAABB().maxZ());
            return MinVector.sub(MaxVector).length() + 0.75;
        } else return 0.0;

    }


    Quaterniond playerRotToQuaternion(double pitch, double yaw) {
        return new Quaterniond().rotateY(Math.toRadians(yaw)).rotateX(Math.toRadians(pitch));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    @Nullable
    public HumanoidModel.ArmPose getArmPose(ItemStack stack, AbstractClientPlayer player, InteractionHand hand) {
        if (!player.swinging) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return null;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player player) {
        return false;
    }
}