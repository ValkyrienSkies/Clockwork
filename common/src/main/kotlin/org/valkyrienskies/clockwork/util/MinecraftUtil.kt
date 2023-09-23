package org.valkyrienskies.clockwork.util

import net.minecraft.core.Direction
import net.minecraft.world.level.block.Rotation
import javax.annotation.Nonnull

object MinecraftUtil {
    @Nonnull
    fun between(a: Direction, b: Direction): Rotation {
        var diff = b.get2DDataValue() - a.get2DDataValue()
        if (diff < 0) {
            diff += 4
        }
        return when (diff) {
            0 -> Rotation.NONE
            1 -> Rotation.CLOCKWISE_90
            2 -> Rotation.CLOCKWISE_180
            3 -> Rotation.COUNTERCLOCKWISE_90
            else -> throw IllegalStateException("Unexpected value: $diff")
        }
    }
}