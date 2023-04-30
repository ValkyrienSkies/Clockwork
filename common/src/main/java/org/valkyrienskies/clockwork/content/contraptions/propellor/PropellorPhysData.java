package org.valkyrienskies.clockwork.content.contraptions.propellor;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.joml.Vector3dc;
import org.joml.Vector3ic;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PropellorPhysData {
    public final Vector3dc bearingPos;
    public final Vector3dc bearingAxis;
    public final List<Vector3ic> propellorPositions;
    public double bearingAngle;
    public double bearingSpeed;
    public boolean inverted;
    public Vector3dc prevAngularMomentum;

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated
    public PropellorPhysData() {
        this.bearingPos = null;
        this.bearingAxis = null;
        this.propellorPositions = null;
    }

    public PropellorPhysData(Vector3dc bearingPos, Vector3dc bearingAxis, double bearingAngle, double bearingSpeed, List<Vector3ic> propellorPositions, boolean inverted) {
        this.bearingPos = bearingPos;
        this.bearingAxis = bearingAxis;
        this.bearingAngle = bearingAngle;
        this.bearingSpeed = bearingSpeed;
        this.propellorPositions = propellorPositions;
        this.inverted = inverted;
    }

    public void setPrevAngularMomentum(Vector3dc prevAngularMomentum) {
        this.prevAngularMomentum = prevAngularMomentum;
    }
    public Vector3dc getPrevAngularMomentum() {
        return prevAngularMomentum;
    }
}
