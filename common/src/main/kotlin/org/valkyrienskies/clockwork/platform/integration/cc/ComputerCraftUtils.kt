package org.valkyrienskies.clockwork.platform.integration.cc

import dan200.computercraft.api.peripheral.IPeripheralProvider

object ComputerCraftUtils {
    /**
     * Stupid difference in Forge/Fabric cuz for uses LazyOptional<IPeripheral> instead of IPeripheral
     */
    @JvmStatic
    fun getClockworkPeripheralProvider(): IPeripheralProvider {
        throw AssertionError()
    }
}