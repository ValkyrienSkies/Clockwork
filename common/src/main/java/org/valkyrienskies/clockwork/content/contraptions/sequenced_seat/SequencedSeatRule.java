package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.HashSet;
import java.util.Set;

public class SequencedSeatRule {

    public static SequencedSeatRule empty() {
        return new SequencedSeatRule(new HashSet<>(), SequencedSeatOperation.NOTHING, null);
    }

    public static SequencedSeatRule deserializeNBT(CompoundTag tag) {
        Set<InputKey> keys = InputKey.fromInt(tag.getInt("keys"));

        SequencedSeatOperation operation = SequencedSeatOperation.values()[tag.getInt("operation")];
        SequencedSeatValue value = operation.defaultValue();

        if (value != null)
            value.deserializeNBT(tag.get("value"));

        return new SequencedSeatRule(keys, operation, value);
    }

    private final Set<InputKey> inputKeys;
    private final SequencedSeatOperation operation;
    private final SequencedSeatValue value;

    public SequencedSeatRule(Set<InputKey> inputKeys, SequencedSeatOperation operation, SequencedSeatValue value) {
        this.inputKeys = inputKeys;
        this.operation = operation;
        this.value = value;
    }

    public boolean matches(Set<InputKey> inputKeys) {
        return inputKeys.equals(this.inputKeys);
    }

    public float calculateModifier(SequencedSeatBlockEntity be, Direction face, Set<InputKey> inputKeys) {
        boolean matches = matches(inputKeys);
        return switch (operation) {
            case NOTHING -> 0;
            case TURN_ANGLE -> angleRotate(be, face, matches);
            case TURN_DISTANCE -> distanceRotate(be, face, matches);
            case MULTIPLY -> matches ? ((SequencedSeatValue.MultiplyValue) value).multiplier : 0;
        };
    }

    private boolean inAction = false;
    private float angleRotate(SequencedSeatBlockEntity be, Direction face, boolean matches) {
        if (!inAction && !matches) return 0;

        int targetDegrees = -((SequencedSeatValue.AngleValue) value).degrees;

        float degreesAway = be.getDegreesAwayFromBase(face);
        float diff = matches ? targetDegrees - degreesAway : -degreesAway;

        if (diff > 180) diff -= 360;
        else if (diff < -180) diff += 360;

        if (diff < 0.1 && diff > -0.1) {
            inAction = matches;
            return 0;
        }

        inAction = true;
        double degreesPerTick = KineticTileEntity.convertToAngular(be.getSpeed());
        if (Math.abs(degreesPerTick) > Math.abs(diff)) {
            return (float) (diff / degreesPerTick);
        } else return (diff * degreesPerTick) < 0 ? -1 : 1;
        // If diff and degrees per tick have different signs, we need to reverse the direction
        // So we use * < to check that
    }

    private float distanceRotate(SequencedSeatBlockEntity be, Direction face, boolean matches) {
        /*
        if (!inAction && !matches) return 0;

        int targetDegrees = ((SequencedSeatValue.DistanceValue) value).meters * 512;

        float degreesAway = be.getDegreesAwayFromBase(face);
        float diff = matches ? targetDegrees - degreesAway : -degreesAway;

        if (diff > 180) diff -= 360;
        else if (diff < -180) diff += 360;

        if (diff < 0.1 && diff > -0.1) {
            inAction = matches;
            return 0;
        }

        inAction = true;
        double metersPerTick = KineticTileEntity.convertToLinear(be.getSpeed());
        if (Math.abs(metersPerTick) > Math.abs(diff)) {
            return (float) (diff / metersPerTick);
        } else return (diff * metersPerTick) < 0 ? -1 : 1;
        */
        return 1; //TODO
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("keys", InputKey.asInt(inputKeys));
        tag.putInt("operation", operation.ordinal());

        if (value != null)
            tag.put("value", value.serializeNBT());


        return tag;
    }

    public Set<InputKey> getInputKeys() {
        return inputKeys;
    }

    public SequencedSeatOperation getOperation() {
        return operation;
    }

    public SequencedSeatValue getValue() {
        return value;
    }
}
