package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import org.joml.Vector3d;
import org.valkyrienskies.core.apigame.constraints.VSConstraintAndId;

public record PhysBearingUpdateData(double bearingAngle, float bearingRPM, boolean locked, VSConstraintAndId angleConstraint) {
}
