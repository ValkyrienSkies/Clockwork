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
import org.valkyrienskies.clockwork.content.contraptions.combustion_engine.CombustionEngineBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatBlockEntity;
import org.valkyrienskies.clockwork.integration.cc.*;

public class ClockworkForgePeripheralProviders {
    public static void register() {
        ComputerCraftAPI.registerPeripheralProvider(new ClockworkPeripheralProvider());
    }

    public static class ClockworkPeripheralProvider implements IPeripheralProvider {
        @NotNull
        @Override
        public LazyOptional<IPeripheral> getPeripheral(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction) {
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AfterblazerBlockEntity afterblazer)
                return LazyOptional.of(() -> new AfterblazerPeripheral(afterblazer));
            else if (be instanceof BalloonerBlockEntity ballooner)
                return LazyOptional.of(() -> new BalloonerPeripheral(ballooner));
            else if (be instanceof FlapBearingBlockEntity flap)
                return LazyOptional.of(() -> new FlapBearingPeripheral(flap));
            else if (be instanceof PropellorBearingBlockEntity propellor)
                return LazyOptional.of(() -> new PropellorBearingPeripheral(propellor));
            else if (be instanceof SequencedSeatBlockEntity seat)
                return LazyOptional.of(() -> new CommandSeatPeripheral(seat));
            else if (be instanceof CombustionEngineBlockEntity engine)
                return LazyOptional.of(() -> new CombustionEnginePeripheral(engine));
            else if (be instanceof PhysBearingBlockEntity phys)
                return LazyOptional.of(() -> new PhysBearingPeripheral(phys));
            return LazyOptional.empty();
        }
    }
}
