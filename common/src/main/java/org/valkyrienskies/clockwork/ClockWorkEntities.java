package org.valkyrienskies.clockwork;

import com.simibubi.create.content.contraptions.components.actors.SeatEntity;
import com.simibubi.create.foundation.data.CreateEntityBuilder;
import com.simibubi.create.foundation.utility.Lang;
import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatEntity;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueEntity;
import org.valkyrienskies.clockwork.platform.SharedValues;

import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ClockWorkEntities {

    public static final EntityEntry<BluperGlueEntity> BLUPERGLUE = SharedValues.getBluperGlue();
    public static final EntityEntry<SequencedSeatEntity> SEQUENCED_SEAT = SharedValues.getSequencedSeat();

    public static void register() {}
}
