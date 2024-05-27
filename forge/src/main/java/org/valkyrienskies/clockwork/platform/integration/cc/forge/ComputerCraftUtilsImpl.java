package org.valkyrienskies.clockwork.platform.integration.cc.forge;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.valkyrienskies.clockwork.integration.cc.ClockworkComputerCraftIntegration;

public class ComputerCraftUtilsImpl {
    public static IPeripheralProvider getClockworkPeripheralProvider() {
        return (level, blockPos, direction) -> {
            IPeripheral peripheral = ClockworkComputerCraftIntegration.INSTANCE.getPerpheral(level, blockPos, direction);
            return peripheral == null ? LazyOptional.empty() : LazyOptional.of(() -> peripheral);
        };
    }
}
