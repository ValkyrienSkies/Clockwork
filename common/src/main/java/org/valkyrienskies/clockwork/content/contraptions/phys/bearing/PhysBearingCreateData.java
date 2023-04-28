package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.Ship;

public record PhysBearingCreateData(Vector3dc bearingPos, Vector3dc bearingAxis, Vector3d bearingRotation, float bearingRPM, boolean locked, Ship contraptionShip) {
}
