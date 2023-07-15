package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat;

import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public enum SequencedSeatOperation {
    NOTHING("nothing", AllIcons.I_NONE),
    TURN_ANGLE("angle", AllIcons.I_ROTATE_CCW),
    TURN_DISTANCE("distance", AllIcons.I_PRIORITY_HIGH),
    MULTIPLY("multiply", AllIcons.I_PRIORITY_VERY_HIGH);

    private final Component component;
    private final AllIcons icon;


    SequencedSeatOperation(String name, AllIcons icon) {
        this.component = new TranslatableComponent("sequenced_seat.operation." + name);
        this.icon = icon;
    }

    public Component asComponent() {
        return component;
    }

    public AllIcons getIcon() {
        return icon;
    }

    public SequencedSeatValue defaultValue() {
        return switch (this) {
            case NOTHING -> null;
            case TURN_ANGLE -> SequencedSeatValue.angle(90);
            case TURN_DISTANCE -> SequencedSeatValue.distance(1);
            case MULTIPLY -> SequencedSeatValue.multiply(1);
        };
    }
}
