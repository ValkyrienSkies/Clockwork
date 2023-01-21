package org.valkyrienskies.clockwork.platform.api;

/*
 * BlockEntity
 */
public interface IExtendedBearing {
    boolean isPropellor = false;
    boolean isFlap = false;

    void setPropellor();

    void setFlap();
}
