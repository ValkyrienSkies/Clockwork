package org.valkyrienskies.clockwork.platform.integration.cc.forge;

import dan200.computercraft.api.peripheral.IPeripheralProvider;
import org.valkyrienskies.clockwork.forge.integration.cc.ClockworkForgePeripheralProvider;

public class ComputerCraftUtilsImpl {
    public static IPeripheralProvider getClockworkPeripheralProvider() {
        return new ClockworkForgePeripheralProvider();
    }
}
