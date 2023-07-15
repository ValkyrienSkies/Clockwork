package org.valkyrienskies.clockwork.platform.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity;

public class ForgeSequencedSeatEntity extends SequencedSeatEntity {
    public ForgeSequencedSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
    }
}
