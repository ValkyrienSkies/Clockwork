package org.valkyrienskies.clockwork.platform.integration.cc

import dan200.computercraft.api.peripheral.IPeripheralProvider
import dev.architectury.injectables.annotations.ExpectPlatform

object ComputerCraftUtils {
    /**
     * Stupid difference in Forge/Fabric cuz for uses LazyOptional<IPeripheral> instead of IPeripheral
     */
    @JvmStatic
    @ExpectPlatform
    fun getClockworkPeripheralProvider(): IPeripheralProvider {
        throw AssertionError()
    }
}