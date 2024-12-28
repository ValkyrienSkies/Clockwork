package org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import org.joml.Vector3dc
import org.valkyrienskies.core.apigame.joints.VSFixedJoint
import org.valkyrienskies.core.apigame.joints.VSJointAndId
import org.valkyrienskies.core.apigame.joints.VSRevoluteJoint

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PhysBearingData {
    //pos of bearing in subship coordinates
    val bearingPosition: Vector3dc?
    val bearingAxis: Vector3dc?
    var bearingAngle = 0.0
    var bearingRPM = 0f
    var locked = false
    var shiptraptionID: Long
    var aligning = false
    var attachConstraint: VSRevoluteJoint? = null

    @JsonIgnore
    var actualAngle = 0.0

    @JsonIgnore
    var attachID: Int? = null

    @JsonIgnore
    var hingeID: Int? = null

    @JsonIgnore
    var secondAttachId: Int? = null

    // Default constructor for Jackson, should never be invoked manually
    @Deprecated("")
    constructor() {
        bearingPosition = null
        bearingAxis = null
        shiptraptionID = -1
    }

    constructor(
        bearingPosition: Vector3dc?,
        bearingAxis: Vector3dc?,
        bearingAngle: Double,
        bearingRPM: Float,
        locked: Boolean,
        shiptraptionID: Long,
        constraintAndId: VSJointAndId,
    ) {
        this.bearingPosition = bearingPosition
        this.bearingAxis = bearingAxis
        this.bearingAngle = bearingAngle
        this.bearingRPM = bearingRPM
        this.locked = locked
        this.shiptraptionID = shiptraptionID
        attachConstraint = constraintAndId.joint as VSRevoluteJoint
        attachID = constraintAndId.jointId
    }
}