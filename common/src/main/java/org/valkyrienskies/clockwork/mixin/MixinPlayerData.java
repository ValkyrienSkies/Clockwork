package org.valkyrienskies.clockwork.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit;
import org.valkyrienskies.clockwork.AreaData;
import org.valkyrienskies.clockwork.content.sterner_silly_stuff.CWAreaDataHelper;
import org.valkyrienskies.clockwork.util.CWEntityDataSerializers;

import java.util.HashSet;

import static org.valkyrienskies.clockwork.util.AreaDataSerializer.AREA_TOOLKIT;

@Mixin(Player.class)
public abstract class MixinPlayerData extends LivingEntity implements AreaData {

    protected MixinPlayerData(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void addData(CallbackInfo info) {
        entityData.define(AREA_TOOLKIT, new SelectedAreaToolkit());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeCWData(CompoundTag compoundTag, CallbackInfo info) {
        CompoundTag tag = new CompoundTag();
        CWAreaDataHelper.Companion.save(tag, getArea());
        compoundTag.put("AreaData", tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void readCWData(CompoundTag compoundTag, CallbackInfo info) {
        CompoundTag tag = (CompoundTag) compoundTag.get("AreaData");
        if (tag != null) {
            setArea(CWAreaDataHelper.Companion.load(tag));
        }
    }

    @Override
    public void removeArea(SelectedAreaToolkit kit) {
        setArea(new SelectedAreaToolkit());
    }

    @Override
    public void setArea(SelectedAreaToolkit load) {
        entityData.set(AREA_TOOLKIT, load);
    }

    @Override
    public SelectedAreaToolkit getArea() {
        return entityData.get(AREA_TOOLKIT);
    }
}
