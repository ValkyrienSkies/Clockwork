package org.valkyrienskies.clockwork.fabric.integration.cc;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.integration.cc.ClockworkComputerCraftIntegration;

public class ClockworkFabricPeripheralProvider implements IPeripheralProvider {
    @NotNull
    @Override
    public IPeripheral getPeripheral(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction) {
        return ClockworkComputerCraftIntegration.INSTANCE.getPerpheral(level, blockPos, direction);
    }
}