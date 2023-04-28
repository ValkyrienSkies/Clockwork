package org.valkyrienskies.clockwork.content.contraptions.afterblazer;

import org.joml.Vector2d;
import org.valkyrienskies.clockwork.util.blocktype.EngineHeatLevel;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

public record AfterblazerUpdateData(double jetBurnTime, LiquidFuelType heatLevel, int redstoneLevel, Vector2d jetGimbal) {
}
