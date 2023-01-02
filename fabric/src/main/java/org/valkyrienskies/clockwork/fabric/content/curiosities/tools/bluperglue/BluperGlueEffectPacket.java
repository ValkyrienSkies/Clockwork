package org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class BluperGlueEffectPacket extends SimplePacketBase {

    private BlockPos pos;
    private Direction direction;
    private boolean fullBlock;

    public BluperGlueEffectPacket(BlockPos pos, Direction direction, boolean fullBlock) {
        this.pos = pos;
        this.direction = direction;
        this.fullBlock = fullBlock;
    }

    public BluperGlueEffectPacket(FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        direction = Direction.from3DDataValue(buffer.readByte());
        fullBlock = buffer.readBoolean();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeByte(direction.get3DDataValue());
        buffer.writeBoolean(fullBlock);
    }

    @Environment(EnvType.CLIENT)
    public void handle(Supplier<Context> context) {
        context.get().enqueueWork(this::exec);
        context.get().setPacketHandled(true);
    }

    // fabric: lambda funk
    @Environment(EnvType.CLIENT)
    private void exec() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.player.blockPosition().closerThan(pos, 100))
            return;
        BluperGlueItem.spawnParticles(mc.level, pos, direction, fullBlock);
    }

}

