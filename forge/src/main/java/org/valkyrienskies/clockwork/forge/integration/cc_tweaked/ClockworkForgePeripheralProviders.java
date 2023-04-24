package org.valkyrienskies.clockwork.forge.integration.cc_tweaked;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.ballooner.BalloonerBlockEntity;
import org.valkyrienskies.clockwork.integration.cc.AfterblazerPeripheral;
import org.valkyrienskies.clockwork.integration.cc.BalloonerPeripheral;

public class ClockworkForgePeripheralProviders {
    public static void register() {
        ComputerCraftAPI.registerPeripheralProvider(new AfterblazerPeripheralProvider());
    }

    public static class AfterblazerPeripheralProvider implements IPeripheralProvider {
        @Override
        public LazyOptional<IPeripheral> getPeripheral(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction) {
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AfterblazerBlockEntity afterblazer)
                return LazyOptional.of(() -> new AfterblazerPeripheral(afterblazer));
            else if (be instanceof BalloonerBlockEntity ballooner)
                return LazyOptional.of(() -> new BalloonerPeripheral(ballooner));
            return LazyOptional.empty();
        }
    }
}
