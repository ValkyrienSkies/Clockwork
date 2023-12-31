package org.valkyrienskies.clockwork.mixinduck;

import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;

public interface MixinPlayerDuck {
    void vs_clockwork$setGravitronState(GravitronItem.Companion.GravitronState state);

    GravitronItem.Companion.GravitronState vs_clockwork$getGravitronState();
}
