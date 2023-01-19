package org.valkyrienskies.clockwork.platform.forge;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;

public class PlatformUtilsImpl {
    public static double getReachDistance(Player player) {
        return player.getReachDistance();
    }

    public static Packet<?> createExtraDataSpawnPacket(Entity entity) {
        return NetworkHooks.getEntitySpawningPacket(entity);
    }
    public static void setAboveGroundTicks(ServerPlayer player, int ticks) {
        // todo
        return;
    }
}
