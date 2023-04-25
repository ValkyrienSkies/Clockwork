package org.valkyrienskies.clockwork.content.contraptions.ballooner;

import net.minecraft.core.BlockPos;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.util.blocktype.EngineHeatLevel;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

import java.util.Set;

public class BalloonData {

    public final Vector3dc burnerPos;
    public Set<Vector3dc> volume;
    public float rpm;
    public double burnTemp;
    public LiquidFuelType fuelQuality;

    public BalloonData(Vector3dc burnerPos, Set<Vector3dc> volume, float rpm, double burnTemp, LiquidFuelType fuelQuality) {
        this.burnerPos = burnerPos;
        this.volume = volume;
        this.rpm = rpm;
        this.burnTemp = burnTemp;
        this.fuelQuality = fuelQuality;
    }
}
