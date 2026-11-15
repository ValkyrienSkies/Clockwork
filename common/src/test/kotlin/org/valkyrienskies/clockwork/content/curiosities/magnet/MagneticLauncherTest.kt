package org.valkyrienskies.clockwork.content.curiosities.magnet

import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sqrt

class MagneticLauncherTest {
    private val stage = listOf(-1.0, 1.0).flatMap {
        MagneticField.poles(Vector3d(0.0, 0.0, it), Vector3d(0.0, 0.0, 1.0), 2.0)
    }

    private fun rodForce(distance: Double, blocks: Int = 4, sources: List<MagneticField.Pole> = stage): Vector3d {
        val centers = List(blocks) { Vector3d(-distance - it, 0.0, 0.0) }
        val moments = MagneticField.CoreSolver().solve(sources, centers)
        val result = Vector3d()
        for (i in centers.indices) {
            val cores = MagneticField.corePoles(centers[i], moments[i])
            for (group in sources.chunked(8)) {
                val center = group.fold(Vector3d()) { value, pole -> value.add(pole.position) }.div(group.size.toDouble())
                val coarse = MagneticField.coarseWeight(centers[i].distanceSquared(center), .4 + sqrt(.285))
                if (coarse < 1.0) for (core in cores) for (pole in group)
                    result.add(MagneticField.pairForce(core, pole).mul(1.0 - coarse))
                if (coarse > 0.0) for (core in cores) for (pole in MagneticField.coarsePoles(group))
                    result.add(MagneticField.pairForce(core, pole).mul(coarse))
            }
        }
        return result
    }

    @Test fun ironRodHasUsefulPullSeveralBlocksBeforeTheStage() {
        val mass = 4 * 7850.0
        assertTrue(rodForce(3.0).x / mass in 1.8..2.2)
        assertTrue(rodForce(4.0).x / mass in 1.2..1.4)
        assertTrue(rodForce(5.0).x / mass in .7..0.9)
        assertEquals(0.0, rodForce(3.0).y, 1e-8)
        assertEquals(0.0, rodForce(3.0).z, 1e-8)
        assertEquals(0.0, rodForce(3.0, sources = emptyList()).length())
    }

    @Test fun closeRangeIronForcesStayWithinThePreviousPeakEnvelope() {
        for ((blocks, previousPeak) in listOf(1 to 148.87, 2 to 73.66, 4 to 31.57, 6 to 21.03)) {
            val peak = (0..60).maxOf { abs(rodForce(it / 40.0, blocks).x) / (blocks * 7850.0) }
            assertTrue(peak <= previousPeak, "$blocks-block rod peak acceleration: $peak")
        }
    }

    @Test fun poweredMagnetContactWithIronStaysWithinThePreviousPeakEnvelope() {
        val sources = MagneticField.poles(Vector3d(), Vector3d(1.0, 0.0, 0.0), 2.0)
        for ((direction, previousPeak) in listOf(Vector3d(1.0, 0.0, 0.0) to 278631.0,
            Vector3d(0.0, 1.0, 0.0) to 428726.0)) {
            val peak = (100..200).maxOf {
                val center = Vector3d(direction).mul(it / 100.0)
                val moment = MagneticField.CoreSolver().solve(sources, listOf(center)).single()
                val force = Vector3d()
                for (core in MagneticField.corePoles(center, moment)) for (pole in sources)
                    force.add(MagneticField.pairForce(core, pole))
                force.length()
            }
            assertTrue(peak <= previousPeak, "Peak contact force along $direction: $peak")
        }
    }

    @Test fun guidedRodClearsFifteenStagesWithGameTickSwitchingAndContactFriction() {
        val mass = 4 * 7850.0
        val frictionAcceleration = .11 * 10.0
        val dt = 1.0 / 60.0
        var position = -4.0
        var velocity = 0.0
        var activeStage = 0
        for (tick in 0 until 60 * 20) {
            if (tick % 3 == 0) {
                while (activeStage < 15 && position >= activeStage * 2.0 - 1.4) activeStage++
            }
            val acceleration = if (activeStage < 15) rodForce(activeStage * 2.0 - position).x / mass else 0.0
            velocity += (acceleration - frictionAcceleration) * dt
            assertTrue(velocity > 0.0, "Rod stalled at $position")
            position += velocity * dt
            if (position > 29.0) break
        }
        assertEquals(15, activeStage)
        assertTrue(position > 29.0, "Rod stopped at $position")
        assertTrue(velocity > 4.0, "Exit speed was $velocity")
    }
}
