package org.valkyrienskies.clockwork;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ClockworkIcons implements ScreenElement {

    public static final ResourceLocation ICON_ATLAS = ClockworkMod.asResource("textures/gui/icons.png");
    public static final int ICON_ATLAS_SIZE = 256;

    private static int x = 0, y = -1;
    private int iconX;
    private int iconY;

    public static final ClockworkIcons
            GRAB = newRow(),
            ASSEMBLE = next(),
            GRABSSEMBLE = next();

    public ClockworkIcons(int x, int y) {
        iconX = x * 16;
        iconY = y * 16;
    }

    private static ClockworkIcons next() {
        return new ClockworkIcons(++x, y);
    }

    private static ClockworkIcons newRow() {
        return new ClockworkIcons(x = 0, ++y);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(ICON_ATLAS, x, y, 0, iconX, iconY, 16, 16, ICON_ATLAS_SIZE, ICON_ATLAS_SIZE);
    }
}
