package org.valkyrienskies.clockwork.forge.capability;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext;
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel;
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncPlayerCapabilityDataPacket implements S2CCWPacket {
    public UUID uuid;
    public CompoundTag compoundTag;

    public SyncPlayerCapabilityDataPacket(UUID uuid, CompoundTag compoundTag) {
        this.uuid = uuid;
        this.compoundTag = compoundTag;
    }

    public static SyncPlayerCapabilityDataPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerCapabilityDataPacket(buf.readUUID(), buf.readNbt());
    }

    public static void register(PacketChannel packetChannel) {
        packetChannel.registerPacket(SyncPlayerCapabilityDataPacket.class, SyncPlayerCapabilityDataPacket::decode);
    }

    @Override
    public void write(@NotNull FriendlyByteBuf buffer) {
        buffer.writeNbt(compoundTag);
        buffer.writeUUID(uuid);
    }

    @Override
    public void handle(@NotNull ClientNetworkContext context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().level.getPlayerByUUID(uuid);
            PlayerDataCapability.getCapabilityOptional(player).ifPresent(c -> c.deserializeNBT(compoundTag));
        });
    }
}
