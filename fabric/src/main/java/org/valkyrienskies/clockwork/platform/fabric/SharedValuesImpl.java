package org.valkyrienskies.clockwork.platform.fabric;

import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.tterrag.registrate.util.entry.EntityEntry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.world.item.CreativeModeTab;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueEntity;
import org.valkyrienskies.clockwork.fabric.ClockWorkGroup;
import org.valkyrienskies.clockwork.fabric.FabricClockworkEntities;
import org.valkyrienskies.clockwork.platform.CWItem;
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel;

import java.util.function.BiConsumer;

public class SharedValuesImpl {
    private static final CreativeModeTab TAB = new ClockWorkGroup();
    private static final PacketChannel CHANNEL = new PacketChannelImpl();

    public static CreativeModeTab creativeTab() {
        return TAB;
    }

    public static EntityEntry<BluperGlueEntity> getBluperGlue() {
        return (EntityEntry) FabricClockworkEntities.BLUPERGLUE;
    }

    public static PacketChannel getPacketChannel() {
        return CHANNEL;
    }

    public static BiConsumer<CWItem, CustomRenderedItemModelRenderer<?>> customRenderedRegisterer() {
        return BuiltinItemRendererRegistry.INSTANCE::register;
    }

}
