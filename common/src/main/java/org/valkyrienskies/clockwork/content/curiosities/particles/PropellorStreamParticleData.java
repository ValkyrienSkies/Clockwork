package org.valkyrienskies.clockwork.content.curiosities.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.contraptions.particle.ICustomParticleDataWithSprite;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.valkyrienskies.clockwork.ClockWorkParticles;

public class PropellorStreamParticleData implements ParticleOptions, ICustomParticleDataWithSprite<PropellorStreamParticleData> {

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
    public ParticleType<?> getType() {
        return ClockWorkParticles.PROP_STREAM.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeInt(posX);
        buffer.writeInt(posY);
        buffer.writeInt(posZ);
    }

    @Override
    public String writeToString() {
        return String.format("%s %d %d %d", ClockWorkParticles.PROP_STREAM.parameter(), posX, posY, posZ);
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
