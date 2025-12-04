package org.valkyrienskies.clockwork.mixinduck;

import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesState;
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


    void setAeronautGogglesState(AeronautGogglesState state);

    AeronautGogglesState getAeronautGogglesState();

    void setFlapsAngle(float angle);

    float getFlapsAngle();

    void setPrevFlapsAngle(float angle);

    float getPrevFlapsAngle();

    void setGogglesDown(boolean down);

    boolean getGogglesDown();

    void setGogglesNeedRefresh(boolean refresh);

    boolean getGogglesNeedRefresh();
}
