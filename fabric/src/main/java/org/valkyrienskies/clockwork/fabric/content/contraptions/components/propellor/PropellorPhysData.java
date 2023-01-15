package org.valkyrienskies.clockwork.fabric.content.contraptions.components.propellor;

import org.joml.Vector3dc;
import org.joml.Vector3ic;

import java.util.List;

public class PropellorPhysData {
    public final Vector3dc bearingPos;
    public final Vector3dc bearingAxis;
    public double bearingAngle;
    public double bearingSpeed;
    public final List<Vector3ic> propellorPositions;

    public PropellorPhysData(Vector3dc bearingPos, Vector3dc bearingAxis, double bearingAngle, double bearingSpeed, List<Vector3ic> propellorPositions) {
        this.bearingPos = bearingPos;
        this.bearingAxis = bearingAxis;
        this.bearingAngle = bearingAngle;
        this.bearingSpeed = bearingSpeed;
        this.propellorPositions = propellorPositions;
    }
}
