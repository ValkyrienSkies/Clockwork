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
import org.joml.*;
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck;
import org.valkyrienskies.clockwork.platform.CWItem;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.constraints.*;
import org.valkyrienskies.core.impl.api.LoadedServerShipInternal;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import javax.annotation.Nullable;
import java.lang.Math;


public class GravitronItem extends CWItem implements CustomArmPoseItem {

    public GravitronItem(Properties properties) {
        super(properties);
    }

    private GravitronState getState(Player player) {
        var p = (MixinPlayerDuck) player;
        var s = p.cw_getGravitronState();
        if (s == null) {
            s = new GravitronState();
            p.cw_setGravitronState(s);
        }

        return s;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel() instanceof ServerLevel level && context.getPlayer() != null) {
            GravitronState s = getState(context.getPlayer());
            if (s.shipID == null && !context.getPlayer().getCooldowns().isOnCooldown(this) && !s.grabbing) {
                s.grabbing = true;
                context.getPlayer().getCooldowns().addCooldown(this, 20);
                s.grabCD = 20;

                tryGrabShip(s, level, context);
            }
        }
        return super.useOn(context);
    }

    // || ITEM FUNCTIONS || //

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
        if (!(entity instanceof Player p) || !(level instanceof ServerLevel sLevel)) {
            return;
        }

        GravitronState s = getState(p);

        if (isSelected && !s.shouldDrop) {
            updateShip(s, sLevel, p);
        } else {
            dropShip(s, sLevel);
        }
        if (s.grabCD > 0) {
            s.grabCD--;
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    // called first to put the ship into the players grasp
    void tryGrabShip(GravitronState s, ServerLevel level, UseOnContext context) {
        if (context.getPlayer() == null) {
            return;
        }

        Ship ship = VSGameUtilsKt.getShipManagingPos(context.getLevel(), context.getClickedPos());
        Vector3d grabPosInShip = VectorConversionsMCKt.toJOML(context.getClickLocation());
        Vector3d grabPosInWorld = new Vector3d(grabPosInShip);

        if (VSGameUtilsKt.isBlockInShipyard(level, context.getClickedPos()) && ship == null) {
            return;
        }

        if (ship == null) {
            return; // todo: try to assemble a ship when grabbing
//            DenseBlockPosSet toAssemble = new DenseBlockPosSet();
//            toAssemble.add(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ());
//            ship = ShipAssemblyKt.createNewShipWithBlocks(context.getClickedPos(), toAssemble, level);
//            ship.getWorldToShip().transformPosition(grabPosInShip);
        } else {
            ship.getShipToWorld().transformPosition(grabPosInWorld);
        }

        grabShip(s, context.getPlayer(), ship, grabPosInShip);
    }

    // || SHIP FUNCTIONS || //

    void grabShip(GravitronState s, Player p, Ship ship, Vector3dc grabPosInShip) {
        s.shipID = ship.getId();

        s.HeldBlockPos = ship.getTransform().getShipToWorld().transformPosition(new Vector3d(grabPosInShip));
        s.PlayerGrabbedRotation = new Vector2d(p.getXRot(), p.getYRot());

        s.ShipGrabbedPos = new Vector3d(grabPosInShip);
        s.ShipGrabbedRot = ship.getTransform().getShipToWorldRotation();
    }

    // sets down the ship
    void dropShip(GravitronState s, ServerLevel level) {
        s.grabbing = false;

        if (level != null && !level.isClientSide) {
            delConstraint(level, s.positionConstraintID);
            delConstraint(level, s.positionDampeningConstraintID);
            delConstraint(level, s.rotationConstraintID);
            delConstraint(level, s.rotationDampeningConstraintID);
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

    void updateShip(GravitronState s, ServerLevel level, Entity entity) {
        if (s.grabbing) {
            if (s.shipID != null) {

                Ship shipUnloaded = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(s.shipID);
                LoadedServerShip ship = VSGameUtilsKt.getShipObjectWorld(level).getLoadedShips().getById(s.shipID);

                Long worldShipID = VSGameUtilsKt.getShipObjectWorld(level).getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId(level));


                if (ship != null && ((LoadedServerShipInternal) ship).areVoxelsFullyLoaded()) {
                    double mass = ship.getInertiaData().getMass();

                    // Update Rot Values
                    Vector2d playerCurrentRotation = new Vector2d(entity.getXRot(), entity.getYRot());

                    Quaterniondc origPlayerRot = playerRotToQuaternion(s.PlayerGrabbedRotation.x, s.PlayerGrabbedRotation.y).normalize();
                    Quaterniondc newPlayerRot = playerRotToQuaternion(playerCurrentRotation.x, playerCurrentRotation.y).normalize();
                    Quaterniondc deltaPlayerRot = newPlayerRot.mul(origPlayerRot.conjugate(new Quaterniond()), new Quaterniond());
                    Quaterniond rotation = deltaPlayerRot.mul(s.ShipGrabbedRot, new Quaterniond()).normalize();

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
                            s.shipID, worldShipID, RotationCompliance, new Quaterniond(), rotation,
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
                            s.shipID, worldShipID, RotDampingCompliance, new Quaterniond(), rotation,
                            RotDampingMaxForce, RotDampingEff, VSRotDampingAxes.ALL_AXES);

                    //Drop and re grab the Constraints

                    System.out.println(Location);
                    System.out.println(Position);
                    System.out.println();

                    delConstraint(level, s.positionConstraintID);
                    delConstraint(level, s.positionDampeningConstraintID);
                    delConstraint(level, s.rotationConstraintID);
                    delConstraint(level, s.rotationDampeningConstraintID);

                    s.positionConstraintID = VSGameUtilsKt.getShipObjectWorld(level).createNewConstraint(AttachmentConstraint);
                    s.rotationConstraintID = VSGameUtilsKt.getShipObjectWorld(level).createNewConstraint(RotationConstraint);
                    s.positionDampeningConstraintID = VSGameUtilsKt.getShipObjectWorld(level).createNewConstraint(PosDampingConstraint);
                    s.rotationDampeningConstraintID = VSGameUtilsKt.getShipObjectWorld(level).createNewConstraint(RotDampingConstraint);
                } else if (shipUnloaded == null) {
                    dropShip(s, level);
                }
            }
        }
    }

    void delConstraint(ServerLevel level, Integer ID) {
        if (ID != null) {
            VSGameUtilsKt.getShipObjectWorld(level).removeConstraint(ID);
        }
    }

    // || MATH FUNCTIONS || //

    double getShipSize(Ship thisship) {
        if (thisship != null) {
            Vector3d MinVector = new Vector3d(thisship.getShipAABB().minX(), thisship.getShipAABB().minY(), thisship.getShipAABB().minZ());
            Vector3d MaxVector = new Vector3d(thisship.getShipAABB().maxX(), thisship.getShipAABB().maxY(), thisship.getShipAABB().maxZ());
            return MinVector.sub(MaxVector).length() + 0.75;
        } else return 0.0;

    }

    Quaterniond playerRotToQuaternion(double pitch, double yaw) {
        return new Quaterniond().rotateY(Math.toRadians(-yaw)).rotateX(Math.toRadians(pitch));
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

    public static class GravitronState {
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
}