package org.valkyrienskies.clockwork.content.contraptions.cannons;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.joml.Vector3d;
import org.joml.Vector3dc;
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CannonData {
    public final Vector3dc cannonPos;
    public Vector3d recoilVector;

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated
    public CannonData() {
        this.cannonPos = null;
    }

    public CannonData(Vector3dc cannonPos) {
        this.cannonPos = cannonPos;
    }
}
