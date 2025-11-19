package org.valkyrienskies.clockwork.fabric.config;

import fuzs.forgeconfigapiport.impl.ForgeConfigAPIPort;
import net.createmod.catnip.config.ConfigBase;
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

//        for (Map.Entry<ModConfig.Type, ConfigBase> pair : CONFIGS.entrySet())
//            ForgeConfigAPIPort.LOGGER.registerConfig(ClockworkMod.MOD_ID, pair.getKey(), pair.getValue().specification);
//
//        BlockStressValues.registerProvider(ClockworkMod.MOD_ID, SERVER.kinetics.stressValues);
//
//        ModConfigEvent.LOADING.register(AllClockworkConfigs::onLoad);
//        ModConfigEvent.RELOADING.register(AllClockworkConfigs::onReload);
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
