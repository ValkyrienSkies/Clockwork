package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.simibubi.create.AllParticleTypes
import com.simibubi.create.foundation.particle.AirParticle
import com.simibubi.create.foundation.particle.AirParticleData
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.particle.ParticleEngine
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.network.FriendlyByteBuf
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkParticles
import java.util.Locale
import java.util.function.BiFunction
import java.util.function.Function

class LeakParticleData @JvmOverloads constructor(var dir: Vector3f = Vector3f(), var speed: Float = 0f) : ParticleOptions,
    ICustomParticleDataWithSprite<LeakParticleData> {
    override fun getType(): ParticleType<*> {
        return ClockworkParticles.LEAK.get()
    }

    override fun writeToNetwork(buffer: FriendlyByteBuf) {
        buffer.writeVector3f(dir)
        buffer.writeFloat(speed)
    }

    override fun writeToString(): String {
        return String.format(Locale.ROOT, "%s %s %f", ClockworkParticles.LEAK.parameter(), dir, speed)
    }

    override fun getDeserializer(): ParticleOptions.Deserializer<LeakParticleData> {
        return DESERIALIZER
    }

    override fun getCodec(type: ParticleType<LeakParticleData>): Codec<LeakParticleData> {
        return CODEC
    }

    @Environment(EnvType.CLIENT)
    override fun getMetaFactory(): ParticleEngine.SpriteParticleRegistration<LeakParticleData> {
        return ParticleEngine.SpriteParticleRegistration { animatedSprite: SpriteSet -> LeakParticleProvider(animatedSprite) }
    }

    companion object {
        val CODEC: Codec<LeakParticleData> =
            RecordCodecBuilder.create<LeakParticleData>(Function { i: RecordCodecBuilder.Instance<LeakParticleData> ->
                i!!.group<Float, Float, Float, Float>(
                    Codec.FLOAT.fieldOf("dirX").forGetter<LeakParticleData>(Function { p: LeakParticleData -> p.dir.x }),
                    Codec.FLOAT.fieldOf("dirY").forGetter<LeakParticleData>(Function { p: LeakParticleData -> p.dir.y }),
                    Codec.FLOAT.fieldOf("dirZ").forGetter<LeakParticleData>(Function { p: LeakParticleData -> p.dir.z }),
                    Codec.FLOAT.fieldOf("speed")
                        .forGetter<LeakParticleData>(Function { p: LeakParticleData -> p.speed })
                )
                    .apply<LeakParticleData>(
                        i,
                        { dirX: Float, dirY: Float, dirZ: Float, speed: Float -> LeakParticleData(Vector3f(dirX, dirY, dirZ), speed) })
            })
        val DESERIALIZER: ParticleOptions.Deserializer<LeakParticleData> =
            object : ParticleOptions.Deserializer<LeakParticleData> {
                @Throws(CommandSyntaxException::class)
                override fun fromCommand(
                    particleTypeIn: ParticleType<LeakParticleData>,
                    reader: StringReader
                ): LeakParticleData {
                    reader.expect(' ')
                    val dirX = reader.readFloat()
                    reader.expect(' ')
                    val dirY = reader.readFloat()
                    reader.expect(' ')
                    val dirZ = reader.readFloat()
                    reader.expect(' ')
                    val speed = reader.readFloat()
                    return LeakParticleData(Vector3f(dirX, dirY, dirZ), speed)
                }

                override fun fromNetwork(
                    particleTypeIn: ParticleType<LeakParticleData>,
                    buffer: FriendlyByteBuf
                ): LeakParticleData {
                    return LeakParticleData(buffer.readVector3f(), buffer.readFloat())
                }
            }
    }
}
