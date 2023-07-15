package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket;
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext;

public class UpdateSeatRulesPacket implements C2SCWPacket {
    private final BlockPos pos;
    private final SequencedSeatRuleList rulesForward;
    private final SequencedSeatRuleList rulesBackward;
    private final SequencedSeatRuleList rulesLeft;
    private final SequencedSeatRuleList rulesRight;

    public UpdateSeatRulesPacket(FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        rulesForward = new SequencedSeatRuleList();
        rulesBackward = new SequencedSeatRuleList();
        rulesLeft = new SequencedSeatRuleList();
        rulesRight = new SequencedSeatRuleList();

        CompoundTag nbt = buffer.readNbt();

        rulesForward.deserializeNBT(nbt.getList("rulesForward", CompoundTag.TAG_COMPOUND));
        rulesBackward.deserializeNBT(nbt.getList("rulesBackward", CompoundTag.TAG_COMPOUND));
        rulesLeft.deserializeNBT(nbt.getList("rulesLeft", CompoundTag.TAG_COMPOUND));
        rulesRight.deserializeNBT(nbt.getList("rulesRight", CompoundTag.TAG_COMPOUND));
    }

    public UpdateSeatRulesPacket(SequencedSeatBlockEntity be) {
        pos = be.getBlockPos();
        rulesForward = be.getForwardRules();
        rulesBackward = be.getBackwardRules();
        rulesLeft = be.getLeftRules();
        rulesRight = be.getRightRules();
    }

    @Override
    public void handle(ServerNetworkContext context) {
        context.enqueueWork(() -> {
            SequencedSeatBlockEntity be = (SequencedSeatBlockEntity) context.getSender().level.getBlockEntity(pos);
            if (be != null && be.canPlayerUse(context.getSender())) {
                be.updateRules(rulesForward, rulesBackward, rulesLeft, rulesRight);
            }
        });
        context.setPacketHandled(true);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        CompoundTag nbt = new CompoundTag();
        nbt.put("rulesForward", rulesForward.serializeNBT());
        nbt.put("rulesBackward", rulesBackward.serializeNBT());
        nbt.put("rulesLeft", rulesLeft.serializeNBT());
        nbt.put("rulesRight", rulesRight.serializeNBT());
        buffer.writeNbt(nbt);
    }
}
