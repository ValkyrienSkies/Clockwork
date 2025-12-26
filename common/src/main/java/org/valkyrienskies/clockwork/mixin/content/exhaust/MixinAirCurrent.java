package org.valkyrienskies.clockwork.mixin.content.exhaust;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.valkyrienskies.clockwork.mixinduck.MixinAirCurrentDuck;

@Mixin(value = AirCurrent.class, remap = false)
public class MixinAirCurrent implements MixinAirCurrentDuck {
    @Unique
    FanProcessingType vs_clockwork$ownProcessingType = null;

    @ModifyVariable(method = "rebuild", at = @At("STORE"), name = "type")
    private FanProcessingType applyOwnProcessingType(FanProcessingType type) {
        return vs_clockwork$ownProcessingType;
    }

    @Override
    public FanProcessingType getProcessingTypeFor(double temperature) {
        if (temperature < 400) return null;
        if (temperature < 700) return AllFanProcessingTypes.SMOKING;
        return AllFanProcessingTypes.BLASTING;
    }

    @Override
    public void setOwnProcessingType(FanProcessingType type) {
        vs_clockwork$ownProcessingType = type;
    }
}