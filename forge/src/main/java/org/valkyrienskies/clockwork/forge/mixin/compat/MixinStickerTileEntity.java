package org.valkyrienskies.clockwork.forge.mixin.compat;

import com.simibubi.create.content.contraptions.components.structureMovement.chassis.StickerBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.chassis.StickerTileEntity;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueItem;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.constraints.VSAttachmentOrientationConstraint;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Iterator;

import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toMinecraft;

@Mixin(StickerTileEntity.class)
public abstract class MixinStickerTileEntity extends SmartTileEntity {
    public MixinStickerTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    public abstract boolean isAttachedToBlock();

    @Shadow
    private LerpedFloat piston;


    @OnlyIn(Dist.CLIENT)
    @Shadow
    public abstract void playSound(boolean attach);

    @Shadow
    public abstract boolean isBlockStateExtended();

    @Unique
    private boolean lastExtended = false;

    @Unique
    private void removeConstraint(ServerLevel level) {
        if (getTileData().contains("ShipStickerConstraint")) {
            VSGameUtilsKt.getShipObjectWorld(level).removeConstraint(getTileData().getInt("ShipStickerConstraint"));
            getTileData().remove("ShipStickerConstraint");
        }
    }

    @Unique
    private void doAttach(ServerLevel level, Ship ship1, Ship ship2, Vector3d myPos, Direction myDir) {
        if (ship1 == null && ship2 == null)
            return;
        boolean world2Ship = false;
        if (ship1 == null) {
            ship1 = ship2;
            ship2 = null;
            world2Ship = true;
        }

        removeConstraint(level);

        Vector3d myDirNormal = toJOML(Vec3.atLowerCornerOf(myDir.getNormal()));
        myDirNormal.mul(0.5);
        Vector3d ship1ConstraintPos = myPos.add(myDirNormal);
        Vector3d ship2ConstraintPos = new Vector3d(ship1ConstraintPos);

        if(!world2Ship)
            ship1.getShipToWorld().transformPosition(ship2ConstraintPos, ship2ConstraintPos);
        else
            ship1.getWorldToShip().transformPosition(ship1ConstraintPos, ship1ConstraintPos);

        long ship2Id;
        Quaterniondc ship2Rotation = new Quaterniond();

        if (ship2 == null) {
            ship2Id = VSGameUtilsKt.getShipObjectWorld(level).getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId(level));
        } else {
            ship2Id = ship2.getId();
            ship2.getWorldToShip().transformPosition(ship2ConstraintPos, ship2ConstraintPos);
            ship2Rotation = ship2.getTransform().getShipToWorldRotation();
        }

        VSAttachmentOrientationConstraint constraint = new VSAttachmentOrientationConstraint(
                ship1.getId(),
                ship2Id,
                1e-9 / VSGameUtilsKt.getShipObjectWorld(level).getAllShips().getById(ship1.getId()).getInertiaData().getMass(),
                ship1ConstraintPos,
                ship2ConstraintPos,
                1e20,
                ship1.getTransform().getShipToWorldRotation(),
                ship2Rotation,
                1e20
        );
        Integer constraintID = VSGameUtilsKt.getShipObjectWorld(level).createNewConstraint(constraint);
        getTileData().putInt("ShipStickerConstraint", constraintID.intValue());
    }

    @Unique
    private boolean isAttachedToShipOrWorld(boolean attach) {
        boolean result = false;
        Vector3d myPos = toJOML(Vec3.atLowerCornerOf(getBlockPos()));
        if (level == null)
            return false;
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, myPos);
        Ship ship2 = null;
        Vector3d myPosCentered = toJOML(Vec3.atCenterOf(getBlockPos()));
        Direction myDir = this.getBlockState().getValue(DirectionalBlock.FACING);
        Vector3d myDirNormal = toJOML(Vec3.atLowerCornerOf(myDir.getNormal()));

        Vector3d searchPos = myPosCentered.add(myDirNormal);
        if (ship != null)
            ship.getShipToWorld().transformPosition(searchPos, searchPos);

        BlockState worldBlockState = level.getBlockState(new BlockPos(toMinecraft(searchPos)));
        if (!worldBlockState.isAir()) {
            result = true;
        } else {
            double bounds = 0.5;
            AABB searchAABB = new AABB(searchPos.x - bounds, searchPos.y - bounds, searchPos.z - bounds,
                    searchPos.x + bounds, searchPos.y + bounds, searchPos.z + bounds);

            Iterator<Ship> ships = VSGameUtilsKt.getShipsIntersecting(level, searchAABB).iterator();
            Ship shipItr;
            Vector3d transformedSearchPos = searchPos;
            if (ships.hasNext()) {
                do {
                    shipItr = ships.next();
                    if (shipItr == ship) continue;
                    shipItr.getWorldToShip().transformPosition(searchPos, transformedSearchPos);
                    BlockPos blockPos = new BlockPos(toMinecraft(transformedSearchPos));
                    BlockState blockState = level.getBlockState(blockPos);
                    if (!blockState.isAir() && blockState.isFaceSturdy(level, blockPos, Direction.UP, SupportType.RIGID)) {
                        result = true;
                        ship2 = shipItr;
                    }
                } while (ships.hasNext() && !result);
            }
        }
        if (result && !level.isClientSide && attach)
            doAttach((ServerLevel) level, ship, ship2, myPos, myDir);

        return result;
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void injectTick(CallbackInfo ci) {
        if (level == null)
            return;
        boolean blockAttached = isAttachedToBlock();
        boolean shipAttached = isAttachedToShipOrWorld(false);
        if (!blockAttached && piston.getValue(0) != piston.getValue() && piston.getValue() == 1 && shipAttached) {
            if (level.isClientSide) {
                BluperGlueItem.spawnParticles(level, worldPosition, getBlockState().getValue(StickerBlock.FACING), true);
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.playSound(true));
            }
        }
        if (!level.isClientSide) {
            if (lastExtended != isBlockStateExtended()) {
                if (isBlockStateExtended()) {
                    if (!blockAttached && shipAttached) {
                        isAttachedToShipOrWorld(true);
                    }
                } else {
                    removeConstraint((ServerLevel) level);
                }
            }
        }
        lastExtended = isBlockStateExtended();
    }
}
