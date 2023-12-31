package org.valkyrienskies.clockwork.forge.mixin.compat;

import com.simibubi.create.content.contraptions.chassis.StickerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.forge.content.contraptions.sticker.StickerMovementBehaviour;
import org.valkyrienskies.clockwork.forge.content.contraptions.sticker.StickerParticleUtil;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.mixinducks.mod_compat.create.IMixinStickerTileEntity;

import javax.annotation.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;
import static org.valkyrienskies.mod.common.util.VectorConversionsMCKt.toJOML;

@Mixin(StickerBlockEntity.class)
public abstract class MixinStickerTileEntity extends SmartBlockEntity implements IMixinStickerTileEntity {
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
    private boolean vs_clockwork$waitForNoPower = false;

    @Unique
    private void vs_clockwork$removeConstraint(@Nullable ServerLevel level, boolean removeTags) {
        if (getPersistentData().contains("ShipStickerConstraint")) {
            if (level != null) {
                VSGameUtilsKt.getShipObjectWorld(level).removeConstraint(getPersistentData().getInt("ShipStickerConstraint"));
            }
            if (removeTags) {
                getPersistentData().remove("ShipStickerConstraint");
                getPersistentData().remove("ShipStickerShip1Id");
                getPersistentData().remove("ShipStickerShip1Vec");
                getPersistentData().remove("ShipStickerShip1Quat");
                getPersistentData().remove("ShipStickerShip2Id");
                getPersistentData().remove("ShipStickerShip2Vec");
                getPersistentData().remove("ShipStickerShip2Quat");
            }
        }
    }

    boolean vs_clockwork$shipStuck = false;

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void injectTick(CallbackInfo ci) {
        doTick();
    }

    private void doTick() {
        if (level == null) {
            return;
        }

        Direction myDir = this.getBlockState().getValue(DirectionalBlock.FACING);
        Vector3d myDirNormal = toJOML(Vec3.atLowerCornerOf(myDir.getNormal()));

        boolean blockAttached = isAttachedToBlock();
        boolean shipAttached = StickerMovementBehaviour.isAttachedToShipOrWorld(false, level, toJOML(Vec3.atCenterOf(getBlockPos())), myDirNormal, getPersistentData());//isAttachedToShipOrWorld(false);
        if (!blockAttached && piston.getValue(0) != piston.getValue() && piston.getValue() == 1 && shipAttached) {
            new StickerParticleUtil().doBluperParticle(level, worldPosition, myDir);
        }
        if (isBlockStateExtended() && !vs_clockwork$shipStuck) {
            //Sticker extended with no ship related thing stuck to it
            vs_clockwork$waitForNoPower = false;
            if (!blockAttached && shipAttached) {
                //no sameworld block attached but there is a ship related thing near enough
                if (StickerMovementBehaviour.isAttachedToShipOrWorld(true, level, toJOML(Vec3.atCenterOf(getBlockPos())), myDirNormal, getPersistentData())) {
                    vs_clockwork$shipStuck = true;
                }
            }
        } else if (!isBlockStateExtended() && vs_clockwork$shipStuck) {
            //Sticker retracted with ship related thing stuck to it
            if (!level.isClientSide) {
                vs_clockwork$removeConstraint((ServerLevel) level, true);
            }
            vs_clockwork$waitForNoPower = true;
        } else if (isBlockStateExtended() && !getPersistentData().contains("ShipStickerConstraint") && !vs_clockwork$shipStuck && !blockAttached && shipAttached && getBlockState().getValue(POWERED)) {
            //Sticker extended with nothing attached and is powered but there is a ship thing in range
            vs_clockwork$waitForNoPower = false;
            if (StickerMovementBehaviour.isAttachedToShipOrWorld(true, level, toJOML(Vec3.atCenterOf(getBlockPos())), myDirNormal, getPersistentData())) {
                new StickerParticleUtil().doBluperParticle(level, worldPosition, myDir);
                vs_clockwork$shipStuck = true;
            }
        }
        if (vs_clockwork$waitForNoPower && !getBlockState().getValue(POWERED)) {
            vs_clockwork$waitForNoPower = false;
            vs_clockwork$shipStuck = false;
        }
    }

    @Override
    public void destroy() {
        if (level != null) {
            if (!level.isClientSide) {
                vs_clockwork$removeConstraint((ServerLevel) level, true);
            }
        } else {
            throw new RuntimeException("ERROR Couldn't try to clean up constraint!");
        }
    }

    public boolean isAlreadyPowered(boolean reset) {
        boolean result = getPersistentData().contains("ShipStickerAlreadyPowered");
        if (reset) {
            getPersistentData().remove("ShipStickerAlreadyPowered");
        }
        return result;
    }
}
