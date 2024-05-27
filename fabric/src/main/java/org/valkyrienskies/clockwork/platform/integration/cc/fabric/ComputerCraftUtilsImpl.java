package org.valkyrienskies.clockwork.platform.integration.cc.fabric;

import dan200.computercraft.api.peripheral.IPeripheralProvider;
import org.valkyrienskies.clockwork.integration.cc.ClockworkComputerCraftIntegration;

public class ComputerCraftUtilsImpl {
    public static IPeripheralProvider getClockworkPeripheralProvider() {
        return (level, blockPos, direction) -> ClockworkComputerCraftIntegration.INSTANCE.getPerpheral(level, blockPos, direction);
    }
}
