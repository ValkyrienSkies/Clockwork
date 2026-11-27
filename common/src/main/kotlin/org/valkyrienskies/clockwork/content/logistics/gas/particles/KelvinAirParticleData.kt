package org.valkyrienskies.clockwork.content.logistics.gas.particles

import com.mojang.brigadier.StringReader
import com.mojang.serialization.Codec
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.particle.ParticleEngine
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.ClockworkParticles

class KelvinAirParticleData : ParticleOptions, ICustomParticleDataWithSprite<KelvinAirParticleData> {
    override fun getType(): ParticleType<*> {
        return ClockworkParticles.AIR.get()
    }

    override fun writeToNetwork(buffer: FriendlyByteBuf) {
    }

    override fun writeToString(): String {
        return ClockworkParticles.AIR.parameter()
    }

    override fun getDeserializer(): ParticleOptions.Deserializer<KelvinAirParticleData> {
        return DESERIALIZER
    }

    override fun getCodec(type: ParticleType<KelvinAirParticleData>): Codec<KelvinAirParticleData> {
        return CODEC
    }

    @Environment(EnvType.CLIENT)
    override fun getMetaFactory(): ParticleEngine.SpriteParticleRegistration<KelvinAirParticleData> {
        return ParticleEngine.SpriteParticleRegistration { animatedSprite: SpriteSet ->
            KelvinAirParticleProvider(animatedSprite)
        }
    }

    companion object {
        val CODEC: Codec<KelvinAirParticleData> = Codec.unit(::KelvinAirParticleData)

        val DESERIALIZER: ParticleOptions.Deserializer<KelvinAirParticleData> =
            object : ParticleOptions.Deserializer<KelvinAirParticleData> {
                override fun fromCommand(
                    particleTypeIn: ParticleType<KelvinAirParticleData>,
                    reader: StringReader
                ): KelvinAirParticleData {
                    return KelvinAirParticleData()
                }

                override fun fromNetwork(
                    particleTypeIn: ParticleType<KelvinAirParticleData>,
                    buffer: FriendlyByteBuf
                ): KelvinAirParticleData {
                    return KelvinAirParticleData()
                }
            }
    }
}
