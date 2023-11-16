package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import com.simibubi.create.foundation.gui.widget.ScrollInput
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component

interface SequencedSeatValue {
    fun asComponent(): Component
    fun configureInput(input: ScrollInput)
    fun serializeNBT(): Tag
    fun deserializeNBT(tag: Tag)
    class DistanceValue constructor(var meters: Int) : SequencedSeatValue {
        override fun asComponent(): Component {
            return Component.literal(meters.toString() + "m")
        }

        override fun configureInput(input: ScrollInput) {
            input.setState(meters)
            input.titled(KEY)
            input.withRange(1, Int.MAX_VALUE)
            input.calling { v: Int -> meters = v }
        }

        override fun serializeNBT(): Tag {
            return IntTag.valueOf(meters)
        }

        override fun deserializeNBT(tag: Tag) {
            meters = (tag as IntTag).asInt
        }

        companion object {
            private val KEY = Component.translatable("sequenced_seat.value.distance")
        }
    }

    class AngleValue constructor(var degrees: Int) : SequencedSeatValue {
        override fun asComponent(): Component {
            return Component.literal("$degrees°")
        }

        override fun configureInput(input: ScrollInput) {
            input.setState(degrees)
            input.titled(KEY)
            input.withRange(-180, 180)
            input.calling { v: Int -> degrees = v }
        }

        override fun serializeNBT(): Tag {
            return IntTag.valueOf(degrees)
        }

        override fun deserializeNBT(tag: Tag) {
            degrees = (tag as IntTag).asInt
        }

        companion object {
            private val KEY = Component.translatable("sequenced_seat.value.angle")
        }
    }

    class MultiplyValue constructor(var multiplier: Float) : SequencedSeatValue {
        override fun asComponent(): Component {
            return Component.literal(multiplier.toString() + "x")
        }

        override fun configureInput(input: ScrollInput) {
            input.setState(multiplier.toInt() * 2)
            input.titled(KEY)
            input.withRange(-8, 8)
            input.calling { v: Int -> multiplier = v.toFloat() / 2f }
        }

        override fun serializeNBT(): Tag {
            return FloatTag.valueOf(multiplier)
        }

        override fun deserializeNBT(tag: Tag) {
            multiplier = (tag as FloatTag).asFloat
        }

        companion object {
            private val KEY = Component.translatable("sequenced_seat.value.multiply")
        }
    }

    companion object {
        fun distance(meters: Int): SequencedSeatValue {
            return DistanceValue(meters)
        }

        fun angle(degrees: Int): SequencedSeatValue {
            return AngleValue(degrees)
        }

        fun multiply(v: Float): SequencedSeatValue {
            return MultiplyValue(v)
        }
    }
}
