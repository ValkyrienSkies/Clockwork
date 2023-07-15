package org.valkyrienskies.clockwork.content.physicalities.reaction_wheel;

import org.joml.Vector3dc;

public record ReactionWheelCreateData(Vector3dc wheelPos, Vector3dc wheelAxis, float wheelSpeed, boolean spinup, boolean spindown, boolean active, float sourceSpeed) {
}
