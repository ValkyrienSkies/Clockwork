package org.valkyrienskies.clockwork.platform.forge;

import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.EntityEntry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler;
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandHandler;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity;
import org.valkyrienskies.clockwork.forge.ClockworkModForgeClient;
import org.valkyrienskies.clockwork.forge.ForgeClockworkEntities;
import org.valkyrienskies.clockwork.forge.mixin.accessors.ItemAccessor;
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel;

import java.util.function.BiConsumer;

public class SharedValuesImpl {
    private static final PacketChannel CHANNEL = new PacketChannelImpl();

    public static PacketChannel getPacketChannel() {
        return CHANNEL;
    }

    public static BiConsumer<Item, CustomRenderedItemModelRenderer> customRenderedRegisterer() {
        return (item, renderer) -> ((ItemAccessor) item).setRenderProperties(SimpleCustomRenderer.create(item, renderer));
    }

    public static BiConsumer<BlockItem, CustomRenderedItemModelRenderer> customBlockItemRenderedRegisterer() {
        return (item, renderer) -> ((ItemAccessor) item).setRenderProperties(SimpleCustomRenderer.create(item, renderer));
    }

    public static EntityEntry<SequencedSeatEntity> getSequencedSeat() {
        return (EntityEntry) ForgeClockworkEntities.SEQUENCED_SEAT;
    }

    public static GravitronHandler getGravitronHandler() {
        return ClockworkModForgeClient.GRAVITRON_HANDLER;
    }

    public static WanderwandHandler getWanderwandHandler() {
        return ClockworkModForgeClient.WANDERWAND_HANDLER;
    }

//    public static WanderWandClusterRenderer getAuricHandler() {
//        return ClockworkModForgeClient.WANDER_HANDLER;
//    }
}
