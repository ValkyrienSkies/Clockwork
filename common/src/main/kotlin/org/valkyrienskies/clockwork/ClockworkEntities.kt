package org.valkyrienskies.clockwork

import com.tterrag.registrate.util.entry.EntityEntry
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity
import org.valkyrienskies.clockwork.platform.SharedValues

object ClockworkEntities {
    val SEQUENCED_SEAT: EntityEntry<SequencedSeatEntity> = SharedValues.sequencedSeat

    @JvmStatic
    fun register() {
    }
}