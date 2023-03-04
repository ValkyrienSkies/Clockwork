package org.valkyrienskies.clockwork.forge.integration.cc_tweaked;

import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.core.ServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.InputKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CCEvents {
    public static void sequencedSeatKeysUpdated(ServerLevel level, BlockPos pos, Set<InputKey> keys) {
        List<String> stringKeys = new ArrayList<>(List.of());
        for (InputKey key : keys) {
            stringKeys.add(key.toString());
        }

        Set<BlockPos> possibleComputers = Set.of(pos.above(), pos.below(), pos.north(), pos.west(), pos.east(), pos.south());
        for (ServerComputer computer : ServerContext.get(level.getServer()).registry().getComputers()) {
            if (possibleComputers.contains(computer.getPosition()))
                computer.queueEvent("command_seat_keys", stringKeys.toArray());
        }
    }
}
