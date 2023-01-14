package org.valkyrienskies.clockwork;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.valkyrienskies.clockwork.platform.SharedValues;

public class ClockWorkMod {
    public static final String MOD_ID = "vs_clockwork";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ClockWorkMod.MOD_ID);
    public static final CreativeModeTab BASE_CREATIVE_TAB = SharedValues.creativeTab();
    public static final Logger MIXIN_LOGGER = LoggerFactory.getLogger("ClockworkMixins");
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {

    }

    public static void initClient() {

    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
