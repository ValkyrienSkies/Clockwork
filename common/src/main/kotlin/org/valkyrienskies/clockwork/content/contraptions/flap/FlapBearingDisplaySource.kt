package org.valkyrienskies.clockwork.content.contraptions.flap

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.valkyrienskies.clockwork.ClockworkLang

class FlapBearingDisplaySource: NumericSingleLineDisplaySource() {
    override fun provideLine(context: DisplayLinkContext?, stats: DisplayTargetStats?): MutableComponent? {
        val bearing = context?.sourceBlockEntity as? FlapBearingBlockEntity ?: return ZERO.copy()

        return ClockworkLang.text(bearing.getInterpolatedAngle(0.0f).toString()).component()
    }

    override fun getName(): Component? {
        return Component.literal(Component.translatable("block.vs_clockwork.andesite_flap_bearing").string + " " + Component.translatable("sequenced_seat.value.angle").string) // Avoids having to add new translation keys
    }

    override fun allowsLabeling(context: DisplayLinkContext?) = true
}