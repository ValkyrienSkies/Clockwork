package org.valkyrienskies.clockwork.content.logistics.heat.usage.gas_nozzle

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class TempGasNozzleSyncPacket : S2CCWPacket {

    private val pos: BlockPos
    private val size: Int

    constructor(buffer: FriendlyByteBuf) {
        pos = buffer.readBlockPos()
        val nbt = buffer.readNbt()
        size = nbt!!.getInt("Clockwork\$pocketSize")
    }

    constructor(ce: GasNozzleBlockEntity) {
        size = ce.pocketSize
        pos = ce.blockPos
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level!!.getBlockEntity(
                    pos
                ) is GasNozzleBlockEntity
            ) {
                val ce =
                    Minecraft.getInstance().level!!.getBlockEntity(pos) as GasNozzleBlockEntity?
                ce?.syncPocketSize(size)
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        val nbt = CompoundTag()
        nbt.putInt("Clockwork\$pocketSize", size)
        buffer.writeNbt(nbt)
    }
}