package org.valkyrienskies.clockwork.forge.integration.cc_restiched;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlockEntity;
import org.valkyrienskies.clockwork.integration.cc.AfterblazerPeripheral;

public class ClockworkForgePeripheralProviders {
    public static void register() {
        ComputerCraftAPI.registerPeripheralProvider(new AfterblazerPeripheralProvider());
    }

    public static class AfterblazerPeripheralProvider implements IPeripheralProvider {
        @Override
        public LazyOptional<IPeripheral> getPeripheral(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction) {
            if (level.getBlockEntity(blockPos) instanceof AfterblazerBlockEntity afterblazer) {
                return LazyOptional.of(() -> new AfterblazerPeripheral(afterblazer));
            }
            return LazyOptional.empty();
        }
    }
}
