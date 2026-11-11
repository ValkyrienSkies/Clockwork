package org.valkyrienskies.clockwork

import com.tterrag.registrate.util.entry.EntityEntry
import org.valkyrienskies.clockwork.content.contraptions.propeller.contraption.CopterContraptionEntity
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity
import org.valkyrienskies.clockwork.platform.SharedValues

object ClockworkEntities {
    val SEQUENCED_SEAT: EntityEntry<SequencedSeatEntity> = SharedValues.sequencedSeat
    val COPTER_CONTRAPTION: EntityEntry<CopterContraptionEntity> = SharedValues.copterContraption

    @JvmStatic
    fun register() {
    }
}
