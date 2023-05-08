package org.valkyrienskies.clockwork.content.contraptions.universal_joint;

import org.joml.Vector3dc;
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint;

public record UniversalJointUpdateData(Vector3dc connectedPos, VSRopeConstraint constraint, Integer constraintID) {
}
