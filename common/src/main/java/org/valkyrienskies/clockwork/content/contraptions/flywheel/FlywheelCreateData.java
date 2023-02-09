package org.valkyrienskies.clockwork.content.contraptions.flywheel;

import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import org.joml.Vector3dc;

public record FlywheelCreateData(Vector3dc pos, Vector3dc axis, LerpedFloat visualSpeed, float speed) {
}
