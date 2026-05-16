package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil

class GasNozzleDisplaySource : NumericSingleLineDisplaySource() {
    override fun provideLine(context: DisplayLinkContext?, stats: DisplayTargetStats?): MutableComponent? {
        val nozzle = context?.sourceBlockEntity as? GasNozzleBlockEntity ?: return ZERO.copy()
        if (!nozzle.hasPocket) return ClockworkLang.translate("gui.gas_nozzle.info.no_pocket.title").component()

        val leaks = nozzle.currentIdealOutput.toInt()
        return when (context.sourceConfig().getInt("TargetData")) {
            0 -> DuctTextUtil.translateTemperature(ClockworkLang.builder(), nozzle.pocketTemperature, true).component()
            1 -> Component.literal(leaks.toString())
            2 -> DuctTextUtil.translateVolume(ClockworkLang.builder(), nozzle.balloonVolume, true).component()
            else -> ZERO.copy() // Only reachable if the tag is corrupted
        }
    }

    override fun getName(): Component? {
        return Component.translatable("block.vs_clockwork.gas_nozzle") // Avoids having to add new translation keys
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
                .forOptions(ClockworkLang.translatedOptions("display_source.gas_nozzle", "temp", "leaks", "volume"))
        }, "TargetData")
    }

    override fun allowsLabeling(context: DisplayLinkContext?) = true

    override fun getPassiveRefreshTicks() = 40
}