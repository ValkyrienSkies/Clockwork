package org.valkyrienskies.clockwork

import org.valkyrienskies.core.api.world.connectivity.DoubleAugmentation
import org.valkyrienskies.core.api.world.connectivity.DoubleComponentAugmentation
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore

object ClockworkAugmentations {
    private val augmentKeys: HashMap<String, DoubleAugmentation> = HashMap()
    private val componentAugmentKeys: HashMap<String, DoubleComponentAugmentation> = HashMap()

    fun registerAugmentation(key: String, shipObjectWorld: ServerShipWorldCore) {
        augmentKeys[key] = shipObjectWorld.createDoubleSumAugmentation("clockwork", key)
    }

    fun getAugmentation(key: String): DoubleAugmentation {
        return augmentKeys[key] ?: error("No augmentation found with key $key")
    }

    fun registerComponentAugmentation(key: String, shipObjectWorld: ServerShipWorldCore) {
        componentAugmentKeys[key] = shipObjectWorld.createDoubleSumComponentAugmentation("clockwork", key)
    }

    fun getComponentAugmentation(key: String): DoubleComponentAugmentation {
        return componentAugmentKeys[key] ?: error("No component augmentation found with key $key")
    }
}