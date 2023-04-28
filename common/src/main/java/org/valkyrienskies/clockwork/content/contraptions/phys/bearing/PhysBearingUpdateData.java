package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import org.joml.Vector3d;

public record PhysBearingUpdateData(Vector3d bearingRotation, float bearingRPM, boolean locked) {
}
