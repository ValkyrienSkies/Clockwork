package org.valkyrienskies.clockwork.util.gui

import net.createmod.catnip.lang.LangBuilder
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.util.gui.DuctUnits.*

object DuctTextUtil {

    @JvmStatic
    fun <T : Convertible<T>> translate(
        builder: LangBuilder,
        value: Double,
        allowSimplify: Boolean,
        fromUnit: T,
        toUnit: T? = null
    ): LangBuilder {
        val doSimplify = allowSimplify && ClockworkConfig.CLIENT.simplifyDisplayUnits
        val targetUnit = toUnit ?: fromUnit.base()
        val converted = fromUnit.convertTo(value, targetUnit)
        val (finalValue, finalKey) = targetUnit.display(converted, doSimplify)
        return builder.text(finalValue).space().translate(finalKey)
    }

    @JvmStatic
    fun translateTemperature(builder: LangBuilder, value: Double, allowSimplify: Boolean, unit: TemperatureUnit? = null): LangBuilder {
        return translate(builder, value, allowSimplify, TemperatureUnit.BASE, ClockworkConfig.CLIENT.tempDisplayUnit)
    }

    @JvmStatic
    fun translatePressure(builder: LangBuilder, value: Double, allowSimplify: Boolean, unit: PressureUnit? = null): LangBuilder {
        return translate(builder, value, allowSimplify, PressureUnit.BASE, ClockworkConfig.CLIENT.pressureDisplayUnit)
    }

    @JvmStatic
    fun translateEnergy(builder: LangBuilder, value: Double, allowSimplify: Boolean, unit: EnergyUnit? = null): LangBuilder {
        return translate(builder, value, allowSimplify, EnergyUnit.BASE, ClockworkConfig.CLIENT.energyDisplayUnit)
    }

    @JvmStatic
    fun translateVolume(builder: LangBuilder, value: Double, allowSimplify: Boolean, unit: VolumeUnit? = null): LangBuilder {
        return translate(builder, value, allowSimplify, VolumeUnit.BASE, ClockworkConfig.CLIENT.volumeDisplayUnit)
    }

    @JvmStatic
    fun translateMass(builder: LangBuilder, value: Double, allowSimplify: Boolean, unit: MassUnit? = null): LangBuilder {
        return translate(builder, value, allowSimplify, MassUnit.BASE, ClockworkConfig.CLIENT.massDisplayUnit)
    }
}