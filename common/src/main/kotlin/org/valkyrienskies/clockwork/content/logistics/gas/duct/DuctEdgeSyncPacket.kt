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
import org.valkyrienskies.kelvin.util.INodeBlockEntity

class DuctEdgeSyncPacket : S2CCWPacket {
    override var player: Player? = null
    val pos: BlockPos
    val direction: Direction
    val type: DuctEdgeType

    constructor(pos: BlockPos, direction: Direction, type: DuctEdgeType) {
        this.pos = pos
        this.direction = direction
        this.type = type
    }

    constructor(buffer: FriendlyByteBuf) {
        this.pos = buffer.readBlockPos()
        this.direction = buffer.readEnum(Direction::class.java)
        this.type = buffer.readEnum(DuctEdgeType::class.java)
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
                val ce = Minecraft.getInstance().level!!.getBlockEntity(pos) as DuctBlockEntity?
                val be = Minecraft.getInstance().level!!.getBlockEntity(pos.relative(direction)) as? INodeBlockEntity ?: return@enqueueWork
                ce?.setEdgeType(direction, be.getDuctNodePosition(),type, true)
            }
        }
        context.setPacketHandled(true)
    }
}