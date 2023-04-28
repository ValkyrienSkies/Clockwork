package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public class PhysBearingData {

    public final Vector3dc bearingPosition;
    public Vector3d bearingRotation;
    public float bearingRPM;
    public boolean locked;

    public PhysBearingData(Vector3dc bearingPosition, Vector3dc bearingRotation, float bearingRPM, boolean locked) {
        this.bearingPosition = bearingPosition;
        this.bearingRotation = new Vector3d(bearingRotation);
        this.bearingRPM = bearingRPM;
        this.locked = locked;
    }
}
