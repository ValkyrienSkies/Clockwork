package org.valkyrienskies.clockwork.util

import org.valkyrienskies.core.api.util.DoublePair
import org.valkyrienskies.core.api.world.connectivity.Component
import org.valkyrienskies.core.api.world.connectivity.DoubleComponentAugmentation

class DoubleAvgComponentAugmentation(override val key: String) : DoubleComponentAugmentation {
    override fun combineDouble(a: Double, b: Double): Double {
        return (a + b) / 2.0
    }

    override fun splitDouble(value: Double, component1: Component, component2: Component): DoublePair {
        return DoublePair(value, value)
    }
}