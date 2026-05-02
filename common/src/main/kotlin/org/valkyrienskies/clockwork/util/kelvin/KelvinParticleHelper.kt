package org.valkyrienskies.clockwork.util.kelvin

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

object KelvinParticleHelper {
    private const val MAX_PARTICLES_PER_CALL = 32
    private const val DEFAULT_DISK_RADIUS = 0.3

    private val AXIS_X: Vector3dc = Vector3d(1.0, 0.0, 0.0)
    private val AXIS_Y: Vector3dc = Vector3d(0.0, 1.0, 0.0)
    private val AXIS_Z: Vector3dc = Vector3d(0.0, 0.0, 1.0)

    fun spawnParticleWithRatio(level: ClientLevel, ductNodePos: DuctNodePos, pos: Vector3dc, speed: Vector3dc) {

        val network = ClockworkModClient.getKelvin()
        val gasMasses = network.getGasMassAt(ductNodePos)

        if (gasMasses.isEmpty()) return

        val sum = gasMasses.values.sum()
        var cumulative = 0.0
        var particleGas = gasMasses.keys.first()
        for ((gas, value) in gasMasses) {
            cumulative += value
            if (level.random.nextDouble() <= cumulative/sum) {
                particleGas = gas
                break
            }
        }

        network.createGasParticle(level, particleGas, ductNodePos, pos.x(), pos.y(), pos.z(), speed.x(), speed.y(), speed.z())
    }

    fun spawnJetWithRatio(
        level: ClientLevel,
        ductNodePos: DuctNodePos,
        blockCenter: Vector3dc,
        outward: Direction,
        speedMagnitude: Double,
        particleCount: Double,
        radius: Double = DEFAULT_DISK_RADIUS,
        outwardOffset: Double = 0.5
    ) {
        val n = stochasticCount(particleCount, level.random)
        if (n <= 0) return
        val pos = Vector3d()
        val vel = Vector3d()
        repeat(n) {
            sampleJet(blockCenter, outward, speedMagnitude, radius, outwardOffset, level.random, pos, vel)
            spawnParticleWithRatio(level, ductNodePos, pos, vel)
        }
    }

    fun spawnJetForGas(
        level: ClientLevel,
        ductNodePos: DuctNodePos,
        gas: GasType,
        blockCenter: Vector3dc,
        outward: Direction,
        speedMagnitude: Double,
        particleCount: Double,
        radius: Double = DEFAULT_DISK_RADIUS,
        outwardOffset: Double = 0.5
    ) {
        val n = stochasticCount(particleCount, level.random)
        if (n <= 0) return
        val network = ClockworkModClient.getKelvin()
        val pos = Vector3d()
        val vel = Vector3d()
        repeat(n) {
            sampleJet(blockCenter, outward, speedMagnitude, radius, outwardOffset, level.random, pos, vel)
            network.createGasParticle(level, gas, ductNodePos, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z)
        }
    }

    // Stochastic rounding so fractional flows still spawn occasionally instead of being
    // floored to zero, plus a hard cap so big emitters don't dump hundreds of particles per tick.
    private fun stochasticCount(count: Double, random: RandomSource): Int {
        if (count <= 0) return 0
        val whole = floor(count).toInt()
        val frac = count - whole
        val extra = if (random.nextDouble() < frac) 1 else 0
        return minOf(whole + extra, MAX_PARTICLES_PER_CALL)
    }

    private fun sampleJet(
        blockCenter: Vector3dc,
        outward: Direction,
        speedMagnitude: Double,
        radius: Double,
        outwardOffset: Double,
        random: RandomSource,
        outPos: Vector3d,
        outVel: Vector3d
    ) {
        val (axis1, axis2) = perpendicularAxes(outward)
        // r = R * sqrt(u) gives uniform area distribution on the disk; uniform u alone clusters at center.
        val r = radius * sqrt(random.nextDouble())
        val theta = random.nextDouble() * 2.0 * PI
        val u = r * cos(theta)
        val v = r * sin(theta)
        val nx = outward.normal.x.toDouble()
        val ny = outward.normal.y.toDouble()
        val nz = outward.normal.z.toDouble()
        outPos.set(blockCenter)
            .add(nx * outwardOffset, ny * outwardOffset, nz * outwardOffset)
            .add(axis1.x() * u, axis1.y() * u, axis1.z() * u)
            .add(axis2.x() * v, axis2.y() * v, axis2.z() * v)
        outVel.set(nx * speedMagnitude, ny * speedMagnitude, nz * speedMagnitude)
    }

    private fun perpendicularAxes(facing: Direction): Pair<Vector3dc, Vector3dc> {
        return when (facing.axis) {
            Direction.Axis.Y -> AXIS_X to AXIS_Z
            Direction.Axis.X -> AXIS_Y to AXIS_Z
            Direction.Axis.Z -> AXIS_X to AXIS_Y
        }
    }
}
