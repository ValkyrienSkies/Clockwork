package org.valkyrienskies.clockwork.platform.api.network

import net.minecraft.server.level.ServerPlayer

interface ServerNetworkContext : NetworkContext {
    val sender: ServerPlayer
}