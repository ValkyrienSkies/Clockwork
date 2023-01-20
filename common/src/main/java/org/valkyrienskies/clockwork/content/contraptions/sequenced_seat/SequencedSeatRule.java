package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import net.minecraft.nbt.CompoundTag;

import java.util.HashSet;
import java.util.Set;

public record SequencedSeatRule(Set<InputKey> inputKeys, SequencedSeatOperation operation, SequencedSeatValue value) {

    public static SequencedSeatRule empty() {
        return new SequencedSeatRule(new HashSet<>(), SequencedSeatOperation.NOTHING, null);
    }

    public boolean matches(Set<InputKey> inputKeys) {
        return inputKeys.equals(this.inputKeys);
    }

    public float calculateModifier(SequencedSeatBlockEntity be) {
        return keepTurning(be) ? switch (operation) {
            case NOTHING -> 0;
            case TURN_ANGLE, TURN_DISTANCE -> 1;
            case MULTIPLY -> ((SequencedSeatValue.MultiplyValue) value).multiplier;
        } : 0;
    }

    private boolean keepTurning(SequencedSeatBlockEntity be) {
        int duration = switch (operation) {
            case TURN_ANGLE -> {
                int target = ((SequencedSeatValue.AngleValue) value).degrees;
                double degreesPerTick = (double) KineticTileEntity.convertToAngular(be.getSpeed());
                int ticks = (int)(target / degreesPerTick);
                double degreesErr = target - degreesPerTick * (double)ticks;
                yield ticks + (degreesPerTick > 2.0 * degreesErr ? 0 : 1);
            }
            case TURN_DISTANCE -> {
                int target = ((SequencedSeatValue.DistanceValue) value).meters;
                double metersPerTick = (double)KineticTileEntity.convertToLinear(be.getSpeed());
                yield  (int)(target / metersPerTick + 1.0);
            }
            default -> Integer.MAX_VALUE;
        };


        return be.getTicksSinceLastUpdate() < duration;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("keys", InputKey.asInt(inputKeys));
        tag.putInt("operation", operation.ordinal());

        if (value != null)
            tag.put("value", value.serializeNBT());


        return tag;
    }

    public static SequencedSeatRule deserializeNBT(CompoundTag tag) {
        Set<InputKey> keys = InputKey.fromInt(tag.getInt("keys"));

        SequencedSeatOperation operation = SequencedSeatOperation.values()[tag.getInt("operation")];
        SequencedSeatValue value = operation.defaultValue();

        if (value != null)
            value.deserializeNBT(tag.get("value"));

        return new SequencedSeatRule(keys, operation, value);
    }
}
