package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class GravitronFreezePacket(val shipId: Long, val anchor: Vec3, val frozen: Boolean, val age: Float) : S2CCWPacket {
    override var player: Player? = null

    constructor(buffer: FriendlyByteBuf) : this(buffer.readLong(),
        Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readBoolean(), buffer.readFloat())

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeLong(shipId)
        buffer.writeDouble(anchor.x)
        buffer.writeDouble(anchor.y)
        buffer.writeDouble(anchor.z)
        buffer.writeBoolean(frozen)
        buffer.writeFloat(age)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { GravitronEffects.acceptFreeze(this) }
        context.setPacketHandled(true)
    }
}
