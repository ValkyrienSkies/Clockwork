package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class GatemakyrInputPacket @JvmOverloads constructor(val action: Action, val side: Int, val free: Boolean, val distance: Double, val width: Int = 3, val height: Int = width) : C2SCWPacket {
    enum class Action { CHARGE, RELEASE, CANCEL, SETTINGS, CLEAR }
    constructor(buffer: FriendlyByteBuf) : this(buffer.readEnum(Action::class.java), buffer.readVarInt(), buffer.readBoolean(), buffer.readDouble(), buffer.readVarInt(), buffer.readVarInt())
    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeEnum(action); buffer.writeVarInt(side); buffer.writeBoolean(free); buffer.writeDouble(distance); buffer.writeVarInt(width); buffer.writeVarInt(height)
    }
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork { GatemakyrServer.input(context.sender, this) }
        context.setPacketHandled(true)
    }
}
