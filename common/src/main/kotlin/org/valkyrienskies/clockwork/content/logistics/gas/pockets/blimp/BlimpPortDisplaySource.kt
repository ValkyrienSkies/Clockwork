package org.valkyrienskies.clockwork.content.logistics.gas.pockets.blimp

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import kotlin.math.roundToInt

class BlimpPortDisplaySource : NumericSingleLineDisplaySource() {
    override fun provideLine(context: DisplayLinkContext?, stats: DisplayTargetStats?): MutableComponent {
        val port = context?.sourceBlockEntity as? BlimpPortBlockEntity ?: return ZERO.copy()
        if (!port.hasPocket) return ClockworkLang.translate("gui.blimp_port.info.no_pocket.title").component()

        return when (context.sourceConfig().getInt("TargetData")) {
            0 -> Component.literal("${(port.inflationRatio * 100.0).roundToInt()}%")
            1 -> DuctTextUtil.translatePressure(ClockworkLang.builder(), port.balloonPressure, true).component()
            2 -> DuctTextUtil.translateTemperature(ClockworkLang.builder(), port.balloonTemperature, true).component()
            3 -> DuctTextUtil.translateVolume(ClockworkLang.builder(), port.balloonVolume, true).component()
            4 -> Component.literal(port.leakCount.toString())
            else -> ZERO.copy()
        }
    }

    override fun getName(): Component = Component.translatable("block.vs_clockwork.blimp_port")

    override fun initConfigurationWidgets(
        context: DisplayLinkContext?,
        builder: ModularGuiLineBuilder?,
        isFirstLine: Boolean
    ) {
        super.initConfigurationWidgets(context, builder, isFirstLine)
        if (isFirstLine) return

        builder?.addSelectionScrollInput(0, 137, { selectionScrollInput, _ ->
            selectionScrollInput.forOptions(
                ClockworkLang.translatedOptions("display_source.blimp_port", "inflation", "pressure", "temp", "volume", "leaks")
            )
        }, "TargetData")
    }

    override fun allowsLabeling(context: DisplayLinkContext?) = true

    override fun getPassiveRefreshTicks() = 20
}
