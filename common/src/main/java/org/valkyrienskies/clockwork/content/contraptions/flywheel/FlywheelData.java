package org.valkyrienskies.clockwork.content.contraptions.flywheel;

import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import org.joml.Vector3dc;

public class FlywheelData {

    private final Vector3dc pos;

    private final Vector3dc axis;

    public float speed;

    public LerpedFloat visualSpeed;

    public FlywheelData(Vector3dc pos, Vector3dc axis, LerpedFloat visualSpeed, float speed) {
        this.pos = pos;
        this.axis = axis;
        this.visualSpeed = visualSpeed;
        this.speed = speed;
    }
}
