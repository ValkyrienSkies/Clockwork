package org.valkyrienskies.clockwork.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PlatformUtils {

    @ExpectPlatform
    public static double getReachDistance(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Packet<?> createExtraDataSpawnPacket(Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setAboveGroundTicks(ServerPlayer player, int ticks) {
        throw new AssertionError();
    }
}
