package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import org.joml.Vector3d;
import org.valkyrienskies.core.apigame.constraints.VSConstraintAndId;
import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint;
import org.valkyrienskies.core.apigame.constraints.VSHingeOrientationConstraint;

public record PhysBearingUpdateData(double bearingAngle, float bearingRPM, boolean locked, VSHingeOrientationConstraint hingeConstraint, VSFixedOrientationConstraint angleConstraint) {
}
