package org.valkyrienskies.clockwork.content.contraptions.universal_joint;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joml.Vector3dc;
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint;
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UniversalJointData {

    public Vector3dc connectedPos;

    public VSRopeConstraint constraint;
    @JsonIgnore
    public Integer constraintID;

    @Deprecated(since = "Jackson only")
    public UniversalJointData() {
        this.connectedPos = null;
        this.constraint = null;
    }

    public UniversalJointData(Vector3dc connectedPos) {
        this.connectedPos = connectedPos;
    }
}
