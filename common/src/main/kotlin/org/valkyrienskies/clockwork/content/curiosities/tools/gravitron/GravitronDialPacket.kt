package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class GravitronDialPacket : S2CCWPacket {

    val quote: Float

    constructor(quote: Float) {
        this.quote = quote
    }

    constructor(buffer: FriendlyByteBuf) {
        quote = buffer.readFloat()
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            val player = Minecraft.getInstance().player
            val duckPlayer = player as MixinPlayerDuck

            duckPlayer.prevGravitronDialAngle = duckPlayer.gravitronDialAngle
            duckPlayer.gravitronDialAngle = quote
            duckPlayer.needsRefresh = true
        }
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeFloat(quote)
    }
}