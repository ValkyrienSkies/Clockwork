package org.valkyrienskies.clockwork.content.contraptions.reaction_wheel;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3ic;

import java.util.List;

public class ReactionWheelData {

    public final Vector3dc wheelPos;
    public final Vector3dc wheelAxis;
    public double wheelSpeed;
    private boolean spinup;
    private boolean spindown;
    public Vector3d prevAngMomentum = new Vector3d();
    public boolean active;

    public ReactionWheelData(Vector3dc wheelPos, Vector3dc wheelAxis, double wheelSpeed, boolean spinup, boolean spindown, boolean active) {
        this.wheelPos = wheelPos;
        this.wheelAxis = wheelAxis;
        this.wheelSpeed = wheelSpeed;
        this.spinup = spinup;
        this.spindown = spindown;
        this.active = active;
    }

}
