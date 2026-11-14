package org.valkyrienskies.clockwork.util

import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringJoints
import org.valkyrienskies.core.internal.joints.*
import java.util.EnumMap

class ConnectedShipMassTest {
    private val pose = VSJointPose(Vector3d(), Quaterniond())
    private val ships = mutableMapOf(
        1L to ConnectedShipMass.Load(1000.0),
        2L to ConnectedShipMass.Load(2000.0),
        3L to ConnectedShipMass.Load(100_000.0),
        4L to ConnectedShipMass.Load(4000.0)
    )
    private val joints = mutableListOf<VSJoint>()
    private fun graph() = ConnectedShipMass(joints) { ships[it] }
    private fun hinge(a: Long?, b: Long) { joints.add(VSRevoluteJoint(a, pose, b, pose)) }
    private fun spring(a: Long?, b: Long) {
        val pair = SpringJoints.create(a ?: 99, b, pose, pose, 3.0, 1000.0)
        joints.add(pair.first)
        joints.add(pair.second)
    }
    private fun mass(a: Long?, b: Long?, graph: ConnectedShipMass = graph()): Double {
        val (left, right) = graph.endpoints(a, b)
        return SpringJoints.effectiveMass(left.mass, right.mass, left.anchored, right.anchored)
    }

    @Test fun totalLoadIncludesElasticConnectionsOnceAndKeepsRigidMassSeparate() {
        hinge(1, 2)
        spring(2, 3)
        joints.add(VSDistanceJoint(3, pose, 4, pose, maxDistance = 4f))
        joints.add(VSDistanceJoint(4, pose, 1, pose, maxDistance = 4f))
        val graph = ConnectedShipMass(joints, elastic = { true }) { ships[it] }
        assertEquals(107000.0, graph.total(1).mass)
        assertEquals(3000.0, graph.rigidMass(1))
        joints.clear()
        assertEquals(1000.0, graph().total(1).mass)
    }

    @Test fun totalLoadDoesNotCrossGroundOrStaticShips() {
        ships[4] = ConnectedShipMass.Load(4000.0, true)
        joints.add(VSDistanceJoint(1, pose, 4, pose, maxDistance = 4f))
        hinge(4, 3)
        joints.add(VSDistanceJoint(1, pose, null, pose, maxDistance = 4f))
        joints.add(VSDistanceJoint(2, pose, null, pose, maxDistance = 4f))
        val graph = ConnectedShipMass(joints, elastic = { true }) { ships[it] }
        assertEquals(1000.0, graph.total(1).mass)
    }

    @Test fun connectedPayloadCountsOnceAcrossLoopsAndDuplicateJoints() {
        hinge(1, 2)
        joints.add(VSFixedJoint(2, pose, 3, pose))
        joints.add(VSSphericalJoint(3, pose, 1, pose))
        hinge(2, 1)
        assertEquals(103_000.0, mass(null, 1))
        assertEquals(103_000.0, mass(3, null))
        assertEquals(1.0 / (1.0 / 103_000 + 1.0 / 4000), mass(1, 4), 1e-9)
    }

    @Test fun worldAndStaticAnchorsStopTraversalIntoOtherMechanisms() {
        ships[4] = ConnectedShipMass.Load(4000.0, true)
        hinge(1, 4)
        hinge(4, 3)
        assertEquals(2000.0, mass(1, 2))
        val (left, _) = graph().endpoints(1, 2)
        assertTrue(left.anchored)
        assertEquals(1000.0, left.mass)
        joints.clear()
        hinge(null, 1)
        hinge(null, 3)
        assertEquals(2000.0, mass(1, 2))
    }

    @Test fun groundBodyIdIsAlsoAnAnchor() {
        ships[99] = ConnectedShipMass.Load(0.0, true)
        hinge(99, 1)
        assertEquals(2000.0, mass(1, 2))
    }

    @Test fun gimbalsCarryMassButFreeSlidersDoNot() {
        val motions = EnumMap<VSD6Joint.D6Axis, VSD6Joint.D6Motion>(VSD6Joint.D6Axis::class.java)
        VSD6Joint.D6Axis.entries.forEach { motions[it] = VSD6Joint.D6Motion.LOCKED }
        motions[VSD6Joint.D6Axis.SWING1] = VSD6Joint.D6Motion.FREE
        joints.add(VSD6Joint(1, pose, 3, pose, motions = motions))
        assertEquals(101_000.0, mass(null, 1))
        motions[VSD6Joint.D6Axis.X] = VSD6Joint.D6Motion.FREE
        assertEquals(1000.0, mass(null, 1))
    }

    @Test fun springChainsCountBothSidesWithoutTraversingTheEvaluatedSpring() {
        spring(1, 2)
        spring(2, 3)
        assertEquals(103_000.0, mass(null, 1))
        assertEquals(1.0 / (1.0 / 1000 + 1.0 / 102_000), mass(1, 2), 1e-9)
        assertEquals(1.0 / (1.0 / 3000 + 1.0 / 100_000), mass(2, 3), 1e-9)
        spring(null, 1)
        val graph = ConnectedShipMass(joints, groundBody = 99) { ships[it] }
        assertEquals(103_000.0, mass(null, 1, graph))
        assertEquals(1.0 / (1.0 / 1000 + 1.0 / 102_000), mass(1, 2, graph), 1e-9)
    }

    @Test fun parallelSpringsAndRopesRetainSeparateLoadsIncludingBranches() {
        spring(1, 2)
        spring(1, 2)
        spring(2, 3)
        hinge(1, 4)
        joints.add(VSDistanceJoint(1, pose, 2, pose, maxDistance = 3f))
        val graph = ConnectedShipMass(joints, elastic = { it is VSSpringJoint || it is VSDistanceJoint }) { ships[it] }
        assertEquals(1.0 / (1.0 / 5000 + 1.0 / 102_000), mass(1, 2, graph), 1e-9)
        assertEquals(mass(1, 2, graph), mass(2, 1, graph))
    }

    @Test fun slackRopePayloadOnlyContributesOnceItsLimitIsActive() {
        spring(1, 2)
        joints.add(VSDistanceJoint(2, pose, 3, pose, maxDistance = 3f))
        val slack = ConnectedShipMass(joints) { ships[it] }
        val taut = ConnectedShipMass(joints, elastic = { it is VSSpringJoint || it is VSDistanceJoint }) { ships[it] }
        assertEquals(3000.0, mass(null, 1, slack))
        assertEquals(103_000.0, mass(null, 1, taut))
    }

    @Test fun tautWorldRopeDoesNotMakeLightSpringEndpointRigid() {
        ships[1] = ConnectedShipMass.Load(500.0)
        ships[2] = ConnectedShipMass.Load(200_000.0)
        spring(1, 2)
        val rope = VSDistanceJoint(null, pose, 1, pose, maxDistance = 4f)
        joints.add(rope)
        val freeMass = SpringJoints.effectiveMass(500.0, 200_000.0)
        assertEquals(freeMass, mass(1, 2, graph()), 1e-9)
        val taut = ConnectedShipMass(joints, elastic = { it is VSSpringJoint || it is VSDistanceJoint }) { ships[it] }
        assertFalse(taut.endpoints(1, 2).first.anchored)
        assertEquals(freeMass, mass(1, 2, taut), 1e-9)
        assertEquals(200_500.0, mass(null, 1, taut), 1e-9)
        joints.remove(rope)
        assertEquals(freeMass, mass(1, 2, graph()), 1e-9)
    }

    @Test fun elasticLoopsFallBackToSeparateRigidLoadsWithoutCountingBodiesTwice() {
        spring(1, 2)
        spring(2, 3)
        spring(3, 1)
        hinge(1, 4)
        assertEquals(1.0 / (1.0 / 5000 + 1.0 / 2000), mass(1, 2), 1e-9)
    }

    @Test fun endpointsAlreadyInSameAssemblyFallBackWithoutCountingPayloadTwice() {
        hinge(1, 2)
        hinge(2, 3)
        assertEquals(1.0 / (1.0 / 1000 + 1.0 / 2000), mass(1, 2), 1e-9)
    }

    @Test fun newSnapshotTracksMassStaticStateAndDisconnectedPayload() {
        hinge(1, 3)
        assertEquals(101_000.0, mass(null, 1))
        ships[3] = ConnectedShipMass.Load(200_000.0)
        assertEquals(201_000.0, mass(null, 1))
        ships[3] = ConnectedShipMass.Load(200_000.0, true)
        assertEquals(2000.0, mass(1, 2))
        joints.clear()
        assertEquals(1000.0, mass(null, 1))
    }

    @Test fun missingAndInvalidBodiesDoNotPoisonMassOrJoinUnrelatedShips() {
        hinge(1, 99)
        hinge(99, 3)
        assertEquals(1000.0, mass(null, 1))
        ships[99] = ConnectedShipMass.Load(Double.NaN)
        assertEquals(101_000.0, mass(null, 1))
    }
}
