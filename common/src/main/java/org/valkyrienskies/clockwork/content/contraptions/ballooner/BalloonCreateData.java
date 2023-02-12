package org.valkyrienskies.clockwork.content.contraptions.ballooner;

import net.minecraft.core.BlockPos;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.util.blocktype.EngineHeatLevel;

import java.util.Set;

public record BalloonCreateData(Vector3dc burnerPos, Set<Vector3dc> volume, float rpm, double burnTemp, EngineHeatLevel heatLevel) {
}
