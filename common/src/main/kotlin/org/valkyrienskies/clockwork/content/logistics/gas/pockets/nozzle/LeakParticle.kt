package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.particle.SpriteSet
import net.minecraft.client.particle.TextureSheetParticle
import net.minecraft.core.particles.SimpleParticleType
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.KelvinMod

class LeakParticle(
    level: ClientLevel,
    x: Double,
    y: Double,
    z: Double,
    xSpeed: Double,
    ySpeed: Double,
    zSpeed: Double,
    private val spriteSet: SpriteSet
) : TextureSheetParticle(level, x, y, z, xSpeed, ySpeed, zSpeed) {

    init {
        this.gravity = -0.5f
        this.xd = xSpeed
        this.yd = ySpeed
        this.zd = zSpeed
    }

    override fun tick() {
        super.tick()
        try {
            setSpriteFromAge(spriteSet)
        }  catch (e: Exception)  {
            ClockworkMod.LOGGER.error("Error in tick in LeakParticle. Is a gas particle missing assets? Error: $e")
        }

    }

    override fun getRenderType(): ParticleRenderType {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
    }
}
