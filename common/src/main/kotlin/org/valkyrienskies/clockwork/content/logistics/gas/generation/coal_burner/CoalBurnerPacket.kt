package org.valkyrienskies.clockwork.content.logistics.gas.generation.coal_burner

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class CoalBurnerPacket: S2CCWPacket {
    override var player: Player? = null
    override fun handle(context: ClientNetworkContext) {
        TODO("Not yet implemented")
    }

    override fun write(buffer: FriendlyByteBuf) {
        TODO("Not yet implemented")
    }
}