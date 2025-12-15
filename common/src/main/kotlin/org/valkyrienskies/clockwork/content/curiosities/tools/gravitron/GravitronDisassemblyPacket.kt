package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron;

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.clockwork.util.ShipDestroyer.unfillShip
import org.valkyrienskies.mod.common.getShipManagingPos

class GravitronDisassemblyPacket : C2SCWPacket {
    var clickedPos: BlockPos? = null

    constructor(buffer: FriendlyByteBuf) {
        clickedPos = buffer.readBlockPos()
    }

    constructor(clickedPos: BlockPos) {
        this.clickedPos = clickedPos
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val serverPlayer = context.sender
            val serverLevel = serverPlayer.serverLevel()

            val ship = serverLevel.getShipManagingPos(clickedPos!!)
            if (ship != null) {
                unfillShip(serverLevel, ship)
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(clickedPos!!)
    }
}
