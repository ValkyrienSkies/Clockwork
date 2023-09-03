package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import net.minecraft.core.Direction
import net.minecraft.nbt.ListTag
import net.minecraft.world.level.block.Rotation
import javax.annotation.Nonnull

class SequencedSeatRuleList {
    private val rules = arrayOfNulls<SequencedSeatRule>(MAX_RULES)

    init {
        for (i in 0 until MAX_RULES) {
            rules[i] = SequencedSeatRule.empty()
        }
    }

    fun currentModifier(be: SequencedSeatBlockEntity, face: Direction?): Float {
        for (rule in rules) {
            // loop over every possible rule and check if it wants to apply a modifier
            // It will just take the first one that does
            val result = rule!!.calculateModifier(be, face!!, be.pressedKeys())
            if (result != 0f) {
                return result
            }
        }
        return 0f
    }

    @Nonnull
    fun getRule(index: Int): SequencedSeatRule? {
        return rules[index]
    }

    fun setRule(index: Int, rule: SequencedSeatRule?) {
        rules[index] = rule
    }

    fun addKey(index: Int, key: InputKey) {
        getRule(index)!!.inputKeys.add(key)
    }

    fun removeKey(index: Int, key: InputKey) {
        getRule(index)!!.inputKeys.remove(key)
    }

    fun setOperation(index: Int, operation: SequencedSeatOperation) {
        val rule = getRule(index)
        setRule(index, SequencedSeatRule(rule!!.inputKeys, operation, operation.defaultValue()))
    }

    fun serializeNBT(): ListTag {
        val list = ListTag()
        for (rule in rules) {
            list.add(rule!!.serializeNBT())
        }
        return list
    }

    fun deserializeNBT(tag: ListTag) {
        for (i in 0 until MAX_RULES) {
            rules[i] = SequencedSeatRule.deserializeNBT(tag.getCompound(i))
        }
    }

    companion object {
        const val MAX_RULES = 5
        fun defaultList(rotation: Rotation): SequencedSeatRuleList {
            val list = SequencedSeatRuleList()
            list.setOperation(1, SequencedSeatOperation.MULTIPLY)
            when (rotation) {
                Rotation.NONE -> {
                    list.addKey(1, InputKey.FORWARD)
                }

                Rotation.CLOCKWISE_90 -> {
                    list.addKey(1, InputKey.RIGHT)
                }

                Rotation.CLOCKWISE_180 -> {
                    list.addKey(1, InputKey.BACKWARD)
                }

                Rotation.COUNTERCLOCKWISE_90 -> {
                    list.addKey(1, InputKey.LEFT)
                }
            }
            return list
        }
    }
}