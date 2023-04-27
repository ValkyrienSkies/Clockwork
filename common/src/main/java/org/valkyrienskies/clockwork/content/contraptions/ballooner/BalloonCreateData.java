package org.valkyrienskies.clockwork.content.contraptions.ballooner;

import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

import java.util.HashSet;

public record BalloonCreateData(Vector3dc burnerPos, HashSet<Vector3dc> volume, float rpm, double burnTemp, LiquidFuelType fuelQuality) {
}
