package org.valkyrienskies.clockwork

import org.valkyrienskies.clockwork.util.DoubleAvgAugmentation
import org.valkyrienskies.clockwork.util.DoubleAvgComponentAugmentation
import org.valkyrienskies.core.api.world.connectivity.DoubleAugmentation
import org.valkyrienskies.core.api.world.connectivity.DoubleComponentAugmentation
import org.valkyrienskies.core.internal.world.VsiServerShipWorld

object ClockworkAugmentations {
    private val augmentKeys: HashMap<String, DoubleAugmentation> = HashMap()
    private val componentAugmentKeys: HashMap<String, DoubleComponentAugmentation> = HashMap()

    fun registerSumAugmentation(key: String, shipObjectWorld: VsiServerShipWorld) {
        augmentKeys[key] = shipObjectWorld.createDoubleSumAugmentation("kelvin", key)
    }

    fun registerAvgAugmentation(key: String, shipObjectWorld: VsiServerShipWorld) {
        augmentKeys[key] = DoubleAvgAugmentation("kelvin:$key")
    }

    fun getAugmentation(key: String): DoubleAugmentation {
        return augmentKeys[key] ?: error("No augmentation found with key $key")
    }

    fun registerComponentSumAugmentation(key: String, shipObjectWorld: VsiServerShipWorld) {
        componentAugmentKeys[key] = shipObjectWorld.createDoubleSumComponentAugmentation("kelvin", key)
    }

    fun registerComponentAvgAugmentation(key: String, shipObjectWorld: VsiServerShipWorld) {
        componentAugmentKeys[key] = DoubleAvgComponentAugmentation("kelvin:$key")
    }

    fun getComponentAugmentation(key: String): DoubleComponentAugmentation {
        return componentAugmentKeys[key] ?: error("No component augmentation found with key $key")
    }
}
