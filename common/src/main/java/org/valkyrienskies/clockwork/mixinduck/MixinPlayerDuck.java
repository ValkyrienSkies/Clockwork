package org.valkyrienskies.clockwork.mixinduck;

import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;

public interface MixinPlayerDuck {
    void cw_setGravitronState(GravitronItem.Companion.GravitronState state);

    GravitronItem.Companion.GravitronState cw_getGravitronState();
}
