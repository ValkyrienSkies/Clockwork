package org.valkyrienskies.clockwork.forge.integration.cc;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.integration.cc.apis.AerodynamicsAPI;
import org.valkyrienskies.clockwork.integration.cc.peripherals.generic.GasHeatMethods;

import static org.valkyrienskies.clockwork.integration.cc.GetPeripheralCommonKt.getPeripheralCommon;

public class ClockworkForgePeripheralProviders {
    public static void register() {
        ComputerCraftAPI.registerPeripheralProvider(new ClockworkPeripheralProvider());

        ComputerCraftAPI.registerGenericSource(GasHeatMethods.INSTANCE);

        ComputerCraftAPI.registerAPIFactory(computer -> AerodynamicsAPI.INSTANCE);
    }

    public static class ClockworkPeripheralProvider implements IPeripheralProvider {
        @NotNull
        @Override
        public LazyOptional<IPeripheral> getPeripheral(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction) {
            var peripheral = getPeripheralCommon(level, blockPos, direction);
            if (peripheral == null) {return LazyOptional.empty();}
            return LazyOptional.of(() -> peripheral);
        }
    }
}