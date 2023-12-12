package org.valkyrienskies.clockwork.content.curiosities.tools.bluper

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.clockwork.util.AreaData
import java.util.*

class BluperGluePacket : S2CCWPacket {
    private val firstPos: Optional<BlockPos>

    constructor(buffer: FriendlyByteBuf) {
        firstPos = Optional.of(buffer.readBlockPos())
    }

    constructor(vec: Optional<BlockPos>) {
        firstPos = vec
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(firstPos.get())
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
                if (Minecraft.getInstance().player!!.mainHandItem.item !is BluperGlueItem) {
                    context.setPacketHandled(true)
                    return@enqueueWork
                }
                val areaData = AreaData.of(Minecraft.getInstance().player).get()
                areaData.setFirstPos(firstPos);
            }
        }
        context.setPacketHandled(true)
    }
}