package org.valkyrienskies.clockwork.content.contraptions.afterblazer;

import net.minecraft.core.Direction;
import org.joml.Quaterniond;
import org.joml.Vector2d;
import org.joml.Vector3dc;

public class AfterblazerData {
    public final Direction jetDirection;
    public double jetBurnTime;
    public AfterblazerBlock.EngineHeatLevel heatLevel;
    public int redstoneLevel;
    public final Vector3dc jetPos;
    public Vector2d jetGimbal;
    public AfterblazerData(Direction jetDirection, double jetBurnTime, AfterblazerBlock.EngineHeatLevel heatLevel, int redstoneLevel, Vector3dc jetPos, Vector2d jetGimbal) {
        this.jetDirection = jetDirection;
        this.jetBurnTime = jetBurnTime;
        this.heatLevel = heatLevel;
        this.redstoneLevel = redstoneLevel;
        this.jetPos = jetPos;
        this.jetGimbal = jetGimbal;
    }
}
