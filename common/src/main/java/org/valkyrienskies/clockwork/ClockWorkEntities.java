package org.valkyrienskies.clockwork;

import com.tterrag.registrate.util.entry.EntityEntry;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatEntity;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueEntity;
import org.valkyrienskies.clockwork.platform.SharedValues;

public class ClockWorkEntities {

    public static final EntityEntry<BluperGlueEntity> BLUPERGLUE = SharedValues.getBluperGlue();
    public static final EntityEntry<SequencedSeatEntity> SEQUENCED_SEAT = SharedValues.getSequencedSeat();

    public static void register() {
    }
}
