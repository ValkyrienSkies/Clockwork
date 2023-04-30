package org.valkyrienskies.clockwork.content.contraptions.afterblazer;

import net.minecraft.core.Direction;
import org.joml.Vector2dc;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

public record AfterblazerCreateData(Direction jetDirection, double jetBurnTime, LiquidFuelType heatLevel, int redstoneLevel, Vector3dc jetPos, Vector2dc jetGimbal) {
}
