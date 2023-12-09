package org.valkyrienskies.clockwork.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck;

@Mixin(Player.class)
public class MixinPlayer implements MixinPlayerDuck {

    @Unique
    private GravitronItem.Companion.GravitronState vs_clockwork_mod$_state;

    @Override
    public void cw_setGravitronState(GravitronItem.Companion.GravitronState s) {
        this.vs_clockwork_mod$_state = s;
    }

    @Override
    public GravitronItem.Companion.GravitronState cw_getGravitronState() {
        return vs_clockwork_mod$_state;
    }
}
