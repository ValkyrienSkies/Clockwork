package org.valkyrienskies.clockwork.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllSoundEvents.SoundEntry;
import com.simibubi.create.AllSoundEvents.SoundEntryBuilder;
import com.simibubi.create.Create;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class AllClockworkSounds {
    public static final Map<ResourceLocation, SoundEntry> ALL = new HashMap<>();

    public static final SoundEntry

    PHYSICS_INFUSER_INITIALIZE = create("physics_infuser_initialize").subtitle("Physics Infuser starts")
            .category(SoundSource.BLOCKS)
            .attenuationDistance(16)
            .build(),

    PHYSICS_INFUSER_WINDUP = create("physics_infuser_windup").subtitle("Physics Infuser windup")
            .category(SoundSource.BLOCKS)
            .attenuationDistance(16)
            .build(),

    PHYSICS_INFUSER_LIGHTNING = create("physics_infuser_lightning").subtitle("Zap!")
            .category(SoundSource.BLOCKS)
            .attenuationDistance(16)
            .build(),

    PHYSICS_INFUSER_FINISH = create("physics_infuser_finish").subtitle("Physics Infuser infuses")
            .category(SoundSource.BLOCKS)
            .attenuationDistance(16)
            .build();


    private static SoundEntryBuilder create(String name) {
        return create(ClockWorkModFabric.asResource(name));
    }

    public static SoundEntryBuilder create(ResourceLocation id) {
        return new SoundEntryBuilder(id);
    }

    public static void prepare() {
        for (SoundEntry entry : ALL.values())
            entry.prepare();
    }

    public static void register() {
        for (SoundEntry entry : ALL.values())
            entry.register();
    }

    public static JsonObject provideLangEntries() {
        JsonObject object = new JsonObject();
        for (SoundEntry entry : ALL.values())
            if (entry.hasSubtitle())
                object.addProperty(entry.getSubtitleKey(), entry.getSubtitle());
        return object;
    }

    public static SoundEntryProvider provider(DataGenerator generator) {
        return new SoundEntryProvider(generator);
    }


    private static class SoundEntryProvider implements DataProvider {

        private DataGenerator generator;

        public SoundEntryProvider(DataGenerator generator) {
            this.generator = generator;
        }

        @Override
        public void run(HashCache cache) throws IOException {
            generate(generator.getOutputFolder(), cache);
        }

        @Override
        public String getName() {
            return "Create's Custom Sounds";
        }

        public void generate(Path path, HashCache cache) {
            Gson GSON = (new GsonBuilder()).setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            path = path.resolve("assets/create");

            try {
                JsonObject json = new JsonObject();
                ALL.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            entry.getValue()
                                    .write(json);
                        });
                DataProvider.save(GSON, cache, json, path.resolve("sounds.json"));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
