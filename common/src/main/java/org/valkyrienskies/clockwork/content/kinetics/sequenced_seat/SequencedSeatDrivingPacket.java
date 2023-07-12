package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket;
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext;

import java.util.Set;

public class SequencedSeatDrivingPacket implements C2SCWPacket {
    private final int seatId;
    private final Set<InputKey> pressedKeys;

    public SequencedSeatDrivingPacket(int seatId, Set<InputKey> pressedKeys) {
        this.seatId = seatId;
        this.pressedKeys = pressedKeys;
    }

    public SequencedSeatDrivingPacket(FriendlyByteBuf buffer) {
        seatId = buffer.readInt();
        pressedKeys = InputKey.fromInt(buffer.readInt());
    }

    @Override
    public void handle(ServerNetworkContext context) {
        context.enqueueWork(() -> {
            Level level = context.getSender().level;
            Entity entity = level.getEntity(seatId);
            if (entity instanceof SequencedSeatEntity seat &&
                    context.getSender().equals(seat.getFirstPassenger())) {

                BlockEntity be = level.getBlockEntity(seat.blockPosition());
                if (be instanceof SequencedSeatBlockEntity seatBlock) {
                    seatBlock.updateInput(pressedKeys);
                }
            }
        });
        context.setPacketHandled(true);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(seatId);
        buffer.writeInt(InputKey.asInt(pressedKeys));
    }
}
