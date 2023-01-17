package org.valkyrienskies.clockwork.platform.api.network;

import net.minecraft.server.level.ServerPlayer;

public interface ServerNetworkContext extends NetworkContext {

    ServerPlayer getSender();

}
