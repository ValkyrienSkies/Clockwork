package org.valkyrienskies.clockwork.mixin;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit;
import org.valkyrienskies.clockwork.AreaData;
import org.valkyrienskies.clockwork.util.ClockworkUtils;
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil;

import java.io.IOException;

import static org.valkyrienskies.clockwork.util.AreaDataSerializer.AREA_TOOLKIT;

@Mixin(Player.class)
public abstract class MixinPlayerData extends LivingEntity implements AreaData {

    @Unique
    public Vector3ic firstPos = null;

    @Unique
    public Vector3ic secondPos = null;

    protected MixinPlayerData(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public Vector3ic getFirstPos() {
        return firstPos;
    }

    @Override
    public Vector3ic getSecondPos() {
        return secondPos;
    }

    @Override
    public void setFirstPos(Vector3ic pos) {
        this.firstPos = pos;
    }

    @Override
    public void setSecondPos(Vector3ic pos) {
        this.secondPos = pos;
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void addData(CallbackInfo info) {
        entityData.define(AREA_TOOLKIT, new SelectedAreaToolkit());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeCWData(CompoundTag compoundTag, CallbackInfo info) {
        CompoundTag tag = new CompoundTag();
        saveArea(tag, getArea());

        if (getFirstPos() != null) {
            tag.putInt("XF", getFirstPos().x());
            tag.putInt("YF", getFirstPos().y());
            tag.putInt("ZF", getFirstPos().z());
        }

        if (getSecondPos() != null) {
            tag.putInt("XS", getSecondPos().x());
            tag.putInt("YS", getSecondPos().y());
            tag.putInt("ZS", getSecondPos().z());
        }

        compoundTag.put("AreaData", tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void readCWData(CompoundTag compoundTag, CallbackInfo info) {
        CompoundTag tag = (CompoundTag) compoundTag.get("AreaData");
        if (tag != null) {
            setArea(loadArea(tag));

            setFirstPos(new Vector3i(tag.getInt("XF"), tag.getInt("YF"), tag.getInt("ZF")));
            setSecondPos(new Vector3i(tag.getInt("XS"), tag.getInt("YS"), tag.getInt("ZS")));
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

    public SelectedAreaToolkit loadArea(CompoundTag nbt){
        var toolKit = new SelectedAreaToolkit();
        if (nbt != null) {
            var nb = nbt.getByteArray("SelectedData");
            try {
                toolKit.overwriteFrom(VSJacksonUtil.INSTANCE.getDefaultMapper().readValue(nb, SelectedAreaToolkit.class));
            } catch (IOException ignored) {
            }
        }
        return toolKit;
    }

    public CompoundTag saveArea(CompoundTag nbt, SelectedAreaToolkit area){
        try {
            nbt.putByteArray("SelectedData", VSJacksonUtil.INSTANCE.getDefaultMapper().writeValueAsBytes(area));
        } catch (JsonProcessingException ignored) {

        }
        return nbt;
    }
}
