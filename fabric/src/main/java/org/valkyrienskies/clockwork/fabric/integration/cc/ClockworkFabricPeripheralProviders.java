package org.valkyrienskies.clockwork.fabric.integration.cc;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.integration.cc.GasHeatSource;

import static org.valkyrienskies.clockwork.integration.cc.GetPeripheralCommonKt.getPeripheralCommon;

public class ClockworkFabricPeripheralProviders {
    public static void register() {
        ComputerCraftAPI.registerPeripheralProvider(new ClockworkPeripheralProvider());
        ComputerCraftAPI.registerGenericSource(GasHeatSource.INSTANCE);
    }

    public static class ClockworkPeripheralProvider implements IPeripheralProvider {
        @Override
        public IPeripheral getPeripheral(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction) {
            return getPeripheralCommon(level, blockPos, direction);
        }
    }
}