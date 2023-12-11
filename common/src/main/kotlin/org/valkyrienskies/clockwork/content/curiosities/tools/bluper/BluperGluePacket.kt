package org.valkyrienskies.clockwork.content.curiosities.tools.bluper

import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.AreaData
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML
import java.util.*

class BluperGluePacket : S2CCWPacket {
    private val firstPos: Optional<Vector3ic>

    constructor(buffer: FriendlyByteBuf) {
        firstPos = Optional.of(buffer.readBlockPos().toJOML())
    }

    constructor(vec: Optional<Vector3ic>) {
        firstPos = vec
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(firstPos.get().toBlockPos())
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
                if (Minecraft.getInstance().player!!.mainHandItem.item !is BluperGlueItem) {
                    context.setPacketHandled(true)
                    return@enqueueWork
                }
                val areaData = AreaData.of(Minecraft.getInstance().player).get()
                areaData.firstPos = (firstPos);
            }
        }
        context.setPacketHandled(true)
    }
}