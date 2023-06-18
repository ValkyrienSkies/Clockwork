package org.valkyrienskies.clockwork.content.physicalities.motion.wing;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.valkyrienskies.clockwork.content.materials.solids.colorblock.ColorBlockEntity;
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext;
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket;

public class BlockEntityColorPacket implements S2CCWPacket {

    private final BlockPos pos;

    private final int color;

    public BlockEntityColorPacket(FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();

        CompoundTag nbt = buffer.readNbt();

        color = nbt.getInt("Clockwork$color");
    }

    public BlockEntityColorPacket(ColorBlockEntity ce) {
        pos = ce.getBlockPos();
        color = ce.getColor();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Clockwork$color", color);
        buffer.writeNbt(nbt);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof ColorBlockEntity) {
                ColorBlockEntity ce = (ColorBlockEntity) Minecraft.getInstance().level.getBlockEntity(pos);
                if (ce != null) {
                    ce.setColor(color);
                }
            }

        });
        context.setPacketHandled(true);
    }
}
