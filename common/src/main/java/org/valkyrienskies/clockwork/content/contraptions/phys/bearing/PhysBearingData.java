package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.joml.Vector3d;
import org.joml.Vector3dc;
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PhysBearingData {

    public final Vector3dc bearingPosition;
    public final Vector3dc bearingAxis;
    public Vector3d bearingRotation;
    public float bearingRPM;
    public boolean locked;

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated
    public PhysBearingData() {
        this.bearingPosition = null;
        this.bearingRotation = null;
        this.bearingAxis = null;
    }

    public PhysBearingData(Vector3dc bearingPosition, Vector3dc bearingAxis, Vector3dc bearingRotation, float bearingRPM, boolean locked) {
        this.bearingPosition = bearingPosition;
        this.bearingAxis = bearingAxis;
        this.bearingRotation = new Vector3d(bearingRotation);
        this.bearingRPM = bearingRPM;
        this.locked = locked;
    }
}
