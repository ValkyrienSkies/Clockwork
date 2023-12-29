package org.valkyrienskies.clockwork.forge.integration.cc;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

public class ClockworkForgePeripheralProviders {
    public static void register() {
        ComputerCraftAPI.registerPeripheralProvider(new ClockworkPeripheralProvider());
    }

    public static class ClockworkPeripheralProvider implements IPeripheralProvider {
        @NotNull
        @Override
        public LazyOptional<IPeripheral> getPeripheral(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction) {
            // TODO: Fix this
            /*
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AfterblazerEngineBlockEntity afterblazer)
                return LazyOptional.of(() -> new ForgeAfterblazerPeripheral(afterblazer));
            else if (be instanceof BalloonerBlockEntity ballooner)
                return LazyOptional.of(() -> new ForgeBalloonerPeripheral(ballooner));
            else if (be instanceof FlapBearingBlockEntity flap)
                return LazyOptional.of(() -> new FlapBearingPeripheral(flap));
            else if (be instanceof PropellorBearingBlockEntity propellor)
                return LazyOptional.of(() -> new PropellorBearingPeripheral(propellor));
            else if (be instanceof SequencedSeatBlockEntity seat)
                return LazyOptional.of(() -> new CommandSeatPeripheral(seat));
            else if (be instanceof CombustionEngineBlockEntity engine)
                return LazyOptional.of(() -> new ForgeCombustionEnginePeripheral(engine));
            else if (be instanceof PhysBearingBlockEntity phys)
                return LazyOptional.of(() -> new PhysBearingPeripheral(phys));
            else if (be instanceof ColorBlockEntity color)
                return LazyOptional.of(() -> new ColorPeripheral(color));
             */
            return LazyOptional.empty();
        }
    }
}