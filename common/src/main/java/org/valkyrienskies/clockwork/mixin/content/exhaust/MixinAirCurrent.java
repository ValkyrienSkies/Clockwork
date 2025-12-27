package org.valkyrienskies.clockwork.mixin.content.exhaust;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.createmod.catnip.config.ConfigBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.valkyrienskies.clockwork.ClockworkConfig;
import org.valkyrienskies.clockwork.mixinduck.MixinAirCurrentDuck;

@Mixin(value = AirCurrent.class, remap = false)
public class MixinAirCurrent implements MixinAirCurrentDuck {
    @Unique
    boolean vs_clockwork$disabledParticles = false;
    @Unique
    FanProcessingType vs_clockwork$ownProcessingType = null;

    // For disabling airflow particles we ensure the random check always fails
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/config/ConfigBase$ConfigFloat;get()Ljava/lang/Object;"))
    private Object replaceWithInf(ConfigBase.ConfigFloat instance, Operation<Object> original) {
        if (vs_clockwork$disabledParticles) return Double.NEGATIVE_INFINITY;
        return original.call(instance);
    }

    @ModifyVariable(method = "rebuild", at = @At("STORE"), name = "type")
    private FanProcessingType applyOwnProcessingType(FanProcessingType type) {
        return vs_clockwork$ownProcessingType;
    }

    @Override
    public FanProcessingType getProcessingTypeFor(double temperature) {
        if (temperature >= ClockworkConfig.SERVER.getBulkBlastingTemp()) return AllFanProcessingTypes.BLASTING;
        if (temperature >= ClockworkConfig.SERVER.getBulkSmokingTemp()) return AllFanProcessingTypes.SMOKING;
        return null;
    }

    @Override
    public void disableParticles(boolean disabled) {
        vs_clockwork$disabledParticles = disabled;
    }

    @Override
    public void setOwnProcessingType(FanProcessingType type) {
        vs_clockwork$ownProcessingType = type;
    }
}