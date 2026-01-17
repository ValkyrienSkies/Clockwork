package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.ComputerCraftAPI

object GenericPeripheralsCommon {
    fun register() {
        ComputerCraftAPI.registerGenericSource(GasHeatSource)
    }
}