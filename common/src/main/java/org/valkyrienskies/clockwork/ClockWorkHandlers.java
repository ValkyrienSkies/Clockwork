package org.valkyrienskies.clockwork;

import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueSelectionHandler;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronRenderHandler;
import org.valkyrienskies.clockwork.content.curiosities.tools.pastrymaker.PastrymakerRenderHandler;

public class ClockWorkHandlers {

    public static final GravitronRenderHandler GRAVITRON_HANDLER = new GravitronRenderHandler();
    public static final BluperGlueSelectionHandler BLUPER_HANDLER = new BluperGlueSelectionHandler();
    public static final PastrymakerRenderHandler PASTRYMAKER_RENDER_HANDLER = new PastrymakerRenderHandler();

    public static void tick() {
        GRAVITRON_HANDLER.tick();
        BLUPER_HANDLER.tick();
        PASTRYMAKER_RENDER_HANDLER.tick();
    }
}
