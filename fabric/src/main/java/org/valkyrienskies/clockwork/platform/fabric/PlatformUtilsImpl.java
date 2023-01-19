package org.valkyrienskies.clockwork.platform.fabric;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import io.github.fabricators_of_create.porting_lib.entity.ExtraSpawnDataEntity;
import io.github.fabricators_of_create.porting_lib.mixin.common.accessor.ServerGamePacketListenerImplAccessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PlatformUtilsImpl {
    public static double getReachDistance(Player player) {
        return ReachEntityAttributes.getReachDistance(player, player.isCreative() ? 5.0 : 4.5);
    }

    public static Packet<?> createExtraDataSpawnPacket(Entity entity) {
        return ExtraSpawnDataEntity.createExtraDataSpawnPacket(entity);
    }

    public static void setAboveGroundTicks(ServerPlayer player, int ticks) {
        ((ServerGamePacketListenerImplAccessor) player.connection).port_lib$setAboveGroundTickCount(0);
    }
}
