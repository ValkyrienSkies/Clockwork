package org.valkyrienskies.clockwork.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.valkyrienskies.clockwork.ponder.ClockworkPonderIndex;
import org.valkyrienskies.clockwork.ponder.ClockworkPonderTags;

public class ClockworkDataGen {
    public static void register(DataGenerator gen, ExistingFileHelper helper, boolean client, boolean server) {
        if (client) {
            ClockworkPonderTags.register();
            ClockworkPonderIndex.register();
            ClockworkPonderIndex.registerLang();
        }
    }
}
