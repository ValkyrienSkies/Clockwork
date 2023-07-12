package org.valkyrienskies.clockwork.fabric.integration.cc_restiched;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.propellor.PropellorBearingBlockEntity;
import org.valkyrienskies.clockwork.content.kinetics.combustion_engine.CombustionEngineBlockEntity;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlockEntity;
import org.valkyrienskies.clockwork.content.materials.solids.colorblock.ColorBlockEntity;
import org.valkyrienskies.clockwork.content.propulsion.afterblazer.AfterblazerBlockEntity;
import org.valkyrienskies.clockwork.content.propulsion.ballooner.BalloonerBlockEntity;
import org.valkyrienskies.clockwork.integration.cc.*;
import org.valkyrienskies.clockwork.util.blocktype.ConnectedWingAlike;

public class ClockworkFabricPeripheralProviders {
    public static void register() {
        ComputerCraftAPI.registerPeripheralProvider(new ClockworkPeripheralProvider());
    }

    public static class ClockworkPeripheralProvider implements IPeripheralProvider {
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
            else if (be instanceof PropellorBearingBlockEntity propellor)
                return new PropellorBearingPeripheral(propellor);
            else if (be instanceof SequencedSeatBlockEntity seat)
                return new CommandSeatPeripheral(seat);
            else if (be instanceof CombustionEngineBlockEntity engine)
                return new CombustionEnginePeripheral(engine);
            else if (be instanceof PhysBearingBlockEntity phys)
                return new PhysBearingPeripheral(phys);
            else if (be instanceof ColorBlockEntity color)
                return new ColorPeripheral(color);
            return null;
        }
    }
}
