package org.valkyrienskies.clockwork.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesState;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.CreativeGravitronItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState;
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck;

@Mixin(Player.class)
public class MixinPlayer implements MixinPlayerDuck {

    @Unique
    private GravitronState vs_clockwork$state;

    @Unique
    private AeronautGogglesState vs_clockwork$gogglesState;

    @Unique
    private float vs_clockwork$angle = 0f;

    @Unique
    private float vs_clockwork$prevAngle = 0f;

    @Unique
    private boolean vs_clockwork$needsRefresh = true;

    @Unique
    private float vs_clockwork$flapsAngle = 0f;

    @Unique
    private float vs_clockwork$prevFlapsAngle = 0f;

    @Unique
    private static final EntityDataAccessor<Boolean> vs_clockwork$GOGGLES_DOWN =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private boolean vs_clockwork$gogglesNeedRefresh = true;

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void vs_clockwork$defineGogglesData(CallbackInfo ci) {
        ((Player) (Object) this).getEntityData().define(vs_clockwork$GOGGLES_DOWN, false);
    }

    @Override
    public void setGravitronState(GravitronState s) {
        this.vs_clockwork$state = s;
    }

    @Override
    public GravitronState getGravitronState() {
        return vs_clockwork$state;
    }

    @Override
    public void setGravitronDialAngle(float angle) {
        this.vs_clockwork$angle = angle;
    }

    @Override
    public float getGravitronDialAngle() {
        return vs_clockwork$angle;
    }

    @Override
    public void setPrevGravitronDialAngle(float angle) {
        this.vs_clockwork$prevAngle = angle;
    }

    @Override
    public float getPrevGravitronDialAngle() {
        return vs_clockwork$prevAngle;
    }

    @Override
    public void setNeedsRefresh(boolean refresh) {
        this.vs_clockwork$needsRefresh = refresh;
    }

    @Override
    public boolean getNeedsRefresh() {
        return vs_clockwork$needsRefresh;
    }

    @Override
    public void setAeronautGogglesState(AeronautGogglesState state) {
        this.vs_clockwork$gogglesState = state;
    }

    @Override
    public AeronautGogglesState getAeronautGogglesState() {
        return this.vs_clockwork$gogglesState;
    }

    @Override
    public void setFlapsAngle(float angle) {
        this.vs_clockwork$flapsAngle = angle;
    }

    @Override
    public float getFlapsAngle() {
        return this.vs_clockwork$flapsAngle;
    }

    @Override
    public void setPrevFlapsAngle(float angle) {
        this.vs_clockwork$prevFlapsAngle = angle;
    }

    @Override
    public float getPrevFlapsAngle() {
        return vs_clockwork$prevFlapsAngle;
    }

    @Override
    public void setGogglesDown(boolean down) {
        ((Player) (Object) this).getEntityData().set(vs_clockwork$GOGGLES_DOWN, down);
    }

    @Override
    public boolean getGogglesDown() {
        return ((Player) (Object) this).getEntityData().get(vs_clockwork$GOGGLES_DOWN);
    }

    @Override
    public void setGogglesNeedRefresh(boolean refresh) {
        this.vs_clockwork$gogglesNeedRefresh = refresh;
    }

    @Override
    public boolean getGogglesNeedRefresh() {
        return this.vs_clockwork$gogglesNeedRefresh;
    }
}
