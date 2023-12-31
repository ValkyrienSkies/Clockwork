package org.valkyrienskies.clockwork.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck;

@Mixin(Player.class)
public class MixinPlayer implements MixinPlayerDuck {

    @Unique
    private GravitronItem.Companion.GravitronState vs_clockwork$state;

    @Override
    public void vs_clockwork$setGravitronState(GravitronItem.Companion.GravitronState s) {
        this.vs_clockwork$state = s;
    }

    @Override
    public GravitronItem.Companion.GravitronState vs_clockwork$getGravitronState() {
        return vs_clockwork$state;
    }
}
