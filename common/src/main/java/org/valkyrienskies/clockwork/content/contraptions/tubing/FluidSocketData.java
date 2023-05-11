package org.valkyrienskies.clockwork.content.contraptions.tubing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joml.Vector3dc;
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FluidSocketData {

    public Vector3dc connectedPos;

    public VSRopeConstraint constraint;
    @JsonIgnore
    public Integer constraintID;

    @Deprecated(since = "Jackson only")
    public FluidSocketData() {
        this.connectedPos = null;
        this.constraint = null;
    }

    public FluidSocketData(Vector3dc connectedPos) {
        this.connectedPos = connectedPos;
    }
}
