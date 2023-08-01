package org.valkyrienskies.clockwork.content.curiosities.tools.auric_designator;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3ic;
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext;
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class AreaDesignatorSelectionPacket implements S2CCWPacket {

    private final Vector3ic firstPos;

    public AreaDesignatorSelectionPacket(FriendlyByteBuf buffer) {
        firstPos = VectorConversionsMCKt.toJOML(buffer.readBlockPos());
    }
    public AreaDesignatorSelectionPacket(AreaDesignatorItem adi) {
        firstPos = adi.firstPos;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(VectorConversionsMCKt.toBlockPos(firstPos));
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
                if (!(Minecraft.getInstance().player.getMainHandItem().getItem() instanceof AreaDesignatorItem)) {
                    context.setPacketHandled(true);
                    return;
                }
                AreaDesignatorItem adi = (AreaDesignatorItem) Minecraft.getInstance().player.getMainHandItem().getItem();
                if (adi != null) {
                    adi.firstPos = firstPos;
                }
            }
        });
        context.setPacketHandled(true);
    }
}
