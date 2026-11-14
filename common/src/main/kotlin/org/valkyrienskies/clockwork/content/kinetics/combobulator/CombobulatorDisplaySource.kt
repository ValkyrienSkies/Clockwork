package org.valkyrienskies.clockwork.content.kinetics.combobulator

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

class CombobulatorDisplaySource: NumericSingleLineDisplaySource() {
    override fun provideLine(context: DisplayLinkContext?, stats: DisplayTargetStats?): MutableComponent {
        val combobulator = context?.sourceBlockEntity as? CombobulatorBlockEntity ?: return ZERO.copy()

        return Component.literal(combobulator.value(3).toString())
    }

    override fun getName(): Component = Component.translatable("block.vs_clockwork.combobulator")

    override fun allowsLabeling(context: DisplayLinkContext?) = true
}
