package org.valkyrienskies.clockwork.fabric.config;


import com.simibubi.create.content.kinetics.BlockStressValues;
import com.simibubi.create.foundation.config.ConfigBase;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;
import org.valkyrienskies.clockwork.ClockworkMod;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public class AllClockworkConfigs {
    private static final Map<ModConfig.Type, ConfigBase> CONFIGS = new EnumMap<>(ModConfig.Type.class);

    public static CWClient CLIENT;
    public static CWCommon COMMON;
    public static CWServer SERVER;

    public static ConfigBase byType(ModConfig.Type type) {
        return CONFIGS.get(type);
    }

    private static <T extends CWConfigBase> T init(Supplier<T> factory, ModConfig.Type side) {
        Pair<T, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(builder -> {
            T config = factory.get();
            config.registerAll(builder);
            return config;
        });

        T config = specPair.getLeft();
        config.specification = specPair.getRight();
        CONFIGS.put(side, config);
        return config;
    }

    public static void init() {
        CLIENT = init(CWClient::new, ModConfig.Type.CLIENT);
        COMMON = init(CWCommon::new, ModConfig.Type.COMMON);
        SERVER = init(CWServer::new, ModConfig.Type.SERVER);

        for (Map.Entry<ModConfig.Type, ConfigBase> pair : CONFIGS.entrySet()) {
            ForgeConfigRegistry.INSTANCE.register(ClockworkMod.MOD_ID, pair.getKey(), pair.getValue().specification);
            //ModLoadingContext.registerConfig(ClockworkMod.MOD_ID, pair.getKey(), pair.getValue().specification);
        }


        BlockStressValues.registerProvider(ClockworkMod.MOD_ID, SERVER.kinetics.stressValues);

        ModConfigEvents.loading(ClockworkMod.MOD_ID).register(AllClockworkConfigs::onLoad);
        ModConfigEvents.reloading(ClockworkMod.MOD_ID).register(AllClockworkConfigs::onReload);
    }

    public static void onLoad(ModConfig modConfig) {
        for (ConfigBase config : CONFIGS.values())
            if (config.specification == modConfig
                    .getSpec())
                config.onLoad();
    }

    public static void onReload(ModConfig modConfig) {
        for (ConfigBase config : CONFIGS.values())
            if (config.specification == modConfig
                    .getSpec())
                config.onReload();
    }

}