package org.valkyrienskies.clockwork.platform.integration.cc.fabric;

import dan200.computercraft.api.peripheral.IPeripheralProvider;
import org.valkyrienskies.clockwork.fabric.integration.cc.ClockworkFabricPeripheralProvider;

public class ComputerCraftUtilsImpl {
    public static IPeripheralProvider getClockworkPeripheralProvider() {
        return new ClockworkFabricPeripheralProvider();
    }
}
