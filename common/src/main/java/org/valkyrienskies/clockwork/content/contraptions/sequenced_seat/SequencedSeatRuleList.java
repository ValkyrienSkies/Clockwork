package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import javax.annotation.Nonnull;

public class SequencedSeatRuleList {
    public static final int MAX_RULES = 5;
    private final SequencedSeatRule[] rules = new SequencedSeatRule[MAX_RULES];

    public SequencedSeatRuleList() {
        for (int i = 0; i < MAX_RULES; i++) {
            rules[i] = SequencedSeatRule.empty();
        }
    }

    public float currentModifier(SequencedSeatBlockEntity be) {
        for (SequencedSeatRule rule : rules) {
            if (rule == null) continue;

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
        setRule(index, new SequencedSeatRule(rule.inputKeys(), operation, rule.value()));
    }

    public void setValue(int index, SequencedSeatValue value) {
        SequencedSeatRule rule = getRule(index);
        setRule(index, new SequencedSeatRule(rule.inputKeys(), rule.operation(), value));
    }
}
