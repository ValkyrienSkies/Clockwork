package org.valkyrienskies.clockwork.content.logistics.gas.duct

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class DuctEdgeSyncPacket : S2CCWPacket {
    override var player: Player? = null
    val pos: BlockPos
    val direction: Direction
    val type: ConnectionType

    constructor(pos: BlockPos, direction: Direction, type: ConnectionType) {
        this.pos = pos
        this.direction = direction
        this.type = type
    }

    constructor(buffer: FriendlyByteBuf) {
        this.pos = buffer.readBlockPos()
        this.direction = buffer.readEnum(Direction::class.java)
        this.type = buffer.readEnum(ConnectionType::class.java)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        buffer.writeEnum(direction)
        buffer.writeEnum(type)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level!!.getBlockEntity(
                    pos
                ) is DuctBlockEntity
            ) {
                val ce =
                    Minecraft.getInstance().level!!.getBlockEntity(pos) as DuctBlockEntity?
                ce?.setEdgeType(direction, type, true)
            }
        }
        context.setPacketHandled(true)
    }
}