package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.InteractionHand
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class GravitronInputPacket : C2SCWPacket {
    private val leftClicked: Boolean

    constructor(buffer: FriendlyByteBuf) {
        leftClicked = buffer.readBoolean()
    }

    constructor(leftClicked: Boolean) {
        this.leftClicked = leftClicked
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val player = context.sender
            if (player.getItemInHand(InteractionHand.MAIN_HAND).item == ClockworkItems.GRAVITRON.get()) {
                if (leftClicked) {
                    ClockworkItems.GRAVITRON.get().leftClickItemServer(player)
                }
                // Implement other logic here (scrolling, rotating, WASD)
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBoolean(leftClicked)
    }
}
