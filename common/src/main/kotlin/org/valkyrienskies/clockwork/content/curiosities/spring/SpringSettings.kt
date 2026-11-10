package org.valkyrienskies.clockwork.content.curiosities.spring

import net.minecraft.nbt.CompoundTag
import kotlin.math.round

internal data class SpringSettings(val stiffness: Double = 1.0, val damping: Double = 1.0, val twist: Double = 1.0) {
    fun save() = CompoundTag().also { it.putDouble("stiffness", stiffness); it.putDouble("damping", damping); it.putDouble("twist", twist) }
    fun adjust(field: Int, direction: Int, fine: Boolean): SpringSettings {
        val delta = direction.coerceIn(-1, 1) * if (fine) 0.025 else 0.1
        return when (field) {
            0 -> copy(stiffness = (stiffness + delta).coerceIn(0.25, 4.0))
            1 -> copy(damping = (damping + delta).coerceIn(0.0, 2.0))
            else -> copy(twist = (twist + delta).coerceIn(0.0, 4.0))
        }
    }
    fun reset(field: Int) = when (field) { 0 -> copy(stiffness = 1.0); 1 -> copy(damping = 1.0); else -> copy(twist = 1.0) }
    companion object {
        fun easeLength(current: Double, target: Double, ticks: Double = 1.0): Double {
            val travel = ticks.coerceAtLeast(0.0) / 16
            return current + (target - current).coerceIn(-travel, travel)
        }

        fun load(tag: CompoundTag): SpringSettings {
            fun value(key: String, min: Double, max: Double): Double =
                if (!tag.contains(key) || !tag.getDouble(key).isFinite()) 1.0 else tag.getDouble(key).coerceIn(min, max)
            return SpringSettings(value("stiffness", 0.25, 4.0), value("damping", 0.0, 2.0), value("twist", 0.0, 4.0))
        }
        fun adjustLength(length: Double, direction: Int, fine: Boolean): Double {
            val step = if (fine) 1.0 / 16 else 0.25
            return (round(length / step) * step + direction.coerceIn(-1, 1) * step).coerceIn(0.5, 128.0)
        }
    }
}
