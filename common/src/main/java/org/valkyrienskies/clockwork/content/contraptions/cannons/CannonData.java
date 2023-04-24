package org.valkyrienskies.clockwork.content.contraptions.cannons;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public class CannonData {
    public final Vector3dc cannonPos;
    public Vector3d recoilVector;
    public double recoilForce;

    public CannonData(Vector3dc cannonPos) {
        this.cannonPos = cannonPos;
    }
}
