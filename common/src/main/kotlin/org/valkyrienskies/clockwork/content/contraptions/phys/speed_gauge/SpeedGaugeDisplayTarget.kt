package org.valkyrienskies.clockwork.content.contraptions.phys.speed_gauge

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats
import com.simibubi.create.foundation.utility.Components
import com.simibubi.create.foundation.utility.Lang
import net.minecraft.network.chat.MutableComponent
import kotlin.math.roundToInt

class SpeedGaugeDisplayTarget: NumericSingleLineDisplaySource() {
    override fun provideLine(context: DisplayLinkContext?, stats: DisplayTargetStats?): MutableComponent {
        if (context!!.sourceBlockEntity !is SpeedGaugeBlockEntity) return ZERO.copy()
        val speed = (context.sourceBlockEntity as SpeedGaugeBlockEntity).speed
        
        return Lang.number(speed.roundToInt().toDouble())
            .space()
            .add(Components.literal("m/s"))
            .component()
    }

    override fun allowsLabeling(context: DisplayLinkContext?): Boolean {
        return true
    }
}