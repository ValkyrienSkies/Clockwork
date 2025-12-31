package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.Direction
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.impl.client.particle.DefaultGasParticle

class LeakParticleProvider(private val sprite: SpriteSet) : ParticleProvider<LeakParticleData> {
    override fun createParticle(
        type: LeakParticleData,
        level: ClientLevel,
        x: Double,
        y: Double,
        z: Double,
        xSpeed: Double,
        ySpeed: Double,
        zSpeed: Double
    ): Particle {
        try {
            val velVec = type.dir.mul(type.speed, Vector3f())
            val randomDirOffset = 0.25f
            val closestDirection = type.dir.getClosestDirection()
            val perpAxis = if (closestDirection == Direction.UP || closestDirection == Direction.DOWN) {
                Direction.EAST.step()
            } else {
                closestDirection.clockWise.step()
            }
            velVec.add(
                perpAxis.x * (level.random.nextFloat() - 0.5f) * randomDirOffset,
                perpAxis.y * (level.random.nextFloat() - 0.5f) * randomDirOffset,
                perpAxis.z * (level.random.nextFloat() - 0.5f) * randomDirOffset
            )
            val particle = LeakParticle(level, x, y, z, velVec.x.toDouble(), velVec.y.toDouble(), velVec.z.toDouble(), sprite)
            particle.setSpriteFromAge(sprite)
            return particle
        } catch (e: Exception)  {
            ClockworkMod.LOGGER.error("Error in createParticle in LeakParticleProvider. Is a gas particle missing assets? Error: $e")
        }
        return LeakParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite)
    }

    private fun Vector3f.getClosestDirection(): Direction {
        val directions = Direction.values()
        var closestDirection = directions[0]
        var maxDot = this.dot(closestDirection.step())
        for (i in 1 until directions.size) {
            val dir = directions[i]
            val dot = this.dot(dir.step())
            if (dot > maxDot) {
                maxDot = dot
                closestDirection = dir
            }
        }
        return closestDirection
    }
}
