package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
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

    Tag serializeNBT();

    void deserializeNBT(Tag tag);

    class DistanceValue implements SequencedSeatValue {
        private static final TranslatableComponent KEY = new TranslatableComponent("sequenced_seat.value.distance");
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
            input.withRange(1, Integer.MAX_VALUE);
            input.calling(v -> meters = v);
        }

        @Override
        public Tag serializeNBT() {
            return IntTag.valueOf(meters);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            meters = ((IntTag) tag).getAsInt();
        }
    }

    class AngleValue implements SequencedSeatValue {
        private static final TranslatableComponent KEY = new TranslatableComponent("sequenced_seat.value.angle");
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
            input.withRange(-180, 180);
            input.calling(v -> degrees = v);
        }

        @Override
        public Tag serializeNBT() {
            return IntTag.valueOf(degrees);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            degrees = ((IntTag) tag).getAsInt();
        }
    }

    class MultiplyValue implements SequencedSeatValue {
        private static final TranslatableComponent KEY = new TranslatableComponent("sequenced_seat.value.multiply");
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

        @Override
        public Tag serializeNBT() {
            return FloatTag.valueOf(multiplier);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            multiplier = ((FloatTag) tag).getAsFloat();
        }
    }
}
