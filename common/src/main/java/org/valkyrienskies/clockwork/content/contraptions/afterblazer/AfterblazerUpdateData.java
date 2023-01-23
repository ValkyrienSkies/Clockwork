package org.valkyrienskies.clockwork.content.contraptions.afterblazer;

import org.joml.Vector2d;

public record AfterblazerUpdateData(double jetBurnTime, AfterblazerBlock.EngineHeatLevel heatLevel, int redstoneLevel, Vector2d jetGimbal) {
}
