package org.valkyrienskies.clockwork.mixinduck;

import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.CreativeGravitronItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronState;

public interface MixinPlayerDuck {
    void setGravitronState(GravitronState state);

    GravitronState getGravitronState();

    void setGravitronDialAngle(float angle);

    float getGravitronDialAngle();

    void setPrevGravitronDialAngle(float angle);

    float getPrevGravitronDialAngle();

    void setNeedsRefresh(boolean refresh);

    boolean getNeedsRefresh();
}
