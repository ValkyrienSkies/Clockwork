package org.valkyrienskies.clockwork.fabric.mixin;

import com.simibubi.create.content.contraptions.chassis.StickerBlockEntity;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.fabric.content.contraptions.sticker.ISticker;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import javax.annotation.Nullable;

@Mixin(SmartBlockEntity.class)
public abstract class MixinSmartBlockEntity extends CachedRenderBBBlockEntity implements ISticker {

    public MixinSmartBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void vs_clockwork$removeConstraint(@Nullable ServerLevel level, boolean removeTags) {
        if (getCustomData().contains("ShipStickerConstraint")) {
            if (level != null)
                VSGameUtilsKt.getShipObjectWorld(level).removeConstraint(getCustomData().getInt("ShipStickerConstraint"));
            if (removeTags) {
                getCustomData().remove("ShipStickerConstraint");
                getCustomData().remove("ShipStickerShip1Id");
                getCustomData().remove("ShipStickerShip1Vec");
                getCustomData().remove("ShipStickerShip1Quat");
                getCustomData().remove("ShipStickerShip2Id");
                getCustomData().remove("ShipStickerShip2Vec");
                getCustomData().remove("ShipStickerShip2Quat");
            }
        }
    }

    @Inject(method = "destroy", at = @At("TAIL"), remap = false)
    private void vs_clockwork$destroy(CallbackInfo ci) {
        if ((SmartBlockEntity) (Object) this instanceof StickerBlockEntity) {
            if (level != null) {
                if (!level.isClientSide) {
                    vs_clockwork$removeConstraint((ServerLevel) level, true);
                }
            } else {
                throw new RuntimeException("ERROR Couldn't try to clean up constraint!");
            }
        }
    }
}
