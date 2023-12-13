package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import org.valkyrienskies.clockwork.content.curiosities.tools.bluper.SelectedAreaToolkit
import java.util.*

interface AreaData {
    companion object {
        @JvmStatic
        fun of(context: Any?): Optional<AreaData> {
            if (context is AreaData) {
                return Optional.of((context as AreaData?)!!)
            }
            return Optional.empty()
        }
    }

    fun setArea(load: SelectedAreaToolkit)

    fun getArea(): SelectedAreaToolkit

    fun getFirstPos(): Optional<BlockPos>

    fun setFirstPos(pos: Optional<BlockPos>)

    fun getSecondPos(): Optional<BlockPos>

    fun setSecondPos(pos: Optional<BlockPos>)

    fun shouldReset(reset: Boolean)

    fun clearAll()
}