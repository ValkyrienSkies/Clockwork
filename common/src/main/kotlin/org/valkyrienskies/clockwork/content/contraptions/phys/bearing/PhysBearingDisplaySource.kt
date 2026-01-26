package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource.ZERO
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.joml.Math.toDegrees
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.content.curiosities.altmeter.AltMeterBlockEntity
import org.valkyrienskies.clockwork.platform.api.ContraptionController
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import kotlin.math.round
import kotlin.math.roundToInt

class PhysBearingDisplaySource: NumericSingleLineDisplaySource() {
    override fun provideLine(context: DisplayLinkContext?, stats: DisplayTargetStats?): MutableComponent? {
        val bearing = context?.sourceBlockEntity as? PhysBearingBlockEntity ?: return ZERO.copy()

        val angle = round(toDegrees(bearing.getActualAngle() ?: 0.0) * 10) / 10.0
        val mode = bearing.movementMode!!.get()

        return when (context.sourceConfig().getInt("TargetData")) {
            0 -> ClockworkLang.text(angle.toString())
                .component()
            1 -> ClockworkLang.text(mode.name)
                .component()
            else -> ZERO.copy() // Only reachable if the tag is corrupted
        }
    }

    override fun getName(): Component? {
        return Component.translatable("block.vs_clockwork.phys_bearing") // Avoids having to add new translation keys
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
                .forOptions(ClockworkLang.translatedOptions("display_source.phys_bearing", "current", "mode"))
        }, "TargetData")
    }

    override fun allowsLabeling(context: DisplayLinkContext?) = true
}