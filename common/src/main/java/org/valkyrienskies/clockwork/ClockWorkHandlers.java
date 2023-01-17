package org.valkyrienskies.clockwork;

import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueSelectionHandler;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronRenderHandler;

public class ClockWorkHandlers {

    public static final GravitronRenderHandler GRAVITRON_HANDLER = new GravitronRenderHandler();
    public static final BluperGlueSelectionHandler BLUPER_HANDLER = new BluperGlueSelectionHandler();

    public static void tick() {
        GRAVITRON_HANDLER.tick();
        BLUPER_HANDLER.tick();
    }
}
