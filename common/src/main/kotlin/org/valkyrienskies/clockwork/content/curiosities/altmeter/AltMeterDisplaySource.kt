package org.valkyrienskies.clockwork.content.curiosities.altmeter

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import kotlin.math.roundToInt

class AltMeterDisplaySource: NumericSingleLineDisplaySource() {
    override fun provideLine(context: DisplayLinkContext?, stats: DisplayTargetStats?): MutableComponent? {
        val meter = context?.sourceBlockEntity as? AltMeterBlockEntity ?: return ZERO.copy()

        val ship = meter.level.getLoadedShipManagingPos(meter.blockPos)

        val currentHeight = ship?.transform?.shipToWorld?.transformPosition(meter.blockPos.center.toJOML())?.y?.roundToInt() ?: meter.blockPos.y
        val targetHeight = meter.triggerHeight
        val direction = meter.triggerDirection

        return when (context.sourceConfig().getInt("TargetData")) {
            0 -> ClockworkLang.text(currentHeight.toString())
                .component()
            1 -> ClockworkLang.text(targetHeight.toString())
                .component()
            2 -> ClockworkLang.text(direction.name)
                .component()
            else -> ZERO.copy() // Only reachable if the tag is corrupted
        }
    }

    override fun getName(): Component? {
        return Component.translatable("block.vs_clockwork.alt_meter") // Avoids having to add new translation keys
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
                .forOptions(ClockworkLang.translatedOptions("display_source.alt_meter", "current", "target", "direction"))
        }, "TargetData")
    }

    override fun allowsLabeling(context: DisplayLinkContext?) = true
}