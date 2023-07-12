package org.valkyrienskies.clockwork.content.propulsion.ballooner;

import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

import java.util.HashSet;

public record BalloonUpdateData(HashSet<Vector3dc> volume, float rpm, double burnTemp, LiquidFuelType fuelQuality) {
}
