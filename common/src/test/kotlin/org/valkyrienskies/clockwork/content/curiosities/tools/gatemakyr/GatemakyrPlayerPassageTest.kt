package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.math.abs

class GatemakyrPlayerPassageTest {
    private fun floor(x: Double) = GatemakyrTarget.Frame(Vec3(x, 10.0, 0.0), Vec3(1.0, 0.0, 0.0),
        Vec3(0.0, 0.0, -1.0), Vec3(0.0, 1.0, 0.0), Vec3.ZERO)
    private fun wall(x: Double) = GatemakyrTarget.Frame(Vec3(x, 20.0, 4.0), Vec3(0.0, 0.0, 1.0),
        Vec3(0.0, 1.0, 0.0), Vec3(-1.0, 0.0, 0.0), Vec3.ZERO)

    @Test fun fallingThroughAFloorPreservesEyeInterpolationAndRotatesMomentum() {
        val passage = GatemakyrPassage(floor(0.0), wall(32.0))
        val start = Vec3(0.5, 10.2, 0.0)
        val end = Vec3(0.5, 9.8, 0.0)
        val old = passage.point(start)
        val current = passage.point(end)
        for (fraction in listOf(0.0, 0.49, 0.5, 0.51, 1.0)) {
            val expected = passage.point(start.lerp(end, fraction))
            assertEquals(0.0, expected.distanceTo(old.lerp(current, fraction)), 1e-10)
        }
        val velocity = passage.velocity(end.subtract(start), end, passage)
        assertEquals(-0.4, velocity.x, 1e-10)
        assertEquals(0.0, velocity.y, 1e-10)
    }

    @Test fun crossingPreservesTheCameraBasisEvenWhenLookingStraightUpOrDown() {
        val passage = GatemakyrPassage(floor(0.0), wall(32.0))
        fun camera(pitch: Float, yaw: Float, roll: Float) = Quaterniond().rotationYXZ(
            Math.toRadians(-yaw.toDouble()), Math.toRadians(pitch.toDouble()), Math.toRadians(roll.toDouble()))
        fun Quaterniond.rotate(axis: Vec3): Vec3 {
            val result = transform(Vector3d(axis.x, axis.y, axis.z))
            return Vec3(result.x, result.y, result.z)
        }
        for (pitch in listOf(-90f, -20f, 0f, 73f, 90f)) for (yaw in listOf(-170f, -90f, 0f, 45f, 90f, 180f)) {
            val angles = passage.angles(pitch, yaw)
            val tilt = passage.cameraRoll(yaw, angles)
            val entry = camera(pitch, yaw, 26f)
            val exit = camera(angles.x, angles.y, tilt + 26f)
            for (axis in listOf(Vec3(0.0, 0.0, 1.0), Vec3(0.0, 1.0, 0.0))) {
                val expected = passage.direction(entry.rotate(axis))
                assertEquals(0.0, expected.distanceTo(exit.rotate(axis)), 1e-6)
            }
            if (abs(angles.x) == 90f) assertEquals(0f, tilt, 1e-4f)
        }
    }

    @Test fun uprightBodyClearsTheFloorAtAWallExit() {
        val frame = wall(0.0)
        val target = GatemakyrTarget(frame.position, Direction.WEST, width = 1, height = 3)
        val eye = frame.position
        val feet = eye.subtract(0.0, 1.62, 0.0)
        val box = AABB.ofSize(feet.add(0.0, 0.9, 0.0), 0.6, 1.8, 0.6)
        val shift = requireNotNull(GatemakyrPassage.inset(box, frame, target))
        assertEquals(0.0, shift.distanceTo(Vec3(0.0, 0.12, 0.0)), 1e-8)
        assertNull(GatemakyrPassage.inset(box, frame, target.copy(height = 1)))
    }

    @Test fun movingGateTransfersPointVelocityIncludingRotation() {
        val old = GatemakyrPassage(floor(0.0), wall(30.0))
        val entry = old.entrance.copy(position = old.entrance.position.add(0.0, 2.0, 0.0))
        val current = GatemakyrPassage(entry, wall(31.0))
        val carried = current.velocity(Vec3(0.0, 2.0, 0.0), entry.position, old)
        assertEquals(0.0, carried.distanceTo(Vec3(1.0, 0.0, 0.0)), 1e-9)
        val stationary = current.velocity(Vec3.ZERO, entry.position, old)
        assertEquals(0.0, stationary.distanceTo(carried.add(current.direction(Vec3(0.0, -2.0, 0.0)))), 1e-9)
        val turned = GatemakyrPassage(entry.copy(u = entry.v, v = entry.u.scale(-1.0)), current.exit)
        val angularStep = turned.entrance.u.subtract(entry.u)
        assertEquals(0.0, turned.velocity(angularStep, entry.position.add(turned.entrance.u), current).length(), 1e-9)
    }

    @Test fun movingMouthsCarryVelocityRelativeToTheirFrames() {
        val last = GatemakyrPassage(floor(0.0), wall(32.0))
        val current = GatemakyrPassage(floor(0.25), wall(34.0))
        val velocity = current.velocity(Vec3(0.25, -0.4, 0.0), Vec3(0.25, 10.0, 0.0), last)
        assertEquals(1.6, velocity.x, 1e-10)
        assertEquals(0.0, velocity.z, 1e-10)
    }
}
