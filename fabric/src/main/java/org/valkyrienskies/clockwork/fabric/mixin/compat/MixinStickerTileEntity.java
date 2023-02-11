package org.valkyrienskies.clockwork.fabric.mixin.compat;

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
import org.valkyrienskies.clockwork.fabric.content.contraptions.sticker.StickerParticleUtil;
import org.valkyrienskies.clockwork.fabric.content.contraptions.sticker.StickerMovementBehaviour;
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
        if (getExtraCustomData().contains("ShipStickerConstraint")) {
            if (level != null)
                VSGameUtilsKt.getShipObjectWorld(level).removeConstraint(getExtraCustomData().getInt("ShipStickerConstraint"));
            if (removeTags) {
                getExtraCustomData().remove("ShipStickerConstraint");
                getExtraCustomData().remove("ShipStickerShip1Id");
                getExtraCustomData().remove("ShipStickerShip1Vec");
                getExtraCustomData().remove("ShipStickerShip1Quat");
                getExtraCustomData().remove("ShipStickerShip2Id");
                getExtraCustomData().remove("ShipStickerShip2Vec");
                getExtraCustomData().remove("ShipStickerShip2Quat");
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
        boolean shipAttached = StickerMovementBehaviour.isAttachedToShipOrWorld(false, level, toJOML(Vec3.atCenterOf(getBlockPos())), myDirNormal, getExtraCustomData());//isAttachedToShipOrWorld(false);
        if (!blockAttached && piston.getValue(0) != piston.getValue() && piston.getValue() == 1 && shipAttached) {
            new StickerParticleUtil().doBluperParticle(level, worldPosition, myDir);
        }
        if (isBlockStateExtended() && !shipStuck) {
            waitForNoPower = false;
            if (!blockAttached && shipAttached) {
                if(StickerMovementBehaviour.isAttachedToShipOrWorld(true, level, toJOML(Vec3.atCenterOf(getBlockPos())), myDirNormal, getExtraCustomData()))
                    shipStuck = true;
            }
        } else if (!isBlockStateExtended() && shipStuck) {
            if (!level.isClientSide) {
                removeConstraint((ServerLevel) level, true);
            }
            waitForNoPower = true;
        } else if (isBlockStateExtended() && !getExtraCustomData().contains("ShipStickerConstraint") && !shipStuck && !blockAttached && shipAttached && getBlockState().getValue(POWERED)) {
            waitForNoPower = false;
            if (StickerMovementBehaviour.isAttachedToShipOrWorld(true, level, toJOML(Vec3.atCenterOf(getBlockPos())), myDirNormal, getExtraCustomData())) {
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
