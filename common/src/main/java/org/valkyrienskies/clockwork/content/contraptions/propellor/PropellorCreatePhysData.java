package org.valkyrienskies.clockwork.content.contraptions.propellor;

import org.joml.Vector3dc;
import org.joml.Vector3ic;

import java.util.List;

public record PropellorCreatePhysData(Vector3dc bearingPos, Vector3dc bearingAxis, double bearingAngle,
                                      double bearingSpeed, List<Vector3ic> propellorPositions, boolean inverted) {

}
