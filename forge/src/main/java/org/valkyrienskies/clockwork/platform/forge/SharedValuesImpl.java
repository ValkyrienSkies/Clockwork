package org.valkyrienskies.clockwork.platform.forge;

import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.tterrag.registrate.util.entry.EntityEntry;
import net.minecraft.world.item.CreativeModeTab;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueEntity;
import org.valkyrienskies.clockwork.forge.ClockworkGroup;
import org.valkyrienskies.clockwork.forge.ForgeClockworkEntities;
import org.valkyrienskies.clockwork.forge.mixin.accessors.ItemAccessor;
import org.valkyrienskies.clockwork.platform.CWItem;
import org.valkyrienskies.clockwork.platform.api.network.PacketChannel;

import java.util.function.BiConsumer;

public class SharedValuesImpl {
    private static final CreativeModeTab TAB = new ClockworkGroup();

    public static CreativeModeTab creativeTab() {
        return TAB;
    }

    public static EntityEntry<BluperGlueEntity> getBluperGlue() {
        return (EntityEntry) ForgeClockworkEntities.BLUPERGLUE;
    }

    public static PacketChannel getPacketChannel() {
        return null;
    }

    public static BiConsumer<CWItem, CustomRenderedItemModelRenderer<?>> customRenderedRegisterer() {
        return (item, renderer) -> ((ItemAccessor) item).setRenderProperties(SimpleCustomRenderer.create(item, renderer));
    }

}
