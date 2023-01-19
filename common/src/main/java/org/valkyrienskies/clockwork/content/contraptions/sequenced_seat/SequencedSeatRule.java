package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import net.minecraft.nbt.CompoundTag;

import java.util.HashSet;
import java.util.Set;

public record SequencedSeatRule(Set<InputKey> inputKeys, SequencedSeatOperation operation, SequencedSeatValue value) {

    public static SequencedSeatRule empty() {
        return new SequencedSeatRule(new HashSet<>(), SequencedSeatOperation.NOTHING, null);
    }

    public boolean matches(Set<InputKey> inputKeys) {
        return inputKeys.containsAll(this.inputKeys);
    }

    public float calculateModifier(SequencedSeatBlockEntity be) {
        return 1;
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
