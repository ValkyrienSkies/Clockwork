package org.valkyrienskies.clockwork.content.propulsion.afterblazer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.joml.Vector2d;
import org.joml.Vector3dc;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AfterblazerData {

    public final Vector3dc pos;
    public final Vector3dc direction;

    public int heat;
    public Vector2d gimbal;

    public AfterblazerData(Vector3dc pos, Vector3dc direction, int heat, Vector2d gimbal) {
        this.pos = pos;
        this.direction = direction;
        this.heat = heat;
        this.gimbal = gimbal;
    }
}
