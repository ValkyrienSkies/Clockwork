package org.valkyrienskies.clockwork.content.propulsion.afterblazer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext;
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket;
import org.valkyrienskies.clockwork.util.blocktype.EngineHeatLevel;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

public class AfterblazerStatusPacket implements S2CCWPacket {

    private final BlockPos pos;
    private final int heat;
    private final int power;

    public AfterblazerStatusPacket(FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();

        CompoundTag nbt = buffer.readNbt();

        heat = nbt.getInt("Clockwork$heat");
        power = nbt.getInt("Clockwork$power");
    }

    public AfterblazerStatusPacket(AfterblazerBlockEntity be) {
        pos = be.getBlockPos();
        heat = be.getHeat();
        power = be.getRedstoneLevel();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Clockwork$heat", heat);
        nbt.putInt("Clockwork$power", power);
        buffer.writeNbt(nbt);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof AfterblazerBlockEntity) {
                AfterblazerBlockEntity ae = (AfterblazerBlockEntity) Minecraft.getInstance().level.getBlockEntity(pos);
                if (ae != null) {
                    ae.setHeat(heat);
                    ae.setRedstoneLevel(power);
                }
            }

        });
        context.setPacketHandled(true);
    }
}
