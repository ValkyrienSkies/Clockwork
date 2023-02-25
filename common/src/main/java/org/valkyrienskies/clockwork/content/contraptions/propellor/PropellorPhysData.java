package org.valkyrienskies.clockwork.content.contraptions.propellor;

import org.joml.Vector3dc;
import org.joml.Vector3ic;

import java.util.List;

public class PropellorPhysData {
    public final Vector3dc bearingPos;
    public final Vector3dc bearingAxis;
    public final List<Vector3ic> propellorPositions;
    public double bearingAngle;
    public double bearingSpeed;
    public boolean inverted;
    public Vector3dc prevAngularMomentum;

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
