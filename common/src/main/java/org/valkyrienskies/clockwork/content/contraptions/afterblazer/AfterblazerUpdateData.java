package org.valkyrienskies.clockwork.content.contraptions.afterblazer;

import org.joml.Vector2d;
import org.valkyrienskies.clockwork.util.blocktype.EngineHeatLevel;

public record AfterblazerUpdateData(double jetBurnTime, EngineHeatLevel heatLevel, int redstoneLevel, Vector2d jetGimbal) {
}
