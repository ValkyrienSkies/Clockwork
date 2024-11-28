package org.valkyrienskies.clockwork.util

import org.valkyrienskies.core.api.world.connectivity.DoubleAugmentation

class DoubleAvgAugmentation(override val key: String) : DoubleAugmentation {
    override fun combineDouble(a: Double, b: Double): Double {
        return (a + b) / 2.0
    }
}