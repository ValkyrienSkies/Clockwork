package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import kotlin.math.abs

class SequencedSeatRule(
        val inputKeys: MutableSet<InputKey>,
        val operation: SequencedSeatOperation,
        val value: SequencedSeatValue?
) {

    fun matches(inputKeys: Set<InputKey>): Boolean {
        return inputKeys == this.inputKeys
    }

    fun calculateModifier(be: SequencedSeatBlockEntity, face: Direction, inputKeys: Set<InputKey>): Float {
        val matches = matches(inputKeys)
        return when (operation) {
            SequencedSeatOperation.NOTHING -> 0f
            SequencedSeatOperation.TURN_ANGLE -> angleRotate(be, face, matches)
            SequencedSeatOperation.TURN_DISTANCE -> distanceRotate(be, face, matches)
            SequencedSeatOperation.MULTIPLY -> if (matches) (value as SequencedSeatValue.MultiplyValue).multiplier else 0f
        }
    }

    private var inAction = false
    private fun angleRotate(be: SequencedSeatBlockEntity, face: Direction, matches: Boolean): Float {
        if (!inAction && !matches) return 0f
        val targetDegrees: Int = -(value as SequencedSeatValue.AngleValue).degrees
        val degreesAway = be.getDegreesAwayFromBase(face)
        var diff = if (matches) targetDegrees - degreesAway else -degreesAway
        if (diff > 180) diff -= 360f else if (diff < -180) diff += 360f
        if (diff < 0.1 && diff > -0.1) {
            inAction = matches
            return 0f
        }
        inAction = true
        val degreesPerTick = KineticBlockEntity.convertToAngular(be.speed).toDouble()
        return if (abs(degreesPerTick) > abs(diff)) {
            (diff / degreesPerTick).toFloat()
        } else if (diff * degreesPerTick < 0) -1f else 1f
        // If diff and degrees per tick have different signs, we need to reverse the direction
        // So we use * < to check that
    }

    private fun distanceRotate(be: SequencedSeatBlockEntity, face: Direction, matches: Boolean): Float {
        if (!inAction && !matches) return 0f
        val targetDegrees: Int = -(value as SequencedSeatValue.AngleValue).degrees
        val degreesAway = be.getDegreesAwayFromBase(face)
        var diff = if (matches) targetDegrees - degreesAway else -degreesAway
        if (diff > 180) diff -= 360f else if (diff < -180) diff += 360f
        if (diff < 0.1 && diff > -0.1) {
            inAction = matches
            return 0f
        }
        inAction = true

        val metersPerTick = KineticBlockEntity.convertToLinear(be.getSpeed())
        return if (abs(metersPerTick) > abs(diff)) {
            (diff / metersPerTick)
        } else if (diff * metersPerTick < 0) {
            -1f
        } else {
            1f
        }
    }

    fun serializeNBT(): CompoundTag {
        val tag = CompoundTag()
        tag.putInt("keys", InputKey.asInt(inputKeys))
        tag.putInt("operation", operation.ordinal)
        if (value != null) tag.put("value", value.serializeNBT())
        return tag
    }

    companion object {
        fun empty(): SequencedSeatRule {
            return SequencedSeatRule(HashSet(), SequencedSeatOperation.NOTHING, null)
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun deserializeNBT(tag: CompoundTag): SequencedSeatRule {
            val keys = InputKey.fromInt(tag.getInt("keys"))
            val operation = SequencedSeatOperation.entries[tag.getInt("operation")]
            val value = operation.defaultValue()
            value?.deserializeNBT(tag["value"]!!)
            return SequencedSeatRule(keys, operation, value)
        }
    }
}
