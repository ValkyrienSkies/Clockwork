package org.valkyrienskies.clockwork;

import com.simibubi.create.foundation.particle.ICustomParticleData;
import com.simibubi.create.foundation.utility.Lang;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.valkyrienskies.clockwork.content.curiosities.particles.PhysLightningParticle;
import org.valkyrienskies.clockwork.platform.api.DeferredRegister;

import java.util.function.Supplier;

public enum ClockworkParticles {
    PHYS_LIGHTNING(PhysLightningParticle.Data::new);
    private final ParticleEntry<?> entry;

    <D extends ParticleOptions> ClockworkParticles(Supplier<? extends ICustomParticleData<D>> typeFactory) {
        String name = Lang.asId(name());
        entry = new ParticleEntry<>(name, typeFactory);
    }

    public static void init() {
        ParticleEntry.REGISTER.registerAll();
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ParticleEngine particles = Minecraft.getInstance().particleEngine;
        for (final ClockworkParticles particle : values()) {
            particle.entry.registerFactory(particles);
        }
    }

    public ParticleType<?> get() {
        return entry.object;
    }

    public String parameter() {
        return entry.name;
    }

    private static class ParticleEntry<D extends ParticleOptions> {
        private static final DeferredRegister<ParticleType<?>> REGISTER =
                DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, ClockworkMod.MOD_ID);

        private final String name;
        private final Supplier<? extends ICustomParticleData<D>> typeFactory;
        private final ParticleType<D> object;

        public ParticleEntry(final String name, final Supplier<? extends ICustomParticleData<D>> typeFactory) {
            this.name = name;
            this.typeFactory = typeFactory;

            object = this.typeFactory.get().createType();
            REGISTER.register(name, () -> object);
        }

        @Environment(EnvType.CLIENT)
        public void registerFactory(final ParticleEngine particles) {
            typeFactory.get().register(object, particles);
        }
    }
}
