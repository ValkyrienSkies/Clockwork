package org.valkyrienskies.clockwork.content.logistics.gas.engine

import joptsimple.internal.Strings
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import org.valkyrienskies.clockwork.util.gui.DuctUnits.MassUnit
import org.valkyrienskies.clockwork.util.gui.DuctUnits.TemperatureUnit
import kotlin.math.ceil
import kotlin.math.floor

object EngineGoggleTooltip {
    fun addGasEngineTooltip(
        tooltip: MutableList<Component>,
        temperatureEfficiency: Float,
        flowEfficiency: Float,
        isPlayerSneaking: Boolean,
        temperature: Double,
        temperatureIncrement: Double,
        flowRate: Double,
        flowRateIncrement: Double
    ) {
        ClockworkLang.translate("gui.gas_engine.info.title").forGoggles(tooltip)
        addBar(
            tooltip,
            "gui.engine.temperature",
            temperatureEfficiency,
            tierHint(
                isPlayerSneaking,
                temperatureUntilNextTierComponent(GasEngineLogic.temperatureUntilNextTier(temperature, temperatureIncrement))
            )
        )
        addBar(
            tooltip,
            "gui.engine.flowrate",
            flowEfficiency,
            tierHintPerTick(
                isPlayerSneaking,
                flowUntilNextTierComponent(flowRate, flowRateIncrement)
            )
        )
    }

    fun addSterlingEngineTooltip(
        tooltip: MutableList<Component>,
        temperatureEfficiency: Float,
        isPlayerSneaking: Boolean,
        temperature: Double,
        temperatureIncrement: Double
    ) {
        ClockworkLang.translate("gui.sterling_engine.info.title").forGoggles(tooltip)
        addBar(
            tooltip,
            "gui.engine.temperature",
            temperatureEfficiency,
            tierHint(
                isPlayerSneaking,
                temperatureUntilNextTierComponent(GasEngineLogic.temperatureUntilNextTier(temperature, temperatureIncrement))
            )
        )
    }

    private fun addBar(tooltip: MutableList<Component>, labelKey: String, efficiency: Float, hint: MutableComponent? = null) {
        ClockworkLang.builder()
            .add(barLabel(labelKey).append(barComponent(efficiency)))
            .forGoggles(tooltip, 1)
        if (hint != null) {
            ClockworkLang.builder()
                .add(hint)
                .forGoggles(tooltip, 2)
        }
    }

    private fun barLabel(labelKey: String): MutableComponent {
        return ClockworkLang.translateDirect(labelKey)
            .withStyle(ChatFormatting.GRAY)
            .append(ClockworkLang.translateDirect("${labelKey}_dots").withStyle(ChatFormatting.DARK_GRAY))
    }

    private fun barComponent(efficiency: Float): MutableComponent {
        val filled = floor(efficiency.coerceIn(0f, 1f) * GasEngineLogic.BAR_SEGMENTS).toInt()
        return Component.empty()
            .append(bars((filled - 1).coerceAtLeast(0), ChatFormatting.DARK_GREEN))
            .append(bars(if (filled > 0) 1 else 0, ChatFormatting.GREEN))
            .append(bars((GasEngineLogic.BAR_SEGMENTS - filled).coerceAtLeast(0), ChatFormatting.DARK_RED))
    }

    private fun bars(level: Int, format: ChatFormatting): MutableComponent {
        return Component.literal(Strings.repeat('|', level)).withStyle(format)
    }

    private fun tierHint(show: Boolean, amount: Component): MutableComponent? {
        if (!show) return null
        return ClockworkLang.translateDirect(
            "gui.engine.until_next_tier",
            amount
        ).withStyle(ChatFormatting.DARK_GRAY)
    }

    private fun tierHintPerTick(show: Boolean, amount: Component): MutableComponent? {
        if (!show) return null
        return ClockworkLang.translateDirect(
            "gui.engine.until_next_tier_per_tick",
            amount
        ).withStyle(ChatFormatting.DARK_GRAY)
    }

    private fun temperatureUntilNextTierComponent(value: Double): Component {
        return DuctTextUtil.translateTemperature(
            ClockworkLang.builder(),
            ceilPositive(value),
            false,
            TemperatureUnit.KELVIN
        ).component()
    }

    private fun flowUntilNextTierComponent(
        flowRate: Double,
        flowRateIncrement: Double
    ): Component {
        val kilogramsPerTick = GasEngineLogic.flowUntilNextTierKilogramsPerTick(
            flowRate,
            flowRateIncrement
        )
        return DuctTextUtil.translateMass(
            ClockworkLang.builder(),
            ceilToNextGram(kilogramsPerTick),
            true,
            MassUnit.KILOGRAM
        ).component()
    }

    private fun ceilPositive(value: Double): Double {
        return if (value <= 0.0) 0.0 else ceil(value)
    }

    private fun ceilToNextGram(kilograms: Double): Double {
        return if (kilograms <= 0.0) 0.0 else ceil(kilograms * 1000.0) / 1000.0
    }
}
