package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import net.minecraft.core.Direction;
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

    public float currentModifier(SequencedSeatBlockEntity be, Direction face) {
        for (SequencedSeatRule rule : rules) {
            // loop over every possible rule and check if it wants to apply a modifier
            // It will just take the first one that does
            float result = rule.calculateModifier(be, face, be.pressedKeys());
            if (result != 0) {
                return result;
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
        getRule(index).getInputKeys().add(key);
    }

    public void removeKey(int index, InputKey key) {
        getRule(index).getInputKeys().remove(key);
    }

    public void setOperation(int index, SequencedSeatOperation operation) {
        SequencedSeatRule rule = getRule(index);
        setRule(index, new SequencedSeatRule(rule.getInputKeys(), operation, operation.defaultValue()));
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
