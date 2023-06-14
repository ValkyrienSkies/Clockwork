package org.valkyrienskies.clockwork.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.valkyrienskies.clockwork.content.materials.solids.colorblock.ColorBlockEntity;
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext;
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket;

public class ColorBlockEntityPacket implements S2CCWPacket {
    private final BlockPos pos;
    private final int color;

    public ColorBlockEntityPacket(BlockPos pos, int color) {
        this.pos = pos;
        this.color = color;
    }

    public ColorBlockEntityPacket(FriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.color);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.enqueueWork(() -> {
            assert Minecraft.getInstance().level != null;
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
            if (be instanceof ColorBlockEntity color) {
                color.setColor(this.color);
                color.setChanged();
            }
        });
        context.setPacketHandled(true);
    }
}
