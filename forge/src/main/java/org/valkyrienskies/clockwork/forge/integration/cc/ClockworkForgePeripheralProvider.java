package org.valkyrienskies.clockwork.forge.integration.cc;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.integration.cc.ClockworkComputerCraftIntegration;

public class ClockworkForgePeripheralProvider implements IPeripheralProvider {
    @NotNull
    @Override
    public LazyOptional<IPeripheral> getPeripheral(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction) {
        IPeripheral peripheral = ClockworkComputerCraftIntegration.INSTANCE.getPerpheral(level, blockPos, direction);
        return peripheral == null ? LazyOptional.empty() : LazyOptional.of(() -> peripheral);
    }
}