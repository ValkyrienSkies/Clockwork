package org.valkyrienskies.clockwork;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.gui.element.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public enum ClockworkGuiTextures implements ScreenElement {

    GRAVITRON_SELECTED("widgets", 0, 0, 22, 22);

    public final ResourceLocation location;
    public int width, height;
    public int startX, startY;

    ClockworkGuiTextures(String location, int width, int height) {
        this(location, 0, 0, width, height);
    }

    ClockworkGuiTextures(int startX, int startY) {
        this("icons", startX * 16, startY * 16, 16, 16);
    }

    ClockworkGuiTextures(String location, int startX, int startY, int width, int height) {
        this(ClockworkMod.MOD_ID, location, startX, startY, width, height);
    }

    ClockworkGuiTextures(String namespace, String location, int startX, int startY, int width, int height) {
        this.location = new ResourceLocation(namespace, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, startX, startY, width, height);
    }
}
