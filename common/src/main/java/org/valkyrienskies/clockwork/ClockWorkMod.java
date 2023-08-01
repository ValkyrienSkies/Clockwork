package org.valkyrienskies.clockwork;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.outliner.Outliner;
import dev.architectury.registry.CreativeTabRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valkyrienskies.clockwork.platform.SharedValues;

public class ClockWorkMod {
    public static final String MOD_ID = "vs_clockwork";

    // versioning
    public static final int BUILD_VERSION = 1;
    public static final int NETWORK_VERSION = 1;
    public static final String NETWORK_VERSION_STR = String.valueOf(NETWORK_VERSION);

    public static final ResourceLocation NETWORK_CHANNEL = ClockWorkMod.asResource("main");


    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ClockWorkMod.MOD_ID);
    public static final CreativeModeTab BASE_CREATIVE_TAB = new ClockWorkTab();
    public static final Logger MIXIN_LOGGER = LoggerFactory.getLogger("ClockworkMixins");
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Outliner OUTLINER = new Outliner();

    public static void init() {
        ClockWorkContraptions.init();
        ClockWorkPackets.init();
    }

    public static void initClient() {


    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
