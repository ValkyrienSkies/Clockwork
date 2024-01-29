package org.valkyrienskies.clockwork.platform.fabric;


import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.EntityEntry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorClusterRenderer;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronHandler;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity;
import org.valkyrienskies.clockwork.content.physicalities.ballast.BallastBlockEntity;
import org.valkyrienskies.clockwork.fabric.*;
import org.valkyrienskies.clockwork.platform.CWItem;
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel;

import java.util.ArrayList;
import java.util.function.BiConsumer;

public class SharedValuesImpl {
    private static final PacketChannel CHANNEL = new PacketChannelImpl();

    public static PacketChannel getPacketChannel() {
        return CHANNEL;
    }

    public static BiConsumer<CWItem, CustomRenderedItemModelRenderer> customRenderedRegisterer() {
        return BuiltinItemRendererRegistry.INSTANCE::register;
    }

    public static BiConsumer<BlockItem, CustomRenderedItemModelRenderer> customBlockItemRenderedRegisterer() {
        return BuiltinItemRendererRegistry.INSTANCE::register;
    }

    public static EntityEntry<SequencedSeatEntity> getSequencedSeat() {
        return (EntityEntry) FabricClockworkEntities.SEQUENCED_SEAT;
    }

    public static BlockEntityEntry<BallastBlockEntity> getBallast() {
        return (BlockEntityEntry) FabricClockworkBlockEntities.BALLAST;
    }

    public static ArrayList<Item> getFrostingBuckets() {
        ArrayList<Item> frostingBuckets = new ArrayList<>();
        frostingBuckets.add(FabricClockworkFluids.VANILLA_FROSTING.get().getBucket());
        frostingBuckets.add(FabricClockworkFluids.CHOCOLATE_FROSTING.get().getBucket());
        frostingBuckets.add(FabricClockworkFluids.STRAWBERRY_FROSTING.get().getBucket());
        return frostingBuckets;
    }

    public static GravitronHandler getGravitronHandler() {
        return ClockworkModFabricClient.GRAVITRON_HANDLER;
    }

    public static AuricDesignatorClusterRenderer getAuricHandler() {
        return ClockworkModFabric.AURIC_HANDLER;
    }

}