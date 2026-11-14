package org.valkyrienskies.clockwork.content.curiosities.spring

import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.sin

class SpringBendingTest {
    @Test fun expandedSettingsRetainPreviousMaximumBendingResponse() {
        val normal = Vector3d(1.0, 0.0, 0.0)
        val direction = Vector3d(kotlin.math.cos(0.1), sin(0.1), 0.0)
        val torque = SpringBending.torque(normal, direction, Vector3d(0.0, 1.0, 0.0),
            Vector3d(0.0, 0.0, 0.2), 1000.0, SpringSettings(16.0, 4.0, 16.0))
        assertEquals(1000.0 * (56.0 * 4.0 * 0.1 - 10.5 * 2.0 * 0.2), torque.z, 1e-8)
        assertEquals(0.0, torque.x, 1e-9)
        assertEquals(0.0, torque.y, 1e-9)
    }

    @Test fun flippedMountGetsRestoringTorqueInsteadOfAnUndirectedAxisEquilibrium() {
        val normal = Vector3d(1.0, 0.0, 0.0)
        for (target in listOf(Vector3d(-1.0, 0.0, 0.0), Vector3d(-1.0, 0.0, 1e-5).normalize())) {
            val torque = SpringBending.torque(normal, target, Vector3d(0.0, 1.0, 0.0), Vector3d(), 1000.0, SpringSettings())
            assertTrue(torque.isFinite)
            assertTrue(torque.length() > 0.0)
            val corrected = Quaterniond().rotationAxis(0.05, torque.x / torque.length(), torque.y / torque.length(), torque.z / torque.length()).transform(normal, Vector3d())
            assertTrue(corrected.dot(target) > normal.dot(target))
        }
    }

    @Test fun alignedMountIsNeutralAndSwappingEndsStillRestoresOrientation() {
        val direction = Vector3d(1.0, 0.0, 0.0)
        assertEquals(0.0, SpringBending.torque(direction, direction, Vector3d(0.0, 1.0, 0.0), Vector3d(), 1000.0, SpringSettings()).length(), 1e-9)
        val target = Vector3d(direction).negate()
        val normal = Vector3d(0.0, 1.0, 0.0)
        val torque = SpringBending.torque(normal, target, Vector3d(1.0, 0.0, 0.0), Vector3d(), 1000.0, SpringSettings())
        assertTrue(Quaterniond().rotationAxis(0.05, torque.x / torque.length(), torque.y / torque.length(), torque.z / torque.length()).transform(normal, Vector3d()).dot(target) > normal.dot(target))
    }

    @Test fun bendingWrenchPreservesNetForceAndMomentForTwoFreeBodies() {
        val chord = Vector3d(4.0, 2.0, 1.0)
        val normal = Vector3d(0.0, 1.0, 0.0)
        val direction = Vector3d(chord).normalize()
        val a = SpringBending.torque(normal, direction, Vector3d(1.0, 0.0, 0.0), Vector3d(), 1000.0, SpringSettings())
        val b = SpringBending.torque(Vector3d(normal).negate(), Vector3d(direction).negate(), Vector3d(1.0, 0.0, 0.0), Vector3d(), 1000.0, SpringSettings())
        val forceA = SpringBending.reactionForce(a, b, chord)
        val forceB = Vector3d(forceA).negate()
        assertEquals(0.0, Vector3d(forceA).add(forceB).length(), 1e-9)
        assertEquals(0.0, Vector3d(a).add(b).add(chord.cross(forceB, Vector3d())).length(), 1e-8)
    }

    @Test fun zeroDampingKeepsBendingTorqueElasticAndSettingsScaleItsStrength() {
        val normal = Vector3d(1.0, 0.0, 0.0)
        val direction = Vector3d(1.0, 0.1, 0.0).normalize()
        val side = Vector3d(0.0, 1.0, 0.0)
        val base = SpringBending.torque(normal, direction, side, Vector3d(), 1000.0, SpringSettings(damping = 0.0))
        val stiffer = SpringBending.torque(normal, direction, side, Vector3d(100.0, 200.0, 300.0), 1000.0, SpringSettings(stiffness = 2.0, damping = 0.0))
        assertEquals(base.length() * 2.0, stiffer.length(), 1e-8)
    }
}
