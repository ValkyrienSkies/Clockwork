package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BearingAttachmentGeometryTest {
    @Test fun everyTargetFaceAlignsWithoutUsingTheTargetsBlockAxis() {
        val owner = Quaterniond().rotateXYZ(.3, -.7, .2)
        val target = Quaterniond().rotateXYZ(-.2, 1.1, .6)
        for (facing in Direction.values()) for (face in Direction.values()) {
            val neutral = BearingAttachmentGeometry.neutral(facing, face, owner, target)
            val axis = Vector3d(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble())
            val targetAxis = Vector3d(-face.stepX.toDouble(), -face.stepY.toDouble(), -face.stepZ.toDouble())
            assertTrue(neutral.transform(targetAxis).distance(axis) < 1e-9)
            assertEquals(0.0, BearingAttachmentGeometry.angle(axis, neutral, neutral), 1e-9)
            for (angle in listOf(-Math.PI, -.7, .4, Math.PI)) {
                val rotated = Quaterniond().rotationAxis(angle, axis.x, axis.y, axis.z).mul(neutral)
                assertEquals(kotlin.math.abs(angle), kotlin.math.abs(BearingAttachmentGeometry.angle(axis, rotated, neutral)), 1e-9)
            }
        }
    }

    @Test fun pivotKeepsTheAttachmentFaceFlushWithTheBearingHead() {
        val block = BlockPos(1200, 70, -500)
        for (face in Direction.values()) for (depth in listOf(.5, .65)) {
            val normal = Vector3d(face.stepX.toDouble(), face.stepY.toDouble(), face.stepZ.toDouble())
            val surface = Vector3d(block.x + .5, block.y + .5, block.z + .5).fma(.5, normal)
            val pivot = BearingAttachmentGeometry.anchor(block, face, depth)
            assertTrue(pivot.sub(surface).distance(Vector3d(normal).mul(depth)) < 1e-9)
        }
    }
}
