package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet

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
    ): Particle? {
        val velVec = type.dir.step().mul(type.speed)
        val randomDirOffset = 0.25f
        val perpAxis=  type.dir.clockWise.step()
        velVec.add(
            perpAxis.x * (level.random.nextFloat() - 0.5f) * randomDirOffset,
            perpAxis.y * (level.random.nextFloat() - 0.5f) * randomDirOffset,
            perpAxis.z * (level.random.nextFloat() - 0.5f) * randomDirOffset
        )
        return LeakParticle(level, x, y, z, velVec.x.toDouble(), velVec.y.toDouble(), velVec.z.toDouble(), sprite)
    }
}
