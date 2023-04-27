package org.valkyrienskies.clockwork.content.contraptions.ballooner;

import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

import java.util.HashSet;

public class BalloonData {
    public final Vector3dc burnerPos;
    public HashSet<Vector3dc> volume;
    public float rpm;
    public double burnTemp;
    public LiquidFuelType fuelQuality;

    public BalloonData(Vector3dc burnerPos, HashSet<Vector3dc> volume, float rpm, double burnTemp, LiquidFuelType fuelQuality) {
        this.burnerPos = burnerPos;
        this.volume = volume;
        this.rpm = rpm;
        this.burnTemp = burnTemp;
        this.fuelQuality = fuelQuality;
    }
}
