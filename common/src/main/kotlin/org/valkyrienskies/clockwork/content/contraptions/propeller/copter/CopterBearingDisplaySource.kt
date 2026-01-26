package org.valkyrienskies.clockwork.content.contraptions.propeller.copter

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.mod.api.toMinecraft
import kotlin.math.round

class CopterBearingDisplaySource: NumericSingleLineDisplaySource() {
    override fun provideLine(context: DisplayLinkContext?, stats: DisplayTargetStats?): MutableComponent? {
        val copter = context?.sourceBlockEntity as? CopterBearingBlockEntity ?: return ZERO.copy()

        val currentVec = copter.tiltVector
        val targetVec = copter.targetTiltVector
        val offsetVec = copter.desiredLocalOffset

        return when (context.sourceConfig().getInt("TargetData")) {
            0 -> ClockworkLang.text(currentVec.niceString())
                .component()
            1 -> ClockworkLang.text(targetVec.niceString())
                .component()
            2 -> ClockworkLang.text(offsetVec.toMinecraft().niceString())
                .component()
            else -> ZERO.copy() // Only reachable if the tag is corrupted
        }
    }

    override fun getName(): Component? {
        return Component.translatable("block.vs_clockwork.copter_bearing") // Avoids having to add new translation keys
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
                .forOptions(ClockworkLang.translatedOptions("display_source.copter_bearing", "current", "target", "offset"))
        }, "TargetData")
    }

    override fun allowsLabeling(context: DisplayLinkContext?) = true
}

private fun Vec3.niceString(): String {
    val roundedX = round(this.x*10)/10.0
    val roundedY = round(this.y*10)/10.0
    val roundedZ = round(this.z*10)/10.0
    return "$roundedX $roundedY $roundedZ"
}