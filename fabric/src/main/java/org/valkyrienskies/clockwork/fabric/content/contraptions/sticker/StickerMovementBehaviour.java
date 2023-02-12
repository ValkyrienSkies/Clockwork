package org.valkyrienskies.clockwork.fabric.content.contraptions.sticker;

import com.simibubi.create.content.contraptions.components.structureMovement.*;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.StabilizedContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.gantry.GantryContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.piston.LinearActuatorTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.piston.PistonContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.pulley.PulleyContraption;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Unique;
import org.valkyrienskies.clockwork.mixin.accessors.IMixinPistonContraption;
import org.valkyrienskies.clockwork.mixin.compat.blockentity.IMixinMechanicalBearingTileEntity;
import org.valkyrienskies.clockwork.mixinduck.IMixinControlledContraptionEntity;
import org.valkyrienskies.clockwork.mixinduck.MixinAbstractContraptionEntityDuck;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.constraints.VSAttachmentOrientationConstraint;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import javax.annotation.Nullable;
import java.util.Iterator;

import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toMinecraft;

public class StickerMovementBehaviour implements MovementBehaviour {

    private static final double DISTANCE_BUFFER = 1.05;
    public boolean isStopped = true;

    private static final Logger LOGGER = LogManager.getLogger("Clockwork.StickerMovementBehaviour");

    @Override
    public boolean renderAsNormalTileEntity() {
        return true;
    }

    @Override
    public void tick(MovementContext context) {
        if (context.world == null || context.world.isClientSide)
            return;

        //BlockEntity tileEntity = context.contraption.presentTileEntities.get(context.localPos);
        CompoundTag extraData = context.tileData.getCompound("ForgeData");
        if (!extraData.isEmpty() && extraData.contains("ShipStickerConstraint")) {
            if (!isStopped)
                doUpdateConstraint(context, null, null);
        } else {
            if (context.state.getValue(BlockStateProperties.EXTENDED)) {
                context.tileData.put("ForgeData", new CompoundTag());
                isAttachedToShipOrWorld(true, context.world, toJOML(context.position), toJOML(context.rotation.apply(Vec3.atLowerCornerOf(context.state.getValue(DirectionalBlock.FACING).getNormal()))), context.tileData.getCompound("ForgeData"));
            }
        }
        //LOGGER.warn("tick");
    }

    @Unique
    @Nullable
    Direction.Axis getFacingAxis(MovementContext context) {
        Direction.Axis axis = null;

        if (context.contraption instanceof PistonContraption pistonContraption)
            axis = ((IMixinPistonContraption) pistonContraption).getOrientation().getAxis();

        if (context.contraption instanceof PulleyContraption pulleyContraption)
            axis = Direction.Axis.Y;

        if (context.contraption instanceof GantryContraption gantryContraption)
            axis = gantryContraption.getFacing().getAxis();

        /*
        if(context.contraption instanceof BearingContraption bearingContraption)
            axis = bearingContraption.getFacing().getAxis();

        if(context.contraption instanceof ClockworkContraption clockworkContraption)
            axis = ((IMixinClockworkContraption)clockworkContraption).getFacing().getAxis();
        */

        return axis;
    }

    @Override
    public void onSpeedChanged(MovementContext context, Vec3 oldMotion, Vec3 motion) {
        Direction.Axis axis = getFacingAxis(context);
        if (axis != null) {
            float axisMotion = Math.abs(VecHelper.getCoordinate(motion, axis));
            isStopped = axisMotion < 0.001;
        } else {
            isStopped = motion.equals(Vec3.ZERO);
        }
        Vector3d a = new Vector3d(1, 45, 1);
    }

    @Unique
    private boolean getAssembleNextTick(MovementContext context) {
        boolean result = false;
        if (context.contraption.entity instanceof ControlledContraptionEntity) {
            if (context.contraption instanceof TranslatingContraption) {
                result = ((LinearActuatorTileEntity) ((IMixinControlledContraptionEntity) context.contraption.entity).grabController()).assembleNextTick;
            }
            if (context.contraption instanceof BearingContraption || context.contraption instanceof StabilizedContraption) {
                result = ((IMixinMechanicalBearingTileEntity) ((IMixinControlledContraptionEntity) context.contraption.entity).grabController()).isAssembleNextTick();
            }
        }
        return result;
    }

    @Override
    public void startMoving(MovementContext context) {
        isStopped = false;
    }

    @Override
    public void stopMoving(MovementContext context) {
        isStopped = true;
        Vector3d position = null;
        Quaterniond quaterniond = null;
        CompoundTag extraData = context.tileData.getCompound("ForgeData");

        double distance = DISTANCE_BUFFER;
        if (extraData.contains("ShipStickerDistance"))
            distance = extraData.getDouble("ShipStickerDistance");


        Direction myDir = context.state.getValue(DirectionalBlock.FACING);
        Vec3 myDirNormal = toMinecraft(toJOML(Vec3.atLowerCornerOf(myDir.getNormal())).mul(.5));

        if (!getAssembleNextTick(context)) { //context.position == null
            StructureTransform structureTransform = ((MixinAbstractContraptionEntityDuck) context.contraption.entity).getStructureTransform();

            position = toJOML(Vec3.atCenterOf(structureTransform.apply(context.localPos)).add(structureTransform.applyWithoutOffsetUncentered(myDirNormal)));

            if (distance < DISTANCE_BUFFER) {
                position.add(toJOML(structureTransform.applyWithoutOffsetUncentered(toMinecraft(toJOML(Vec3.atLowerCornerOf(myDir.getNormal())).mul(distance / -1 + DISTANCE_BUFFER)))));
            }

            extraData.put("ShipStickerShip1Vec", writeVector3D(position));

            if (context.contraption instanceof BearingContraption bearingContraption) {
                Quaterniond tempQuat = toJOML(Vec3.atLowerCornerOf(structureTransform.applyWithoutOffset(context.localPos))).rotationTo(toJOML(Vec3.atLowerCornerOf(context.localPos)), new Quaterniond());
                quaterniond = new Quaterniond();
                tempQuat.mul(readQuatd(extraData.getCompound("ShipStickerShip1Quat")), quaterniond);
                extraData.put("ShipStickerShip1Quat", writeQuatd(quaterniond));
            }
        } else {
            Vector3d tempShip1Pos = toJOML(context.position.add(context.rotation.apply(myDirNormal)));
            extraData.put("ShipStickerShip1Vec", writeVector3D(tempShip1Pos));
        }
        if (!extraData.isEmpty()) {
            if (extraData.contains("ShipStickerConstraint")) {
                doUpdateConstraint(context, position, quaterniond);
            }
        }
    }

    static void doUpdateConstraint(MovementContext context, @Nullable Vector3d ship1Pos, @Nullable Quaterniond ship1Rot) {

        if (context.world.isClientSide)
            return;

        Ship ship1 = null;
        Ship ship2 = null;
        double distance = DISTANCE_BUFFER;

        CompoundTag compoundTag = context.tileData.getCompound("ForgeData");

        if (compoundTag.contains("ShipStickerConstraint")) {

            Vector3d ship2Pos = null;
            Quaterniond ship2Rot = null;

            if (compoundTag.contains("ShipStickerDistance"))
                distance = compoundTag.getDouble("ShipStickerDistance");

            if (compoundTag.contains("ShipStickerShip1Id"))
                ship1 = VSGameUtilsKt.getShipObjectWorld(context.world).getAllShips().getById(compoundTag.getLong("ShipStickerShip1Id"));

            if (compoundTag.contains("ShipStickerShip2Id"))
                ship2 = VSGameUtilsKt.getShipObjectWorld(context.world).getAllShips().getById(compoundTag.getLong("ShipStickerShip2Id"));
            if (compoundTag.contains("ShipStickerShip2Vec")) {
                ship2Pos = new Vector3d(readVector3D(compoundTag.getCompound("ShipStickerShip2Vec")));
            }
            if (compoundTag.contains("ShipStickerShip2Quat")) {
                ship2Rot = new Quaterniond(readQuatd(compoundTag.getCompound("ShipStickerShip2Quat")));
            }

            if (ship1 == null && ship2 == null)
                return;

            Direction myDir = context.state.getValue(DirectionalBlock.FACING);
            Vec3 myDirNormal;
            if (ship1 != null && ship2 != null)
                myDirNormal = toMinecraft(toJOML(Vec3.atLowerCornerOf(myDir.getNormal())).mul(.5));
            else
                myDirNormal = toMinecraft(toJOML(Vec3.atLowerCornerOf(myDir.getNormal())).mul(.5));


            if (ship1Pos == null) {
                if (compoundTag.contains("ShipStickerShip1Vec")) {
                    ship1Pos = new Vector3d(readVector3D(compoundTag.getCompound("ShipStickerShip1Vec")));
                }
                ship1Pos = toJOML(context.position.add(context.rotation.apply(myDirNormal)));

                if (context.contraption instanceof StabilizedContraption stabilizedContraption) {
                    ship1Pos.add(toJOML(Vec3.atLowerCornerOf(stabilizedContraption.getFacing().getNormal())).mul(0.125));
                }
                ship1Pos.add(toJOML(context.motion));

                if (distance < DISTANCE_BUFFER) {
                    ship1Pos.add(toJOML(context.rotation.apply(Vec3.atLowerCornerOf(myDir.getNormal()))).mul(distance / -1 + DISTANCE_BUFFER));
                }

            } else {
                //ship1Pos.add(toJOML(context.rotation.apply(myDirNormal)));
                compoundTag.put("ShipStickerShip1Vec", writeVector3D(ship1Pos));
            }
            if (ship1Rot == null) {
                if (compoundTag.contains("ShipStickerShip1Quat")) {
                    ship1Rot = new Quaterniond(readQuatd(compoundTag.getCompound("ShipStickerShip1Quat")));
                    AbstractContraptionEntity.ContraptionRotationState rotState = context.contraption.entity.getRotationState();
                    ship1Rot = new Quaterniond().setFromNormalized(toJOML(rotState.asMatrix().getAsMatrix4f())).mul(ship1Rot); //toJOML(new Vec3(1, 1, 1)).rotationTo(toJOML(rotatedDir), new Quaterniond());
                }
            }

            VSAttachmentOrientationConstraint constraint = makeConstraint(ship1Pos, new Vector3d(ship1Pos), ship1, ship2, (ServerLevel) context.world, ship1Rot, ship2Rot, ship2Pos);

            ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld((ServerLevel) context.world);
            shipWorld.removeConstraint(compoundTag.getInt("ShipStickerConstraint"));
            Integer constraintId = shipWorld.createNewConstraint(constraint);
            compoundTag.putInt("ShipStickerConstraint", constraintId.intValue());
        }
    }

    @Unique
    private static VSAttachmentOrientationConstraint makeConstraint(Vector3d ship1ConstraintPos, Vector3d ship2ConstraintPos, Ship ship1, Ship ship2, ServerLevel level, @Nullable Quaterniond ship1Rot, @Nullable Quaterniond ship2Rot, @Nullable Vector3d ship2Pos) {

        if (ship1 == null && ship2 == null)
            return null;


        long ship1Id;
        long ship2Id;
        long groundId = VSGameUtilsKt.getShipObjectWorld(level).getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId(level));

        if (ship1Rot == null && ship1 == null)
            ship1Rot = new Quaterniond();
        if (ship2Rot == null && ship2 == null)
            ship2Rot = new Quaterniond();

        double mass = 100;

        if (ship1 != null) {
            ship1.getShipToWorld().transformPosition(ship2ConstraintPos);
            ship1Id = ship1.getId();
            if (ship1Rot == null)
                ship1Rot = (Quaterniond) ship1.getTransform().getShipToWorldRotation();
            mass = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(ship1Id).getInertiaData().getMass();
        } else
            ship1Id = groundId;

        if (ship2 != null) {
            ship2.getWorldToShip().transformPosition(ship2ConstraintPos);
            ship2Id = ship2.getId();
            if (ship2Rot == null)
                ship2Rot = (Quaterniond) ship2.getTransform().getShipToWorldRotation();
            //ship2Rot.add(ship2.getTransform().getShipToWorldRotation());
            mass = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(ship2Id).getInertiaData().getMass();
        } else
            ship2Id = groundId;

        if (ship2Pos != null)
            ship2ConstraintPos = ship2Pos;

        Quaterniondc ship1Rotation = ship1Rot;
        Quaterniondc ship2Rotation = ship2Rot;

        VSAttachmentOrientationConstraint constraint = new VSAttachmentOrientationConstraint(
                ship1Id,
                ship2Id,
                1e-9 / mass,
                ship1ConstraintPos,
                ship2ConstraintPos,
                1e10,
                ship1Rotation,
                ship2Rotation,
                1e10
        );


        return constraint;
    }

    static Vector3d readVector3D(CompoundTag compoundTag) {
        return new Vector3d(compoundTag.getDouble("x"), compoundTag.getDouble("y"), compoundTag.getDouble("z"));
    }

    static CompoundTag writeVector3D(Vector3d vector3d) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putDouble("x", vector3d.x);
        compoundTag.putDouble("y", vector3d.y);
        compoundTag.putDouble("z", vector3d.z);
        return compoundTag;
    }

    static Quaterniond readQuatd(CompoundTag compoundTag) {
        return new Quaterniond(compoundTag.getDouble("x"), compoundTag.getDouble("y"), compoundTag.getDouble("z"), compoundTag.getDouble("w"));
    }

    static CompoundTag writeQuatd(Quaterniond quaterniond) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putDouble("x", quaterniond.x);
        compoundTag.putDouble("y", quaterniond.y);
        compoundTag.putDouble("z", quaterniond.z);
        compoundTag.putDouble("w", quaterniond.w);
        return compoundTag;
    }

    public static boolean isAttachedToShipOrWorld(boolean attach, Level level, Vector3d myPosCentered, Vector3d myDirNormal, CompoundTag compoundTag) {
        boolean result = false;

        if (level == null)
            return false;
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, myPosCentered);
        Ship ship2 = null;

        Vector3d tempDirNormal = new Vector3d(myDirNormal).mul(.75);
        Vector3d searchPos = new Vector3d(myPosCentered).add(tempDirNormal);
        if (ship != null)
            ship.getShipToWorld().transformPosition(searchPos, searchPos);

        BlockPos searchBlockPos = new BlockPos(toMinecraft(searchPos));
        BlockState worldBlockState = level.getBlockState(searchBlockPos);
        double distance = 0;
        if (!worldBlockState.isAir()) {
            distance = Vector3d.distance(myPosCentered.x, myPosCentered.y, myPosCentered.z, searchBlockPos.getX(), searchBlockPos.getY(), searchBlockPos.getZ());
            result = true;
        } else {
            double bounds = 0.5;
            AABB searchAABB = new AABB(searchPos.x - bounds, searchPos.y - bounds, searchPos.z - bounds,
                    searchPos.x + bounds, searchPos.y + bounds, searchPos.z + bounds);

            Iterator<Ship> ships = VSGameUtilsKt.getShipsIntersecting(level, searchAABB).iterator();
            Ship shipItr;
            Vector3d transformedSearchPos = new Vector3d(searchPos);
            if (ships.hasNext()) {
                do {
                    shipItr = ships.next();
                    if (shipItr == ship) continue;
                    shipItr.getWorldToShip().transformPosition(transformedSearchPos);
                    BlockPos blockPos = new BlockPos(toMinecraft(transformedSearchPos));
                    if (VSGameUtilsKt.isBlockInShipyard(level, blockPos)) {
                        BlockState blockState = level.getBlockState(blockPos);
                        if (!blockState.isAir() && blockState.isFaceSturdy(level, blockPos, Direction.UP, SupportType.RIGID)) {

                            searchBlockPos = new BlockPos(toMinecraft(shipItr.getShipToWorld().transformPosition(blockPos.getX(), blockPos.getY(), blockPos.getZ(), new Vector3d())));
                            distance = Vector3d.distance(myPosCentered.x, myPosCentered.y, myPosCentered.z, searchBlockPos.getX(), searchBlockPos.getY(), searchBlockPos.getZ());
                            result = true;
                            ship2 = shipItr;
                        }
                    }
                } while (ships.hasNext() && !result);
            }
        }
        if (result && !level.isClientSide && attach)
            doAttach((ServerLevel) level, ship, ship2, myPosCentered, myDirNormal, compoundTag, distance);

        return result;
    }

    public static void doAttach(ServerLevel level, Ship ship1, Ship ship2, Vector3d myPos, Vector3d myDirNormal, CompoundTag compoundTag, double distance) {
        if (ship1 == null && ship2 == null)
            return;
        removeConstraint(level, false, compoundTag);

        Vector3d adjustedDirNormal = new Vector3d(myDirNormal).mul(0.5);

        Vector3d ship1Pos = new Vector3d(myPos).add(adjustedDirNormal);
        Vector3d ship2ConstraintPos = new Vector3d(ship1Pos);
        if (distance < DISTANCE_BUFFER) {
            ship1Pos.add(new Vector3d(myDirNormal).mul(distance / -1 + DISTANCE_BUFFER));
        }
        Vector3d ship2Pos = null;
        Quaterniond ship1Rot = null;
        Quaterniond ship2Rot = null;

        if (compoundTag.contains("ShipStickerConstraint")) {

            if (compoundTag.contains("ShipStickerDistance"))
                distance = compoundTag.getDouble("ShipStickerDistance");

            if (compoundTag.contains("ShipStickerShip1Id"))
                ship1 = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(compoundTag.getLong("ShipStickerShip1Id"));

            if (compoundTag.contains("ShipStickerShip1Vec")) {
                ship1Pos = new Vector3d(readVector3D(compoundTag.getCompound("ShipStickerShip1Vec")));
            }
            if (compoundTag.contains("ShipStickerShip1Quat")) {
                ship1Rot = new Quaterniond(readQuatd(compoundTag.getCompound("ShipStickerShip1Quat")));
            }
            if (compoundTag.contains("ShipStickerShip2Id"))
                ship2 = VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(compoundTag.getLong("ShipStickerShip2Id"));
            if (compoundTag.contains("ShipStickerShip2Vec")) {
                ship2Pos = new Vector3d(readVector3D(compoundTag.getCompound("ShipStickerShip2Vec")));
            }
            if (compoundTag.contains("ShipStickerShip2Quat")) {
                ship2Rot = new Quaterniond(readQuatd(compoundTag.getCompound("ShipStickerShip2Quat")));
            }
        }

        VSAttachmentOrientationConstraint constraint = makeConstraint(ship1Pos, ship2ConstraintPos, ship1, ship2, level, ship1Rot, ship2Rot, ship2Pos);

        if (constraint != null) {

            long groundId = VSGameUtilsKt.getShipObjectWorld(level).getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId(level));
            if (constraint.getShipId0() != groundId)
                compoundTag.putLong("ShipStickerShip1Id", constraint.getShipId0());
            if (constraint.getShipId1() != groundId)
                compoundTag.putLong("ShipStickerShip2Id", constraint.getShipId1());
            compoundTag.put("ShipStickerShip1Vec", writeVector3D((Vector3d) constraint.getLocalPos0()));
            compoundTag.put("ShipStickerShip1Quat", writeQuatd((Quaterniond) constraint.getLocalRot0()));
            compoundTag.put("ShipStickerShip2Vec", writeVector3D((Vector3d) constraint.getLocalPos1()));
            compoundTag.put("ShipStickerShip2Quat", writeQuatd((Quaterniond) constraint.getLocalRot1()));
            compoundTag.putDouble("ShipStickerDistance", distance);

            Integer constraintID = VSGameUtilsKt.getShipObjectWorld(level).createNewConstraint(constraint);
            compoundTag.putInt("ShipStickerConstraint", constraintID.intValue());

            new StickerParticleUtil().doBluperParticle(level, new BlockPos(toMinecraft(myPos)), Direction.fromNormal((int) adjustedDirNormal.x, (int) adjustedDirNormal.y, (int) adjustedDirNormal.z));
        }
    }

    public static void removeConstraint(@Nullable ServerLevel level, boolean removeTags, CompoundTag compoundTag) {
        if (compoundTag.contains("ShipStickerConstraint")) {
            if (level != null)
                VSGameUtilsKt.getShipObjectWorld(level).removeConstraint(compoundTag.getInt("ShipStickerConstraint"));
            if (removeTags) {
                compoundTag.remove("ShipStickerConstraint");
                compoundTag.remove("ShipStickerShip1Id");
                compoundTag.remove("ShipStickerShip1Vec");
                compoundTag.remove("ShipStickerShip1Quat");
                compoundTag.remove("ShipStickerShip2Id");
                compoundTag.remove("ShipStickerShip2Vec");
                compoundTag.remove("ShipStickerShip2Quat");
            }
        }
    }
}
