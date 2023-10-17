package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.InteractionHand
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class GravitronInputPacket : C2SCWPacket {
    private val leftClicked: Boolean
    private val scrollDelta: Double?

    constructor(buffer: FriendlyByteBuf) {
        leftClicked = buffer.readBoolean()
        scrollDelta = if (buffer.readBoolean()) {
            buffer.readDouble()
        } else {
            null
        }
    }

    constructor(leftClicked: Boolean) {
        this.leftClicked = leftClicked
        this.scrollDelta = null
    }

    constructor(scroll: Double) {
        this.leftClicked = false
        this.scrollDelta = scroll
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val player = context.sender
            if (player.getItemInHand(InteractionHand.MAIN_HAND).item == ClockworkItems.GRAVITRON.get()) {
                if (leftClicked) {
                    ClockworkItems.GRAVITRON.get().leftClickItemServer(player)
                } else if (scrollDelta != null) {
                    ClockworkItems.GRAVITRON.get().mouseScrollServer(player, scrollDelta)
                }
                // Implement other logic here (rotating, WASD)
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBoolean(leftClicked)
        buffer.writeBoolean(scrollDelta != null)
        if (scrollDelta != null) buffer.writeDouble(scrollDelta)
    }
}
