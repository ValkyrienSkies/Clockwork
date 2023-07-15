package org.valkyrienskies.clockwork.content.propulsion.afterblazer;

import org.joml.Vector2d;
import org.joml.Vector3dc;

public record AfterblazerCreateData(Vector3dc pos, Vector3dc direction, int heat, Vector2d gimbal) {
}
