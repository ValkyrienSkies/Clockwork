package org.valkyrienskies.clockwork.content.contraptions.tubing;

import org.joml.Vector3dc;
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint;

public record FluidSocketUpdateData(Vector3dc connectedPos, VSRopeConstraint constraint, Integer constraintID) {
}
