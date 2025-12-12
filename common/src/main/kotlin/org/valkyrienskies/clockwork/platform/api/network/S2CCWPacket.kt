package org.valkyrienskies.clockwork.platform.api.network

import net.minecraft.world.entity.player.Player

interface S2CCWPacket : CWPacket {
    fun handle(context: ClientNetworkContext)

    var player: Player?
}
