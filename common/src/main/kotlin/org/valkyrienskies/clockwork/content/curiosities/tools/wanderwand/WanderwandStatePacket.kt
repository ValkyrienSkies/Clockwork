package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class WanderwandStatePacket(val data: CompoundTag) : S2CCWPacket {
    override var player: Player? = null
    constructor(buffer: FriendlyByteBuf) : this(buffer.readNbt() ?: CompoundTag())
    override fun write(buffer: FriendlyByteBuf) { buffer.writeNbt(data) }
    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { ClockworkModClient.WANDERWAND_EFFECT_RENDERER.accept(data) }
        context.setPacketHandled(true)
    }
}

/** A discrete wheel notch, never a client-supplied length or velocity. */
class WanderwandReelPacket(val direction: Int) : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(buffer.readByte().toInt())
    override fun write(buffer: FriendlyByteBuf) { buffer.writeByte(direction.coerceIn(-1, 1)) }
    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork { WanderwandServer.reel(context.sender, direction.coerceIn(-1, 1)) }
        context.setPacketHandled(true)
    }
}
