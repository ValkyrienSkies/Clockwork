package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import net.minecraft.world.item.ItemStack;
import org.valkyrienskies.clockwork.ClockWorkBlocks;
import org.valkyrienskies.clockwork.ClockWorkGuiTextures;

public class SequencedSeatScreen extends AbstractSimiScreen {
    private final ItemStack renderedItem = ClockWorkBlocks.SEQUENCED_SEAT.asStack();
    private final ClockWorkGuiTextures background = ClockWorkGuiTextures.SEQUENCED_SEAT;

    public SequencedSeatScreen(SequencedSeatBlockEntity be) {

    }

    @Override
    protected void init() {
        setWindowSize(background.width, background.height);
        setWindowOffset(-20, 0);
        super.init();
    }

    @Override
    protected void renderWindow(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        background.render(ms, x, y, this);

        drawCenteredString(ms, font, title, x + (background.width - 8) / 2, y + 3, 0xFFFFFF);


        GuiGameElement.of(renderedItem)
                .<GuiGameElement.GuiRenderBuilder>at(x + background.width + 6, y + background.height - 56, -200)
                .scale(5)
                .render(ms);
    }
}
