package org.valkyrienskies.clockwork;

import com.simibubi.create.foundation.particle.ICustomParticleData;
import net.createmod.catnip.lang.Lang;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.valkyrienskies.clockwork.content.curiosities.particles.PhysLightningParticle;
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.LeakParticleData;
import org.valkyrienskies.clockwork.platform.PlatformUtils;
import org.valkyrienskies.clockwork.platform.api.DeferredRegister;

import java.util.function.Supplier;

public enum ClockworkParticles {
    PHYS_LIGHTNING(PhysLightningParticle.Data::new),
    LEAK("leak", LeakParticleData::new);
    private final ParticleEntry<?> entry;

    <D extends ParticleOptions> ClockworkParticles(Supplier<? extends ICustomParticleData<D>> typeFactory) {
        String name = Lang.asId(name());
        ClockworkMod.INSTANCE.getLOGGER().info("Registering particle: " + name);
        entry = new ParticleEntry<>(name, typeFactory);
    }

    <D extends ParticleOptions> ClockworkParticles(String name, Supplier<? extends ICustomParticleData<D>> typeFactory) {
        ClockworkMod.INSTANCE.getLOGGER().info("Registering particle: " + name);
        entry = new ParticleEntry<>(name, typeFactory);
    }

    public static void init() {
        for (final ClockworkParticles particle : values()) {
            ParticleEntry.REGISTER.register(particle.entry.name, () -> particle.entry.object);
        }
        ParticleEntry.REGISTER.registerAll();
    }

    @Environment(EnvType.CLIENT)
    public static void initClient(Object event) {
        for (final ClockworkParticles particle : values()) {
            //particle.entry.registerFactory(particles);
            PlatformUtils.registerParticleOnPlatform(particle.entry, event);
        }
    }

    public ParticleType<?> get() {
        return entry.object;
    }

    public String parameter() {
        return entry.name;
    }

    public static class ParticleEntry<D extends ParticleOptions> {
        private static final DeferredRegister<ParticleType<?>> REGISTER =
                DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, ClockworkMod.MOD_ID);

        public final String name;
        public final Supplier<? extends ICustomParticleData<D>> typeFactory;
        public final ParticleType<D> object;

        public ParticleEntry(final String name, final Supplier<? extends ICustomParticleData<D>> typeFactory) {
            this.name = name;
            this.typeFactory = typeFactory;

            object = this.typeFactory.get().createType();
        }
    }
}
