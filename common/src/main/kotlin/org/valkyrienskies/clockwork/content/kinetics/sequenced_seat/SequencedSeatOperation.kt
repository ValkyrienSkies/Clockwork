package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import com.simibubi.create.foundation.gui.AllIcons
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent

enum class SequencedSeatOperation(name: String, val icon: AllIcons) {
    NOTHING("nothing", AllIcons.I_NONE),
    TURN_ANGLE("angle", AllIcons.I_ROTATE_CCW),
    TURN_DISTANCE("distance", AllIcons.I_PRIORITY_HIGH),
    MULTIPLY("multiply", AllIcons.I_PRIORITY_VERY_HIGH);

    private val component: Component

    init {
        component = TranslatableComponent("sequenced_seat.operation.$name")
    }

    fun asComponent(): Component {
        return component
    }

    fun defaultValue(): SequencedSeatValue? {
        return when (this) {
            NOTHING -> null
            TURN_ANGLE -> SequencedSeatValue.angle(90)
            TURN_DISTANCE -> SequencedSeatValue.distance(1)
            MULTIPLY -> SequencedSeatValue.multiply(1f)
        }
    }
}