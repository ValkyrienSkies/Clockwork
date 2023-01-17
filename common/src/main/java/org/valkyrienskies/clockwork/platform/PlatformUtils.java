package org.valkyrienskies.clockwork.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueEntity;

public class PlatformUtils {

    @ExpectPlatform
    public static double getReachDistance(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Packet<?> createExtraDataSpawnPacket(Entity entity) {
        throw new AssertionError();
    }
}
