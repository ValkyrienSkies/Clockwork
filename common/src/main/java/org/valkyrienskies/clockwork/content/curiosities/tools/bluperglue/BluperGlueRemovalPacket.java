package org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue;

import java.util.function.Supplier;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public class BluperGlueRemovalPacket extends SimplePacketBase {

    private int entityId;
    private BlockPos soundSource;

    public BluperGlueRemovalPacket(int id, BlockPos soundSource) {
        entityId = id;
        this.soundSource = soundSource;
    }

    public BluperGlueRemovalPacket(FriendlyByteBuf buffer) {
        entityId = buffer.readInt();
        soundSource = buffer.readBlockPos();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeBlockPos(soundSource);
    }

    @Override
    public void handle(Supplier<Context> context) {
        Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            Entity entity = player.level.getEntity(entityId);
            if (!(entity instanceof BluperGlueEntity BluperGlue))
                return;
            double range = 32;
            if (VSGameUtilsKt.squaredDistanceToInclShips(player, BluperGlue.position().x, BluperGlue.position().y, BluperGlue.position().z) > range * range)
                return;
            AllSoundEvents.SLIME_ADDED.play(player.level, null, soundSource, 0.5F, 0.5F);
            BluperGlue.spawnParticles();
            entity.discard();
        });
        ctx.setPacketHandled(true);
    }

}
