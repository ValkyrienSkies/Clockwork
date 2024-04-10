package org.valkyrienskies.clockwork.kelvin.impl

internal object Epsilon {
    private const val EPSILON = 1e-6

    fun isEffectivelyZero(value: Double): Boolean {
        return value < EPSILON
    }
}
