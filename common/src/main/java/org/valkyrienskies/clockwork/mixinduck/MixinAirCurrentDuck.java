package org.valkyrienskies.clockwork.mixinduck;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;

public interface MixinAirCurrentDuck {
    FanProcessingType getProcessingTypeFor(double temperature);

    void setOwnProcessingType(FanProcessingType type);
}
