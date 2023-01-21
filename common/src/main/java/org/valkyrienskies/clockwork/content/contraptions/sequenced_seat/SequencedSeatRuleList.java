package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nonnull;

public class SequencedSeatRuleList {
    public static final int MAX_RULES = 5;
    private final SequencedSeatRule[] rules = new SequencedSeatRule[MAX_RULES];

    public SequencedSeatRuleList() {
        for (int i = 0; i < MAX_RULES; i++) {
            rules[i] = SequencedSeatRule.empty();
        }
    }

    public static SequencedSeatRuleList defaultList(Rotation rotation) {
        SequencedSeatRuleList list = new SequencedSeatRuleList();

        list.setOperation(1, SequencedSeatOperation.MULTIPLY);

        switch (rotation) {
            case NONE -> {
                list.addKey(1, InputKey.FORWARD);
            }
            case CLOCKWISE_90 -> {
                list.addKey(1, InputKey.RIGHT);
            }
            case CLOCKWISE_180 -> {
                list.addKey(1, InputKey.BACKWARD);
            }
            case COUNTERCLOCKWISE_90 -> {
                list.addKey(1, InputKey.LEFT);
            }
        }

        return list;
    }

    public float currentModifier(SequencedSeatBlockEntity be) {
        for (SequencedSeatRule rule : rules) {
            if (rule.matches(be.pressedKeys())) {
                return rule.calculateModifier(be);
            }
        }

        return 0;
    }

    @Nonnull
    public SequencedSeatRule getRule(int index) {
        return rules[index];
    }

    public void setRule(int index, SequencedSeatRule rule) {
        rules[index] = rule;
    }

    public void addKey(int index, InputKey key) {
        getRule(index).inputKeys().add(key);
    }

    public void removeKey(int index, InputKey key) {
        getRule(index).inputKeys().remove(key);
    }

    public void setOperation(int index, SequencedSeatOperation operation) {
        SequencedSeatRule rule = getRule(index);
        setRule(index, new SequencedSeatRule(rule.inputKeys(), operation, operation.defaultValue()));
    }

    public ListTag serializeNBT() {
        ListTag list = new ListTag();
        for (SequencedSeatRule rule : rules) {
            list.add(rule.serializeNBT());
        }
        return list;
    }

    public void deserializeNBT(ListTag tag) {
        for (int i = 0; i < MAX_RULES; i++) {
            rules[i] = SequencedSeatRule.deserializeNBT(tag.getCompound(i));
        }
    }
}
