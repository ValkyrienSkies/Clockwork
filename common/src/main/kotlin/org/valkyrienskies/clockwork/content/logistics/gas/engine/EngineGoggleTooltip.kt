package org.valkyrienskies.clockwork.content.logistics.gas.engine

import joptsimple.internal.Strings
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.valkyrienskies.clockwork.ClockworkLang
import kotlin.math.floor

object EngineGoggleTooltip {
    fun addGasEngineTooltip(tooltip: MutableList<Component>, temperatureEfficiency: Float, flowEfficiency: Float) {
        ClockworkLang.translate("gui.gas_engine.info.title").forGoggles(tooltip)
        addBar(tooltip, "gui.engine.temperature", temperatureEfficiency)
        addBar(tooltip, "gui.engine.flowrate", flowEfficiency)
    }

    fun addSterlingEngineTooltip(tooltip: MutableList<Component>, temperatureEfficiency: Float) {
        ClockworkLang.translate("gui.sterling_engine.info.title").forGoggles(tooltip)
        addBar(tooltip, "gui.engine.temperature", temperatureEfficiency)
    }

    private fun addBar(tooltip: MutableList<Component>, labelKey: String, efficiency: Float) {
        ClockworkLang.builder()
            .add(barLabel(labelKey).append(barComponent(efficiency)))
            .forGoggles(tooltip, 1)
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
}
