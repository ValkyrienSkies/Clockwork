package org.valkyrienskies.clockwork.ponder;

import com.simibubi.create.foundation.ponder.PonderLocalization;
import com.simibubi.create.foundation.ponder.PonderRegistrationHelper;
import com.simibubi.create.foundation.ponder.PonderRegistry;
import org.valkyrienskies.clockwork.ClockWorkBlocks;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.platform.PlatformUtils;
import org.valkyrienskies.clockwork.ponder.scene.CombustionEngineScene;

public class ClockworkPonderIndex {
    private static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(ClockWorkMod.MOD_ID);

    public static void register() {
        HELPER.forComponents(ClockWorkBlocks.COMBUSTION_ENGINE)
                .addStoryBoard("combustion_engine/combustionengine",
                        CombustionEngineScene::use);
    }

    public static void registerTags() {
        PonderRegistry.TAGS.forTag(ClockworkPonderTags.COMBUSTION_ENGINE)
                .add(ClockWorkBlocks.COMBUSTION_ENGINE)
                .add(PlatformUtils.getVanillaFrostingItem())
                .add(PlatformUtils.getChocolateFrostingItem())
                .add(PlatformUtils.getStrawberryFrostingItem());
    }

    public static void registerLang() {
        PonderLocalization.provideRegistrateLang(ClockWorkMod.REGISTRATE);
    }
}
