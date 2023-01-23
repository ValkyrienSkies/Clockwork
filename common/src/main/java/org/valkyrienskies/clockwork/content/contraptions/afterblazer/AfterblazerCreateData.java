package org.valkyrienskies.clockwork.content.contraptions.afterblazer;

import net.minecraft.core.Direction;
import org.joml.Vector2d;
import org.joml.Vector3dc;

public record AfterblazerCreateData(Direction jetDirection, double jetBurnTime, AfterblazerBlock.EngineHeatLevel heatLevel, int redstoneLevel, Vector3dc jetPos, Vector2d jetGimbal) {
}
