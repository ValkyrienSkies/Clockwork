package org.valkyrienskies.clockwork.fabric.integration.cc_restiched;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.computer.core.ServerComputer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.InputKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CCEvents {
    public static void sequencedSeatKeysUpdated(BlockPos pos, Set<InputKey> keys) {
        List<String> stringKeys = new ArrayList<>(List.of());
        for (InputKey key : keys) {
            stringKeys.add(key.toString());
        }

        Set<BlockPos> possibleComputers = Set.of(pos.above(), pos.below(), pos.north(), pos.west(), pos.east(), pos.south());
        for (ServerComputer computer : ComputerCraft.serverComputerRegistry.getComputers()) {
            if (possibleComputers.contains(computer.getPosition()))
                computer.queueEvent("command_seat_keys", stringKeys.toArray());
        }
    }
}
