package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class SlickerAttachmentSyncPacket : S2CCWPacket {
    override var player: Player? = null
    val pos: BlockPos
    val stuck: Boolean

    constructor(be: SlickerBlockEntity) {
        pos = be.blockPos
        stuck = be.shipStuck
    }

    constructor(buffer: FriendlyByteBuf) {
        pos = buffer.readBlockPos()
        stuck = buffer.readBoolean()
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level!!.getBlockEntity(
                    pos
                ) is SlickerBlockEntity
            ) {
                val ce =
                    Minecraft.getInstance().level!!.getBlockEntity(pos) as SlickerBlockEntity?
                if (ce != null) {
                    ce.shipStuck = stuck
                }
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        buffer.writeBoolean(stuck)
    }

}