package org.valkyrienskies.clockwork.content.logistics.solid.delivery

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class UpdateFrequencySlotPacket : C2SCWPacket {
    private val stack: ItemStack
    private val pos: BlockPos

    constructor(buffer: FriendlyByteBuf) {
        stack = buffer.readItem()
        pos = buffer.readBlockPos()
    }

    constructor(newStack: ItemStack, newPos: BlockPos) {
        stack = newStack
        pos  = newPos
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {

            val behaviour = BlockEntityBehaviour.get(context.sender.level, pos, FrequencySlotBehaviour.TYPE)
                ?: return@enqueueWork
            println(stack)
            behaviour.setFrequency(stack)
            behaviour.blockEntity.setChanged() // This forces the BE to save, which it doesn't do sometimes??
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeItem(stack)
        buffer.writeBlockPos(pos)
    }


}
