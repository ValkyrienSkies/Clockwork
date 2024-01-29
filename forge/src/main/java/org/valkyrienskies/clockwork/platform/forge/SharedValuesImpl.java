package org.valkyrienskies.clockwork.platform.forge;

import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.EntityEntry;
import net.minecraft.world.item.BlockItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorClusterRenderer;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity;
import org.valkyrienskies.clockwork.content.physicalities.ballast.BallastBlockEntity;
import org.valkyrienskies.clockwork.forge.ClockworkModForge;
import org.valkyrienskies.clockwork.forge.ClockworkModForgeClient;
import org.valkyrienskies.clockwork.forge.ForgeClockworkBlockEntities;
import org.valkyrienskies.clockwork.forge.ForgeClockworkEntities;
import org.valkyrienskies.clockwork.forge.mixin.accessors.ItemAccessor;
import org.valkyrienskies.clockwork.platform.CWItem;
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel;

import java.util.function.BiConsumer;

public class SharedValuesImpl {
    private static final PacketChannel CHANNEL = new PacketChannelImpl();

    public static PacketChannel getPacketChannel() {
        return CHANNEL;
    }

    public static BiConsumer<CWItem, CustomRenderedItemModelRenderer> customRenderedRegisterer() {
        return (item, renderer) -> ((ItemAccessor) item).setRenderProperties(SimpleCustomRenderer.create(item, renderer));
    }

    public static BiConsumer<BlockItem, CustomRenderedItemModelRenderer> customBlockItemRenderedRegisterer() {
        return (item, renderer) -> ((ItemAccessor) item).setRenderProperties(SimpleCustomRenderer.create(item, renderer));
    }

    public static EntityEntry<SequencedSeatEntity> getSequencedSeat() {
        return (EntityEntry) ForgeClockworkEntities.SEQUENCED_SEAT;
    }

    public static BlockEntityEntry<BallastBlockEntity> getBallast() {
        return (BlockEntityEntry) ForgeClockworkBlockEntities.BALLAST;
    }

    public static GravitronHandler getGravitronHandler() {
        return ClockworkModForgeClient.GRAVITRON_HANDLER;
    }

    public static AuricDesignatorClusterRenderer getAuricHandler() {
        return ClockworkModForgeClient.AURIC_HANDLER;
    }
}
