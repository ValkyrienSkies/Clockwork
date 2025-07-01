package org.valkyrienskies.clockwork.util

import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft

class KNodeSyncPacket: S2CCWPacket {
    val pos: DuctNodePos
    val nodeInfo: CompoundTag

    constructor(pos: DuctNodePos, nodeInfo: CompoundTag) {
        this.pos = pos
        this.nodeInfo = nodeInfo
    }
    constructor(buffer: FriendlyByteBuf) {
        this.pos = DuctNodePos(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readResourceLocation())
        this.nodeInfo = buffer.readNbt() ?: CompoundTag()
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level!!.getBlockEntity(
                    pos.toMinecraft()
                ) is IHeatableBlockEntity
            ) {
                val ce =
                    Minecraft.getInstance().level!!.getBlockEntity(pos.toMinecraft()) as IHeatableBlockEntity?
                ce?.loadData(nodeInfo, pos)
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeDouble(pos.x)
        buffer.writeDouble(pos.y)
        buffer.writeDouble(pos.z)
        buffer.writeResourceLocation(pos.dimensionId)
        buffer.writeNbt(nodeInfo)
    }
}