package org.valkyrienskies.clockwork.forge.mixin.compat;

import com.simibubi.create.content.contraptions.components.structureMovement.chassis.StickerTileEntity;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.forge.content.contraptions.sticker.StickerParticleUtil;
import org.valkyrienskies.clockwork.forge.content.contraptions.sticker.StickerMovementBehaviour;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import javax.annotation.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;

@Mixin(StickerTileEntity.class)
public abstract class MixinStickerTileEntity extends SmartTileEntity {
    public MixinStickerTileEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    public abstract boolean isAttachedToBlock();

    @Shadow
    LerpedFloat piston;

    @Shadow
    public abstract boolean isBlockStateExtended();

    @Unique
    private boolean lastExtended = false;
    @Unique
    private boolean waitForNoPower = false;

    @Unique
    private void removeConstraint(@Nullable ServerLevel level, boolean removeTags) {
        if (getTileData().contains("ShipStickerConstraint")) {
            if (level != null)
                VSGameUtilsKt.getShipObjectWorld(level).removeConstraint(getTileData().getInt("ShipStickerConstraint"));
            if (removeTags) {
                getTileData().remove("ShipStickerConstraint");
                getTileData().remove("ShipStickerShip1Id");
                getTileData().remove("ShipStickerShip1Vec");
                getTileData().remove("ShipStickerShip1Quat");
                getTileData().remove("ShipStickerShip2Id");
                getTileData().remove("ShipStickerShip2Vec");
                getTileData().remove("ShipStickerShip2Quat");
            }
        }
    }

    boolean shipStuck = false;

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void injectTick(CallbackInfo ci) {
        doTick();
    }

    private void doTick() {
        if (level == null)
            return;

        Direction myDir = this.getBlockState().getValue(DirectionalBlock.FACING);
        Vector3d myDirNormal = toJOML(Vec3.atLowerCornerOf(myDir.getNormal()));

        boolean blockAttached = isAttachedToBlock();
        boolean shipAttached = StickerMovementBehaviour.isAttachedToShipOrWorld(false, level, toJOML(Vec3.atCenterOf(getBlockPos())), myDirNormal, getTileData());//isAttachedToShipOrWorld(false);
        if (!blockAttached && piston.getValue(0) != piston.getValue() && piston.getValue() == 1 && shipAttached) {
            new StickerParticleUtil().doBluperParticle(level, worldPosition, myDir);
        }
        if (isBlockStateExtended() && !shipStuck) {
            waitForNoPower = false;
            if (!blockAttached && shipAttached) {
                if(StickerMovementBehaviour.isAttachedToShipOrWorld(true, level, toJOML(Vec3.atCenterOf(getBlockPos())), myDirNormal, getTileData()))
                    shipStuck = true;
            }
        } else if (!isBlockStateExtended() && shipStuck) {
            if (!level.isClientSide) {
                removeConstraint((ServerLevel) level, true);
            }
            waitForNoPower = true;
        } else if (isBlockStateExtended() && !getTileData().contains("ShipStickerConstraint") && !shipStuck && !blockAttached && shipAttached && getBlockState().getValue(POWERED)) {
            waitForNoPower = false;
            if (StickerMovementBehaviour.isAttachedToShipOrWorld(true, level, toJOML(Vec3.atCenterOf(getBlockPos())), myDirNormal, getTileData())) {
                new StickerParticleUtil().doBluperParticle(level, worldPosition, myDir);
                shipStuck = true;
            }
        }
        if (waitForNoPower && !getBlockState().getValue(POWERED)) {
            waitForNoPower = false;
            shipStuck = false;
        }

        lastExtended = isBlockStateExtended();
    }

    @Override
    public void destroy() {
        if (level != null) {
            if (!level.isClientSide) {
                removeConstraint((ServerLevel) level, true);
            }
        } else throw new RuntimeException("ERROR Couldn't try to clean up constraint!");
    }
}
