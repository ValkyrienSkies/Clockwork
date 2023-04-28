package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public record PhysBearingCreateData(Vector3dc bearingPos, Vector3d bearingRotation, float bearingRPM, boolean locked) {
}
