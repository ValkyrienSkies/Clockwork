package org.valkyrienskies.clockwork.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck;

@Mixin(Player.class)
public class MixinPlayer implements MixinPlayerDuck {

//    @Unique
//    private GravitronItem.GravitronState state;
//
//    @Override
//    public void cw_setGravitronState(GravitronItem.GravitronState s) {
//        this.state = s;
//    }
//
//    @Override
//    public GravitronItem.GravitronState cw_getGravitronState() {
//        return state;
//    }
}
