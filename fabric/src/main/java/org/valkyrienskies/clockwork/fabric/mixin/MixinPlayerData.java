package org.valkyrienskies.clockwork.fabric.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.joml.primitives.AABBic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.AreaData;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit;
import org.valkyrienskies.clockwork.util.ClockworkUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.valkyrienskies.clockwork.util.AreaDataSerializer.*;

@Mixin(Player.class)
public abstract class MixinPlayerData extends LivingEntity implements AreaData {

    protected MixinPlayerData(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Unique public boolean shouldReset = false;
    @Unique public int resetTimer = 20;

    @Override
    public void shouldReset(boolean reset) {
        this.shouldReset = reset;
    }

    @Override
    public Optional<Vector3ic> getFirstPos() {
        return entityData.get(FIRST_POS);
    }

    @Override
    public Optional<Vector3ic> getSecondPos() {
        return entityData.get(SECOND_POS);
    }

    @Override
    public void setFirstPos(Optional<Vector3ic> pos) {
        entityData.set(FIRST_POS, pos);
    }

    @Override
    public void setSecondPos(Optional<Vector3ic> pos) {
        entityData.set(SECOND_POS, pos);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void clockwork$tickPlayer(CallbackInfo ci){
        if (shouldReset) {
            resetTimer--;
            if (resetTimer <= 0) {
                resetTimer = 20;
                shouldReset(false);
                HashSet<Set<AABBic>> clone = new HashSet<>(getArea().getSelectionClusters());

                for (Set<AABBic> aabBic : clone) {
                    getArea().dumpCluster(aabBic);
                }
            }
        }
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void addData(CallbackInfo info) {
        entityData.define(AREA_TOOLKIT, new SelectedAreaToolkit());
        entityData.define(FIRST_POS, Optional.empty());
        entityData.define(SECOND_POS, Optional.empty());
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeCWData(CompoundTag compoundTag, CallbackInfo info) {
        CompoundTag tag = new CompoundTag();
        ClockworkUtils.INSTANCE.saveArea(tag, getArea());

        if (getFirstPos().isPresent()) {
            tag.putInt("XF", getFirstPos().get().x());
            tag.putInt("YF", getFirstPos().get().y());
            tag.putInt("ZF", getFirstPos().get().z());
        }

        if (getSecondPos().isPresent()) {
            tag.putInt("XS", getSecondPos().get().x());
            tag.putInt("YS", getSecondPos().get().y());
            tag.putInt("ZS", getSecondPos().get().z());
        }

        compoundTag.put("AreaData", tag);
        compoundTag.putBoolean("Reset", shouldReset);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void readCWData(CompoundTag compoundTag, CallbackInfo info) {
        CompoundTag tag = (CompoundTag) compoundTag.get("AreaData");
        if (tag != null) {
            setArea(ClockworkUtils.INSTANCE.loadArea(tag));

            setFirstPos(Optional.of(new Vector3i(tag.getInt("XF"), tag.getInt("YF"), tag.getInt("ZF"))));
            setSecondPos(Optional.of(new Vector3i(tag.getInt("XS"), tag.getInt("YS"), tag.getInt("ZS"))));
        }

        if (compoundTag.contains("Reset")) {
            shouldReset(compoundTag.getBoolean("Reset"));
        }
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
