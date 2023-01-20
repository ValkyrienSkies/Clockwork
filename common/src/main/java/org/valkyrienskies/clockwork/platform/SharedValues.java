package org.valkyrienskies.clockwork.platform;

import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.tterrag.registrate.util.entry.EntityEntry;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.valkyrienskies.clockwork.content.contraptions.sequenced_seat.SequencedSeatEntity;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueEntity;
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel;

import java.util.function.BiConsumer;

public class SharedValues {

    @ExpectPlatform
    public static CreativeModeTab creativeTab() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static PacketChannel getPacketChannel() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static BiConsumer<CWItem, CustomRenderedItemModelRenderer<?>> customRenderedRegisterer() {
        throw new AssertionError();
    }

    //region Entities
    @ExpectPlatform
    public static EntityEntry<BluperGlueEntity> getBluperGlue() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static EntityEntry<SequencedSeatEntity> getSequencedSeat() {
        throw new AssertionError();
    }
    //endregion
}
