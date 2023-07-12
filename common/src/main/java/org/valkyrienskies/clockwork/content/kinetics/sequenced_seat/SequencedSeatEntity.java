package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.valkyrienskies.clockwork.ClockWorkEntities;
import org.valkyrienskies.clockwork.ClockWorkPackets;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SequencedSeatEntity extends SeatEntity {

    private Set<InputKey> prevKeys = Set.of();

    public SequencedSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static SequencedSeatEntity create(Level level, BlockPos pos) {
        return ClockWorkEntities.SEQUENCED_SEAT.create(level);
    }

    @Override
    public void tick() {
        if (level.isClientSide) {
            if (this.getFirstPassenger() instanceof LocalPlayer) {
                checkKeybinds();
            }
        } else {
            boolean blockPresent = level.getBlockState(blockPosition())
                    .getBlock() instanceof SequencedSeatBlock;
            if (isVehicle() && blockPresent)
                return;
            this.discard();
            BlockEntity be = this.level.getBlockEntity(this.blockPosition());
            if (be instanceof SequencedSeatBlockEntity seatBlock) {
                seatBlock.updateInput(Collections.emptySet());
            }
        }
    }

    private void checkKeybinds() {
        Input input = Objects.requireNonNull(Minecraft.getInstance().player).input;

        Set<InputKey> keys = new HashSet<>();

        if (input.left) {
            keys.add(InputKey.LEFT);
        }

        if (input.right) {
            keys.add(InputKey.RIGHT);
        }

        if (input.up) {
            keys.add(InputKey.FORWARD);
        }

        if (input.down) {
            keys.add(InputKey.BACKWARD);
        }

        if (input.jumping) {
            keys.add(InputKey.JUMP);
        }

        if (!keys.equals(prevKeys)) {
            sendUpdate(keys);
        }

        prevKeys = keys;
    }

    private void sendUpdate(Set<InputKey> keys) {
        ClockWorkPackets.sendToServer(new SequencedSeatDrivingPacket(this.getId(), keys));
    }
}
