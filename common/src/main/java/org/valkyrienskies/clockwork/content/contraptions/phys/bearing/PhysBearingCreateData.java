package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.constraints.VSConstraintAndId;

public record PhysBearingCreateData(Vector3dc bearingPos, Vector3dc bearingAxis, double bearingAngle, float bearingRPM, boolean locked, long shiptraptionID, VSConstraintAndId constraint, VSConstraintAndId hingeConstraint, VSConstraintAndId posDampConstraint, VSConstraintAndId rotDampConstraint) {
}
