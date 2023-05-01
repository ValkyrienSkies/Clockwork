package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.apigame.constraints.VSConstraintAndId;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PhysBearingData {

    public final Vector3dc bearingPosition;
    public final Vector3dc bearingAxis;
    public double bearingAngle;
    public float bearingRPM;
    public boolean locked;
    public long shiptraptionID;
    public boolean aligning;

    public VSConstraintAndId constraintAndId;
    public VSConstraintAndId hingeConstraintAndId;
    public VSConstraintAndId posDampConstraintAndId;
    public VSConstraintAndId rotDampConstraintAndId;
    public VSConstraintAndId angleConstraintAndId;

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated
    public PhysBearingData() {
        this.bearingPosition = null;
        this.bearingAxis = null;
        this.shiptraptionID = -1;
    }

    public PhysBearingData(Vector3dc bearingPosition, Vector3dc bearingAxis, double bearingAngle, float bearingRPM, boolean locked, long shiptraptionID, VSConstraintAndId constraintAndId, VSConstraintAndId hingeConstraintAndId, VSConstraintAndId posDampConstraintAndId, VSConstraintAndId rotDampConstraintAndId, VSConstraintAndId angleConstraintAndId) {
        this.bearingPosition = bearingPosition;
        this.bearingAxis = bearingAxis;
        this.bearingAngle = bearingAngle;
        this.bearingRPM = bearingRPM;
        this.locked = locked;
        this.shiptraptionID = shiptraptionID;
        this.constraintAndId = constraintAndId;
        this.hingeConstraintAndId = hingeConstraintAndId;
        this.posDampConstraintAndId = posDampConstraintAndId;
        this.rotDampConstraintAndId = rotDampConstraintAndId;
        this.angleConstraintAndId = angleConstraintAndId;
    }

    public void setAligning(boolean yn) {
        this.aligning = yn;
    }
}
