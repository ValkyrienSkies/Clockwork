package org.valkyrienskies.clockwork.platform.fabric;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import io.github.fabricators_of_create.porting_lib.entity.ExtraSpawnDataEntity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PlatformUtilsImpl {
    public static double getReachDistance(Player player) {
        return ReachEntityAttributes.getReachDistance(player, player.isCreative() ? 5.0 : 4.5);
    }

    public static Packet<?> createExtraDataSpawnPacket(Entity entity) {
        return ExtraSpawnDataEntity.createExtraDataSpawnPacket(entity);
    }
}
