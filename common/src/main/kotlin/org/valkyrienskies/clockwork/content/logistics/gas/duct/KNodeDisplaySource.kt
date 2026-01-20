package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.simibubi.create.api.behaviour.display.DisplaySource
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod.getKelvin
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import org.valkyrienskies.kelvin.util.INodeBlockEntity

class KNodeDisplaySource: DisplaySource() {

    override fun provideText(
        context: DisplayLinkContext?,
        stats: DisplayTargetStats?
    ): List<MutableComponent?>? {
        val node = context?.sourceBlockEntity as? INodeBlockEntity ?: return listOf()

        val data = context.sourceConfig().getInt("TargetData")

        // Give multi-line mass overview
        if (data == 0) {
            var masses = getKelvin().getGasMassAt(node.getDuctNodePosition()).mapKeys { (gas, _) -> gas.name }
            masses = masses.filter { it.value > 0 }
            val components = mutableListOf<MutableComponent>()

            if (masses.isEmpty()) {
                components.add(Component.literal("Empty"))
            }

            masses.forEach { (t, u) ->
                components.add(Component.literal("$t: ${DuctTextUtil.translateMass(ClockworkLang.builder(), u, true).string()}"))
            }
            return components
        }

        // Heat energy
        if (data == 1) {
            val energy = getKelvin().getHeatEnergy(node.getDuctNodePosition())
            return listOf(DuctTextUtil.translateEnergy(ClockworkLang.builder(), energy, true).component())
        }

        // Pressure
        if (data == 2) {
            val pressure = getKelvin().getPressureAt(node.getDuctNodePosition())
            return listOf(DuctTextUtil.translatePressure(ClockworkLang.builder(), pressure, true).component())
        }

        // Temperature
        if (data == 3) {
            val temp = getKelvin().getTemperatureAt(node.getDuctNodePosition())
            return listOf(DuctTextUtil.translateTemperature(ClockworkLang.builder(), temp, true).component())
        }

        // Only reachable if the tag is corrupted
        return listOf()
    }

    override fun getName(): Component? {
        return Component.translatable("block.vs_clockwork.duct") // Avoids having to add new translation keys
    }

    override fun initConfigurationWidgets(
        context: DisplayLinkContext?,
        builder: ModularGuiLineBuilder?,
        isFirstLine: Boolean
    ) {
        super.initConfigurationWidgets(context, builder, isFirstLine)
        if (isFirstLine) return

        builder?.addSelectionScrollInput(0, 137, { selectionScrollInput, _ ->
            selectionScrollInput
                .forOptions(ClockworkLang.translatedOptions("display_source.duct", "masses", "energy", "pressure", "temperature"))
        }, "TargetData")
    }
}