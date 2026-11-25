package org.valkyrienskies.clockwork.content.curiosities.strut

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.core.internal.joints.VSJointPose
import java.util.UUID
import kotlin.math.abs

class StrutAdjustmentTest {
    private val a = WandAnchor(BlockPos(3150000, 127, -3010000), Direction.NORTH)
    private val b = WandAnchor(BlockPos(4750000, 91, -4080000), Direction.WEST)
    private val origin = Vector3d(a.pos.x + .5, a.pos.y + .5, a.pos.z + .5)
    private val socket = b.local().let { Vector3d(it.x, it.y, it.z) }
    private val mountOffset = Vector3d(socket).sub(b.pos.x + .5, b.pos.y + .5, b.pos.z + .5)
    private val frame = Quaterniond().rotationX(.48)
    private fun link(rotation: Quaterniond) = WandLinks.Link(UUID.randomUUID(), a, b, false, 4.0,
        VSJointPose(Vector3d(origin).add(rotation.transform(Vector3d(mountOffset))).add(4.015332476, -.19, .074), Quaterniond(rotation).mul(frame)),
        VSJointPose(socket, frame), physical = true, strut = true)

    @Test fun translationQuantizesTheRelativeResultOnAllAxes() {
        val strut = link(Quaterniond())
        StrutPhysics.nudge(strut, Direction.WEST, 1, false)
        assertEquals(Vector3d(3.9375, -.1875, .0625), Vector3d(strut.pose0.pos).sub(origin).sub(mountOffset))
    }

    @Test fun rotationQuantizesTheRelativeResultAndTurnsThroughRightAngles() {
        fun radians(degrees: Double) = Math.toRadians(degrees)
        for (axis in listOf(Direction.EAST, Direction.UP, Direction.SOUTH)) {
            val unit = Vector3d(axis.stepX.toDouble(), axis.stepY.toDouble(), axis.stepZ.toDouble())
            val strut = link(Quaterniond().rotationAxis(radians(88.41), unit.x, unit.y, unit.z))
            for (i in 1..5) {
                StrutPhysics.nudge(strut, axis, 1, true)
                val expected = Quaterniond().rotationAxis(radians(87.5 + 2.5 * i), unit.x, unit.y, unit.z).mul(frame)
                assertEquals(1.0, abs(expected.dot(strut.pose0.rot)), 1e-12)
            }
        }
        val strut = link(Quaterniond().rotationZYX(radians(18.43), radians(-11.23), radians(4.51)))
        StrutPhysics.nudge(strut, Direction.NORTH, 1, true)
        val expected = Quaterniond().rotationZYX(radians(15.0), radians(-10.0), radians(5.0)).mul(frame)
        assertEquals(1.0, abs(expected.dot(strut.pose0.rot)), 1e-12)
    }
}
