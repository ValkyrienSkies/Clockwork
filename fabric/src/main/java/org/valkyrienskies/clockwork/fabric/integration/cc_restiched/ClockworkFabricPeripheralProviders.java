package org.valkyrienskies.clockwork.fabric.integration.cc_restiched;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.ballooner.BalloonerBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity;
import org.valkyrienskies.clockwork.integration.cc.AfterblazerPeripheral;
import org.valkyrienskies.clockwork.integration.cc.BalloonerPeripheral;
import org.valkyrienskies.clockwork.integration.cc.FlapBearingPeripheral;

public class ClockworkFabricPeripheralProviders {
    public static void register() {
        ComputerCraftAPI.registerPeripheralProvider(new AfterblazerPeripheralProvider());
    }

    public static class AfterblazerPeripheralProvider implements IPeripheralProvider {
        @NotNull
        @Override
        public IPeripheral getPeripheral(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull Direction direction) {
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof AfterblazerBlockEntity afterblazer)
                return new AfterblazerPeripheral(afterblazer);
            else if (be instanceof BalloonerBlockEntity ballooner)
                return new BalloonerPeripheral(ballooner);
            else if (be instanceof FlapBearingBlockEntity flap)
                return new FlapBearingPeripheral(flap);
            return null;
        }
    }
}
