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

    @Override
    public void setGravitronState(GravitronState s) {
        this.vs_clockwork$state = s;
    }

    @Override
    public GravitronState getGravitronState() {
        return vs_clockwork$state;
    }
}
