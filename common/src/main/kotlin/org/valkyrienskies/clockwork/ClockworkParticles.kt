package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.particle.ICustomParticleData
import com.simibubi.create.foundation.utility.Lang
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.ParticleEngine
import net.minecraft.core.Registry
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import org.valkyrienskies.clockwork.content.curiosities.particles.PhysLightningParticle
import org.valkyrienskies.clockwork.platform.api.DeferredRegister
import java.util.function.Supplier

enum class ClockworkParticles(typeFactory: Supplier<out ICustomParticleData<ParticleOptions>>) {

    PHYS_LIGHTNING({ PhysLightningParticle.Data() as ICustomParticleData<ParticleOptions> });

    private val entry: ParticleEntry<*>

    init {
        val name = Lang.asId(name)
        entry = ParticleEntry(name, typeFactory)
    }

    fun get(): ParticleType<*> {
        return entry.`object`
    }

    fun parameter(): String {
        return entry.name
    }

    private class ParticleEntry<D : ParticleOptions>(
        val name: String,
        typeFactory: Supplier<out ICustomParticleData<D>>
    ) {
        private val typeFactory: Supplier<out ICustomParticleData<D>>
        val `object`: ParticleType<D>

        init {
            this.typeFactory = typeFactory
            `object` = this.typeFactory.get().createType()
            REGISTER.register(
                name
            ) { `object` }
        }

        @Environment(EnvType.CLIENT)
        fun registerFactory(particles: ParticleEngine) {
            typeFactory.get().register(`object`, particles)
        }

        companion object {
            val REGISTER: DeferredRegister<ParticleType<*>> =
                DeferredRegister.create(Registry.PARTICLE_TYPE, ClockworkMod.MOD_ID)
        }
    }

    companion object {
        fun init() {
            ParticleEntry.REGISTER.registerAll()
        }

        @Environment(EnvType.CLIENT)
        fun initClient() {
            val particles = Minecraft.getInstance().particleEngine
            for (particle in values()) particle.entry.registerFactory(particles)
        }
    }
}