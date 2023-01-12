package org.valkyrienskies.clockwork.fabric.content.curiosities.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.contraptions.particle.ICustomParticleData;
import com.simibubi.create.content.contraptions.particle.ICustomParticleDataWithSprite;
import io.github.tropheusj.dripstone_fluid_lib.ParticleFactories;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.mixin.client.particle.ParticleManagerAccessor;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.particle.ParticleProvider;
import org.valkyrienskies.clockwork.fabric.AllClockworkParticles;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;

public class PropellorStreamParticleData implements ParticleOptions, ICustomParticleDataWithSprite<PropellorStreamParticleData> {

    @Override
    public ParticleType<?> getType() {
        return AllClockworkParticles.PROP_STREAM.get();
    }

    public static final Codec<PropellorStreamParticleData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("x").forGetter(p -> p.posX),
            Codec.INT.fieldOf("y").forGetter(p -> p.posY),
            Codec.INT.fieldOf("z").forGetter(p -> p.posZ)).apply(i, PropellorStreamParticleData::new));

    public static final ParticleOptions.Deserializer<PropellorStreamParticleData> DESERIALIZER = new ParticleOptions.Deserializer<PropellorStreamParticleData>() {
        public PropellorStreamParticleData fromCommand(ParticleType<PropellorStreamParticleData> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int x = reader.readInt();
            reader.expect(' ');
            int y = reader.readInt();
            reader.expect(' ');
            int z = reader.readInt();
            return new PropellorStreamParticleData(x, y, z);
        }

        public PropellorStreamParticleData fromNetwork(ParticleType<PropellorStreamParticleData> particleType, FriendlyByteBuf buffer) {
            return new PropellorStreamParticleData(buffer.readInt(), buffer.readInt(), buffer.readInt());
        }
    };

    final int posX;
    final int posY;
    final int posZ;
    public PropellorStreamParticleData(Vec3i pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }
    public PropellorStreamParticleData(int posX, int posY, int posZ) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }
    public PropellorStreamParticleData() {
        this(0, 0, 0);
    }
    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(posX);
        buffer.writeInt(posY);
        buffer.writeInt(posZ);
    }

    @Override
    public String writeToString() {
        return String.format("%s %d %d %d", AllClockworkParticles.PROP_STREAM.parameter(), posX, posY, posZ);
    }
    @Override
    public Deserializer<PropellorStreamParticleData> getDeserializer() {
        return DESERIALIZER;
    }
    @Override
    public Codec<PropellorStreamParticleData> getCodec(ParticleType<PropellorStreamParticleData> particleType) {
        return CODEC;
    }
    @Override
    @Environment(EnvType.CLIENT)
    public ParticleEngine.SpriteParticleRegistration<PropellorStreamParticleData> getMetaFactory() {
        return PropellorStreamParticle.Factory::new;
    }
}
