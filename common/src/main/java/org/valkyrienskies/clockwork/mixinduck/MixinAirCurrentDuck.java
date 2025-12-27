package org.valkyrienskies.clockwork.mixinduck;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;

public interface MixinAirCurrentDuck {
    FanProcessingType getProcessingTypeFor(double temperature);

    void disableParticles(boolean disabled);

    void setOwnProcessingType(FanProcessingType type);
}
