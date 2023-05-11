package org.valkyrienskies.clockwork.content.contraptions.afterblazer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import net.minecraft.core.Direction;
import org.joml.Vector2dc;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AfterblazerData {
    public final Direction jetDirection;
    public double jetBurnTime;
    public LiquidFuelType heatLevel;
    public int redstoneLevel;
    public final Vector3dc jetPos;
    public Vector2dc jetGimbal;
    public boolean overYMax = false;

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated
    public AfterblazerData() {
        this.jetDirection = null;
        this.jetPos = null;
    }

    public AfterblazerData(Direction jetDirection, double jetBurnTime, LiquidFuelType heatLevel, int redstoneLevel, Vector3dc jetPos, Vector2dc jetGimbal) {
        this.jetDirection = jetDirection;
        this.jetBurnTime = jetBurnTime;
        this.heatLevel = heatLevel;
        this.redstoneLevel = redstoneLevel;
        this.jetPos = jetPos;
        this.jetGimbal = jetGimbal;
    }
}
