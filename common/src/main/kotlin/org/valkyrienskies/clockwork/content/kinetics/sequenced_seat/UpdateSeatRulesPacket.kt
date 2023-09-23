package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class UpdateSeatRulesPacket : C2SCWPacket {
    private val pos: BlockPos
    private val rulesForward: SequencedSeatRuleList
    private val rulesBackward: SequencedSeatRuleList
    private val rulesLeft: SequencedSeatRuleList
    private val rulesRight: SequencedSeatRuleList

    constructor(buffer: FriendlyByteBuf) {
        pos = buffer.readBlockPos()
        rulesForward = SequencedSeatRuleList()
        rulesBackward = SequencedSeatRuleList()
        rulesLeft = SequencedSeatRuleList()
        rulesRight = SequencedSeatRuleList()
        val nbt = buffer.readNbt()
        rulesForward.deserializeNBT(nbt!!.getList("rulesForward", CompoundTag.TAG_COMPOUND.toInt()))
        rulesBackward.deserializeNBT(nbt.getList("rulesBackward", CompoundTag.TAG_COMPOUND.toInt()))
        rulesLeft.deserializeNBT(nbt.getList("rulesLeft", CompoundTag.TAG_COMPOUND.toInt()))
        rulesRight.deserializeNBT(nbt.getList("rulesRight", CompoundTag.TAG_COMPOUND.toInt()))
    }

    constructor(be: SequencedSeatBlockEntity) {
        pos = be.blockPos
        rulesForward = be.forwardRules
        rulesBackward = be.backwardRules
        rulesLeft = be.leftRules
        rulesRight = be.rightRules
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val be = context.sender.level.getBlockEntity(pos) as SequencedSeatBlockEntity?
            if (be != null && be.canPlayerUse(context.sender)) {
                be.updateRules(rulesForward, rulesBackward, rulesLeft, rulesRight)
            }
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        val nbt = CompoundTag()
        nbt.put("rulesForward", rulesForward.serializeNBT())
        nbt.put("rulesBackward", rulesBackward.serializeNBT())
        nbt.put("rulesLeft", rulesLeft.serializeNBT())
        nbt.put("rulesRight", rulesRight.serializeNBT())
        buffer.writeNbt(nbt)
    }
}