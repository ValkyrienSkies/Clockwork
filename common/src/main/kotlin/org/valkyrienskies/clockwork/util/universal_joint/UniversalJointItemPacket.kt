package org.valkyrienskies.clockwork.util.universal_joint

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatBlockEntity
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatRuleList
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext
import org.valkyrienskies.clockwork.util.ClockworkConstants

class UniversalJointItemPacket: C2SCWPacket { // I hate that this exists
    private val pos: BlockPos
    private val connectTo: BlockPos

    constructor(buffer: FriendlyByteBuf) {
        pos = buffer.readBlockPos()
        connectTo = buffer.readBlockPos()
    }

    constructor(pos: BlockPos, connectTo: BlockPos) {
        this.pos = pos
        this.connectTo = connectTo

    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val uj = context.sender.level().getBlockEntity(pos) as? IUniversalJoint ?: return@enqueueWork
            uj.tryConnect(context.sender.level(),connectTo)
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        buffer.writeBlockPos(connectTo)

    }
}