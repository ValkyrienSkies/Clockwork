package org.valkyrienskies.clockwork.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.CreativeGravitronItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState;
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck;

@Mixin(Player.class)
public class MixinPlayer implements MixinPlayerDuck {

    @Unique
    private GravitronState vs_clockwork$state;

    @Unique
    private float vs_clockwork$angle = 0f;

    @Unique
    private float vs_clockwork$prevAngle = 0f;

    @Unique
    private boolean vs_clockwork$needsRefresh = true;

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
}
