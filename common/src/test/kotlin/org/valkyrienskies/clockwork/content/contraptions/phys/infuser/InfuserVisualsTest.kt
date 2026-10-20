package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.*

class InfuserVisualsTest {
    @Test fun `beam packets move from the crystal to the region`() {
        val early = InfuserVisuals.BEAM_FLIGHT + InfuserVisuals.PULSE_PERIOD * 0.2f
        val late = InfuserVisuals.BEAM_FLIGHT + InfuserVisuals.PULSE_PERIOD * 0.7f
        assertEquals(1f, InfuserVisuals.beamPulse(early, 0.2f), 0.00001f)
        assertEquals(1f, InfuserVisuals.beamPulse(late, 0.7f), 0.00001f)
        assertTrue(InfuserVisuals.beamPulse(early, 0.7f) < 0.001f)
        assertTrue(InfuserVisuals.beamPulse(late, 0.2f) < 0.001f)
        assertEquals(0f, InfuserVisuals.beamPulse(0f, 0f))
    }

    @Test fun `firing toward a region recoils the core away from it and then settles`() {
        val sequence = InfuserSequence(0, true)
        val fire = sequence.connection(0, 1)
        for (direction in listOf(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 0.0, -1.0))) {
            val recoil = InfuserVisuals.sample(sequence, fire + 1f, listOf(direction))
            val baseline = InfuserVisuals.sample(sequence, fire + 1f, emptyList())
            assertTrue(recoil.offset.subtract(baseline.offset).dot(direction) < -0.15)
            val settled = InfuserVisuals.sample(sequence, fire + 24f, listOf(direction))
            assertEquals(InfuserVisuals.sample(sequence, fire + 24f, emptyList()), settled)
        }
    }

    @Test fun `simultaneous beams cannot multiply recoil without bound`() {
        val sequence = InfuserSequence(0, false)
        val directions = List(128) { Vec3(1.0, 0.0, 0.0) }
        for (age in 0..sequence.finished) {
            val pose = InfuserVisuals.sample(sequence, age.toFloat(), directions)
            assertTrue(pose.offset.length() < 0.85)
            assertTrue(pose.scale in 1f..2.5f)
            assertTrue(pose.pitch.isFinite() && pose.roll.isFinite() && pose.yaw.isFinite())
        }
        assertEquals(InfuserVisuals.CorePose(), InfuserVisuals.sample(sequence, sequence.finished.toFloat(), directions))
    }

    @Test fun `collapse continues the existing motes without spawning an outer shell`() {
        for (first in listOf(true, false)) {
            val sequence = InfuserSequence(0, first)
            val start = sequence.impact - InfuserVisuals.collapseDuration(sequence)
            for (i in 0..63) {
                val before = InfuserVisuals.gatherPoint(sequence, start - 0.001f, i)
                val after = InfuserVisuals.gatherPoint(sequence, start + 0.001f, i)
                assertTrue(before.distanceTo(after) < 0.02)
                var radius = after.length()
                for (step in 1..20) {
                    val point = InfuserVisuals.gatherPoint(sequence, start + (sequence.impact - start) * step / 20f, i)
                    assertTrue(point.length() <= radius + 0.00001)
                    radius = point.length()
                }
                assertEquals(0.0, radius, 0.00001)
            }
        }
    }

    @Test fun `rebound covers most of its distance immediately and decelerates`() {
        assertEquals(0f, InfuserVisuals.burstRadius(0f))
        assertTrue(InfuserVisuals.burstRadius(2f) > 5f)
        assertTrue(InfuserVisuals.burstRadius(2f) - InfuserVisuals.burstRadius(0f) >
            (InfuserVisuals.burstRadius(14f) - InfuserVisuals.burstRadius(12f)) * 20)
        assertEquals(9f, InfuserVisuals.burstRadius(30f))
    }

    @Test fun `joined ribbon edges stay finite and retain their width through bends and camera alignment`() {
        val paths = listOf(
            (0..40).map { Vec3(sin(it * 0.2), cos(it * 0.2), 5.0 + it * 0.1) },
            listOf(Vec3.ZERO, Vec3.ZERO, Vec3(0.0, 0.0, 2.0), Vec3(0.0, 0.0, 3.0)),
            listOf(Vec3(0.0, 0.0, 5.0), Vec3(1.0, 0.0, 5.0), Vec3(1.0, 1.0, 5.0)))
        for (points in paths) {
            val edges = InfuserRibbon.edges(points, List(points.size) { 0.1f })
            for (i in points.indices) {
                assertEquals(0.2, edges[i].left.distanceTo(edges[i].right), 0.00001)
                assertTrue(edges[i].left.add(edges[i].right).scale(0.5).distanceTo(points[i]) < 0.00001)
            }
        }
    }
}
