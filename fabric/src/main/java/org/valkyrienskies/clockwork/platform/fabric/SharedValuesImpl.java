package org.valkyrienskies.clockwork.platform.fabric;

import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.tterrag.registrate.util.entry.EntityEntry;
import com.tterrag.registrate.util.entry.FluidEntry;
import io.github.fabricators_of_create.porting_lib.util.SimpleFlowableFluid;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.valkyrienskies.clockwork.content.kinetics.sequenced_seat.SequencedSeatEntity;
import org.valkyrienskies.clockwork.fabric.ClockWorkGroup;
import org.valkyrienskies.clockwork.fabric.FabricClockworkEntities;
import org.valkyrienskies.clockwork.fabric.FabricClockworkFluids;
import org.valkyrienskies.clockwork.platform.CWItem;
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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

    public static ArrayList<Item> getFrostingBuckets() {
        ArrayList<Item> frostingBuckets = new ArrayList<>();
        frostingBuckets.add(FabricClockworkFluids.VANILLA_FROSTING.get().getBucket());
        frostingBuckets.add(FabricClockworkFluids.CHOCOLATE_FROSTING.get().getBucket());
        frostingBuckets.add(FabricClockworkFluids.STRAWBERRY_FROSTING.get().getBucket());
        return frostingBuckets;
    }

}
