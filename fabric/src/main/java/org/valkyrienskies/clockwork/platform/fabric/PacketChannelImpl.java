package org.valkyrienskies.clockwork.platform.fabric;

import com.tterrag.registrate.fabric.EnvExecutor;
import io.netty.buffer.PooledByteBufAllocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.platform.api.network.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class PacketChannelImpl implements PacketChannel {
    private final Map<Class<? extends C2SCWPacket>, Integer> c2sIdMap = new HashMap<>();
    private final Map<Class<? extends S2CCWPacket>, Integer> s2cIdMap = new HashMap<>();
    private final Int2ObjectMap<Function<FriendlyByteBuf, ? extends C2SCWPacket>> c2sDecoderMap = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Function<FriendlyByteBuf, ? extends S2CCWPacket>> s2cDecoderMap = new Int2ObjectOpenHashMap<>();

    private final PooledByteBufAllocator bufAllocator = new PooledByteBufAllocator(true);
    private int idCounter = 0;

    public PacketChannelImpl() {

        if (!ServerPlayNetworking.registerGlobalReceiver(ClockWorkMod.NETWORK_CHANNEL,
                (server, player, handler, buf, responseSender) -> {
                    int id = buf.readVarInt();

                    Function<FriendlyByteBuf, ? extends C2SCWPacket> decoder = c2sDecoderMap.get(id);
                    if (decoder == null) {
                        throw new RuntimeException("Unknown packet id: " + id);
                    }

                    C2SCWPacket packet = decoder.apply(buf);
                    packet.handle(serverContext(server, player));
                })) throw new RuntimeException("Failed to register server packet handler");

        EnvExecutor.runWhenOn(EnvType.SERVER, () -> () -> {
            if (!ClientPlayNetworking.registerGlobalReceiver(ClockWorkMod.NETWORK_CHANNEL,
                    (client, handler, buf, responseSender) -> {
                        int id = buf.readVarInt();

                        Function<FriendlyByteBuf, ? extends S2CCWPacket> decoder = s2cDecoderMap.get(id);
                        if (decoder == null) {
                            throw new RuntimeException("Unknown packet id: " + id);
                        }

                        S2CCWPacket packet = decoder.apply(buf);
                        packet.handle(clientContext(client));
                    })) throw new RuntimeException("Failed to register client packet handler");
        });
    }

    @Override
    public <T extends CWPacket> void registerPacket(Class<T> clazz, Function<FriendlyByteBuf, T> decode) {
        if (C2SCWPacket.class.isAssignableFrom(clazz)) {
            c2sIdMap.put((Class) clazz, idCounter);
            c2sDecoderMap.put(idCounter, (Function) decode);
            idCounter++;
        } else if (S2CCWPacket.class.isAssignableFrom(clazz)) {
            s2cIdMap.put((Class) clazz, idCounter);
            s2cDecoderMap.put(idCounter, (Function) decode);
            idCounter++;
        } else {
            throw new RuntimeException();
        }
    }

    private ClientNetworkContext clientContext(Executor executor) {
        return new ClientNetworkContext() {
            @Override
            public void enqueueWork(Runnable runnable) {
                executor.execute(runnable);
            }

            @Override
            public void setPacketHandled(boolean value) {

            }
        };
    }

    private ServerNetworkContext serverContext(Executor executor, ServerPlayer player) {
        return new ServerNetworkContext() {
            @Override
            public ServerPlayer getSender() {
                return player;
            }

            @Override
            public void enqueueWork(Runnable runnable) {
                executor.execute(runnable);
            }

            @Override
            public void setPacketHandled(boolean value) {

            }
        };
    }

    @Override
    public void sendToNear(Level level, BlockPos pos, int range, S2CCWPacket message) {
        PlayerLookup.around((ServerLevel) level, pos, range).forEach(player -> sendTo(player, message));
    }

    @Override
    public void sendToServer(C2SCWPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(bufAllocator.buffer());
        buf.writeVarInt(c2sIdMap.get(packet.getClass()));
        packet.write(buf);
        ClientPlayNetworking.send(ClockWorkMod.NETWORK_CHANNEL, buf);
    }

    @Override
    public void sendToClientsTracking(S2CCWPacket packet, Entity entity) {
        PlayerLookup.tracking(entity).forEach(player -> sendTo(player, packet));
    }

    @Override
    public void sendToClientsTrackingAndSelf(S2CCWPacket packet, ServerPlayer player) {
        PlayerLookup.tracking(player).forEach(p -> sendTo(p, packet));
        sendTo(player, packet);
    }

    private void sendTo(ServerPlayer player, S2CCWPacket packet) {
        FriendlyByteBuf buf = new FriendlyByteBuf(bufAllocator.buffer());
        buf.writeVarInt(s2cIdMap.get(packet.getClass()));
        packet.write(buf);
        ServerPlayNetworking.send(player, ClockWorkMod.NETWORK_CHANNEL, buf);
    }
}
