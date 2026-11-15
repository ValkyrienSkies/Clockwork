package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

object BearingAttachmentGeometry {
    fun anchor(block: BlockPos, face: Direction, pivotDepth: Double): Vector3d =
        Vector3d(block.x + .5, block.y + .5, block.z + .5)
            .fma(.5 + pivotDepth, Vector3d(face.stepX.toDouble(), face.stepY.toDouble(), face.stepZ.toDouble()))

    fun neutral(ownerFace: Direction, targetFace: Direction, ownerRotation: Quaterniondc, targetRotation: Quaterniondc): Quaterniond {
        val relative = Quaterniond(ownerRotation).invert().mul(targetRotation)
        val targetAxis = Vector3d(-targetFace.stepX.toDouble(), -targetFace.stepY.toDouble(), -targetFace.stepZ.toDouble())
        val ownerAxis = Vector3d(ownerFace.stepX.toDouble(), ownerFace.stepY.toDouble(), ownerFace.stepZ.toDouble())
        val base = Quaterniond().rotationTo(targetAxis, ownerAxis)
        var nearest = base
        for (quarter in 1..3) {
            val candidate = Quaterniond().rotationAxis(quarter * PI / 2, ownerAxis.x, ownerAxis.y, ownerAxis.z).mul(base)
            if (abs(candidate.dot(relative)) > abs(nearest.dot(relative))) nearest = candidate
        }
        return nearest.normalize()
    }

    fun angle(axis: Vector3dc, relative: Quaterniondc, neutral: Quaterniondc): Double {
        val perpendicular = if (kotlin.math.abs(axis.x()) > .9) Vector3d(0.0, 1.0, 0.0) else Vector3d(1.0, 0.0, 0.0)
        val turned = Quaterniond(relative).mul(Quaterniond(neutral).invert()).transform(Vector3d(perpendicular))
        return atan2(axis.dot(Vector3d(perpendicular).cross(turned)), perpendicular.dot(turned))
    }
}
