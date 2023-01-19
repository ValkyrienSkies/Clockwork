package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public interface SequencedSeatValue {

    static SequencedSeatValue distance(int meters) {
        return new DistanceValue(meters);
    }

    static SequencedSeatValue angle(int degrees) {
        return new AngleValue(degrees);
    }

    static SequencedSeatValue multiply(float v) {
        return new MultiplyValue(v);
    }

    Component asComponent();

    void configureInput(ScrollInput input);

    class DistanceValue implements SequencedSeatValue {
        public int meters;

        private DistanceValue(int meters) {
            this.meters = meters;
        }

        @Override
        public Component asComponent() {
            return new TextComponent(meters + "m");
        }

        @Override
        public void configureInput(ScrollInput input) {
            input.setState(meters);
            input.titled(KEY);
            input.withRange(0, Integer.MAX_VALUE);
            input.calling(v -> meters = v);
        }

        private static final TranslatableComponent KEY = new TranslatableComponent("sequenced_seat.value.distance");
    }

    class AngleValue implements SequencedSeatValue {
        public int degrees;

        private AngleValue(int degrees) {
            this.degrees = degrees;
        }

        @Override
        public Component asComponent() {
            return new TextComponent(degrees + "°");
        }

        @Override
        public void configureInput(ScrollInput input) {
            input.setState(degrees);
            input.titled(KEY);
            input.withRange(-360, 360);
            input.calling(v -> degrees = v);
        }

        private static final TranslatableComponent KEY = new TranslatableComponent("sequenced_seat.value.angle");
    }

    class MultiplyValue implements SequencedSeatValue {
        public float multiplier;

        private MultiplyValue(float multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public Component asComponent() {
            return new TextComponent(multiplier + "x");
        }

        @Override
        public void configureInput(ScrollInput input) {
            input.setState((int) multiplier * 2);
            input.titled(KEY);
            input.withRange(-8, 8);
            input.calling(v -> multiplier = ((float) v) / 2f);
        }

        private static final TranslatableComponent KEY = new TranslatableComponent("sequenced_seat.value.multiply");
    }
}
