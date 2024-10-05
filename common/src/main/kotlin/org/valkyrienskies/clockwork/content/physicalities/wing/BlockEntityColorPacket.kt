package org.valkyrienskies.clockwork.content.physicalities.wing

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.content.generic.ColorBlockEntity
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class BlockEntityColorPacket : S2CCWPacket {
    override var player: Player? = null
    private val pos: BlockPos
    private val color: Int

    constructor(buffer: FriendlyByteBuf) {
        pos = buffer.readBlockPos()
        val nbt = buffer.readNbt()
        color = nbt!!.getInt("Clockwork\$color")
    }

    constructor(ce: ColorBlockEntity) {
        pos = ce.blockPos
        color = ce.getColor()
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        val nbt = CompoundTag()
        nbt.putInt("Clockwork\$color", color)
        buffer.writeNbt(nbt)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level!!.getBlockEntity(
                    pos
                ) is ColorBlockEntity
            ) {
                val ce =
                    Minecraft.getInstance().level!!.getBlockEntity(pos) as ColorBlockEntity?
                ce?.setColor(color)
            }
        }
        context.setPacketHandled(true)
    }
}
