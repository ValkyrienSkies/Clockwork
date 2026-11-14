package org.valkyrienskies.clockwork.api.thinkable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections;

public final class Thinkables {
    private Thinkables() {}

    @Nullable
    public static Thinkable find(Level level, BlockPos pos) {
        return ThinkableConnections.INSTANCE.device(level, pos);
    }

    /** Pulls an input on the server */
    public static ThinkableInput input(ServerLevel level, BlockPos pos, Direction side) {
        var input = ThinkableConnections.INSTANCE.input(level, pos, side);
        return new ThinkableInput(input != null, input == null ? null : input.getValue());
    }
}
