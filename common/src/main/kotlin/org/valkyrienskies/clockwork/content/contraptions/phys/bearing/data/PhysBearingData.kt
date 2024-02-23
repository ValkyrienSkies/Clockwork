package org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import org.joml.Vector3dc
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraintAndId
import org.valkyrienskies.core.apigame.constraints.VSFixedOrientationConstraint
import org.valkyrienskies.core.apigame.constraints.VSHingeOrientationConstraint

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class PhysBearingData {
    val bearingPosition: Vector3dc?
    val bearingAxis: Vector3dc?
    var bearingAngle = 0.0
    var bearingRPM = 0f
    var locked = false
    var shiptraptionID: Long
    private var aligning = false
    var attachConstraint: VSAttachmentConstraint? = null

    @JsonIgnore
    var attachID: Int? = null
    var hingeConstraint: VSHingeOrientationConstraint? = null
    var angleConstraint: VSFixedOrientationConstraint? = null

    @JsonIgnore
    var hingeID: Int? = null

    var secondAttachConstraint: VSAttachmentConstraint? = null

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
            constraintAndId: VSConstraintAndId,
            hingeConstraintAndId: VSConstraintAndId,
            posDampConstraintAndId: VSConstraintAndId?,
            rotDampConstraintAndId: VSConstraintAndId?,
            secondAttachment: VSConstraintAndId?,
    ) {
        this.bearingPosition = bearingPosition
        this.bearingAxis = bearingAxis
        this.bearingAngle = bearingAngle
        this.bearingRPM = bearingRPM
        this.locked = locked
        this.shiptraptionID = shiptraptionID
        attachConstraint = constraintAndId.vsConstraint as VSAttachmentConstraint
        attachID = constraintAndId.constraintId
        hingeConstraint = hingeConstraintAndId.vsConstraint as VSHingeOrientationConstraint
        hingeID = hingeConstraintAndId.constraintId

        secondAttachConstraint = secondAttachment?.vsConstraint as VSAttachmentConstraint?
        secondAttachId = secondAttachment?.constraintId
    }

    fun setAligning(yn: Boolean) {
        aligning = yn
    }
}