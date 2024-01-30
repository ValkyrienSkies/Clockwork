package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand


import net.minecraft.client.Minecraft
import net.minecraft.network.FriendlyByteBuf
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOML


class WanderWandSelectionPacket : S2CCWPacket {
    private val firstPos: Vector3ic?

    constructor(buffer: FriendlyByteBuf) {
        firstPos = buffer.readBlockPos().toJOML()
    }

    constructor(adi: WanderWandItem) {
        firstPos = adi.firstPos
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(firstPos!!.toBlockPos())
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
                if (Minecraft.getInstance().player!!.mainHandItem
                        .item !is WanderWandItem
                ) {
                    context.setPacketHandled(true)
                    return@enqueueWork
                }
                val adi = Minecraft.getInstance().player!!.mainHandItem.item as WanderWandItem
                adi.firstPos = firstPos
            }
        }
        context.setPacketHandled(true)
    }
}