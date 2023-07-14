package org.valkyrienskies.clockwork.platform;

import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.tterrag.registrate.util.entry.EntityEntry;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity;
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.BiConsumer;

public class SharedValues {

    @ExpectPlatform
    public static PacketChannel getPacketChannel() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static BiConsumer<CWItem, CustomRenderedItemModelRenderer> customRenderedRegisterer() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static BiConsumer<BlockItem, CustomRenderedItemModelRenderer> customBlockItemRenderedRegisterer() {
        throw new AssertionError();
    }

    //region Entities

    @ExpectPlatform
    public static EntityEntry<SequencedSeatEntity> getSequencedSeat() {
        throw new AssertionError();
    }
    //endregion

    @ExpectPlatform
    public static ArrayList<Item> getFrostingBuckets() {
        throw new AssertionError();
    }
}
