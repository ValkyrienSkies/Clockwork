package org.valkyrienskies.clockwork.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nonnull;

public class MinecraftUtil {

    @Nonnull
    public static Rotation between(Direction a, Direction b) {
        int diff = b.get2DDataValue() - a.get2DDataValue();
        if (diff < 0) {
            diff += 4;
        }

        return switch (diff) {
            case 0 -> Rotation.NONE;
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> throw new IllegalStateException("Unexpected value: " + diff);
        };
    }

}
