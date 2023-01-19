package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

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
}
