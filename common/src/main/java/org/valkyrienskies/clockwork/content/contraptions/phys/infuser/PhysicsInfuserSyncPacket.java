package org.valkyrienskies.clockwork.content.contraptions.phys.infuser;

import me.pepperbell.simplenetworking.S2CPacket;
import me.pepperbell.simplenetworking.SimpleChannel;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext;
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket;

public class PhysicsInfuserSyncPacket implements S2CCWPacket {

    private NonNullList<ItemStack> inventoryPacket;
    private final BlockPos pos;

    public PhysicsInfuserSyncPacket(FriendlyByteBuf buf) {
        CompoundTag nbt = buf.readNbt();
        if (nbt != null) {
            inventoryPacket = NonNullList.withSize(1, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(nbt, inventoryPacket);
        } else {
            inventoryPacket = NonNullList.withSize(1, ItemStack.EMPTY);
        }
        pos = buf.readBlockPos();
    }

    public PhysicsInfuserSyncPacket(PhysicsInfuserBlockEntity tile) {
        inventoryPacket = tile.inventory;
        pos = tile.getBlockPos();
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        CompoundTag nbt = new CompoundTag();
        ContainerHelper.saveAllItems(nbt, inventoryPacket);
        buffer.writeNbt(nbt);
    }

    @Override
    public void handle(ClientNetworkContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof PhysicsInfuserBlockEntity) {
                PhysicsInfuserBlockEntity ce = (PhysicsInfuserBlockEntity) Minecraft.getInstance().level.getBlockEntity(pos);
                if (ce != null) {
                    ce.inventory = inventoryPacket;
                }
            }

        });
    }
}
