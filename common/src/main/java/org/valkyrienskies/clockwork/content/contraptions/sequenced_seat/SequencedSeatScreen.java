package org.valkyrienskies.clockwork.content.contraptions.sequenced_seat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.ClockWorkBlocks;
import org.valkyrienskies.clockwork.ClockWorkGuiTextures;

import java.util.Arrays;
import java.util.function.Consumer;

public class SequencedSeatScreen extends AbstractSimiScreen {
    private final ItemStack renderedItem = ClockWorkBlocks.SEQUENCED_SEAT.asStack();
    private final ClockWorkGuiTextures background = ClockWorkGuiTextures.SEQUENCED_SEAT;
    private final SequencedSeatBlockEntity be;

    private IconButton confirmButton;
    private SelectionScrollInput[] operationInputs = new SelectionScrollInput[SequencedSeatRuleList.MAX_RULES];
    private ScrollInput[] valueInputs = new ScrollInput[SequencedSeatRuleList.MAX_RULES];
    private Rotation currentShaft = Rotation.NONE;
    public SequencedSeatScreen(SequencedSeatBlockEntity be) {
        this.be = be;
    }

    @Override
    protected void init() {
        setWindowSize(background.width, background.height);
        setWindowOffset(-20, 0);
        super.init();
        int x = this.guiLeft;
        int y = this.guiTop;

        this.confirmButton = new IconButton(x + this.background.width - 33, y + this.background.height - 24, AllIcons.I_CONFIRM);
        this.confirmButton.withCallback(this::onClose);

        makeTabButtons();
        makeKeyButtons();
        makeOperationInputs();
        makeValueInputs();

        this.addRenderableWidget(this.confirmButton);
    }

    @Override
    protected void renderWindow(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        background.render(ms, x, y, this);

        drawCenteredString(ms, font, title, x + (background.width - 8) / 2, y + 3, 0xFFFFFF);

        drawRuleList(ms, x, y, partialTicks);


        GuiGameElement.of(renderedItem)
                .<GuiGameElement.GuiRenderBuilder>at(x + background.width + 6, y + background.height - 56, -200)
                .scale(5)
                .render(ms);
    }

    private void drawRuleList(PoseStack ms, int x, int y, float partialTicks) {
        SequencedSeatRuleList list = currentList();
        for (int i = 0; i < SequencedSeatRuleList.MAX_RULES; i++) {
            SequencedSeatRule rule = list.getRule(i);
            int ruleX = x + 36;
            int ruleY = y + 18 + i * (INPUT_FIELDS_HEIGHT + INPUT_FIELDS_MARGIN);
            if (!rule.inputKeys().isEmpty() || i == 0) {
                operationInputs[i].visible = true;
                SequencedSeatOperation operation = rule.operation();

                if (operation != SequencedSeatOperation.NOTHING) {
                    valueInputs[i].visible = true;

                    drawInputField(ruleX, ruleY, ms, partialTicks, 0);
                } else {
                    valueInputs[i].visible = false;
                    drawInputField(ruleX, ruleY, ms, partialTicks, 1);
                }
                operation.getIcon().render(ms, ruleX + 1, ruleY + 1);

                drawString(
                        ms,
                        font,
                        operation.asComponent(),
                        ruleX + 18,
                        ruleY + ((INPUT_FIELDS_HEIGHT - font.lineHeight) / 2),
                        0xFFFFFF
                );
            } else {
                operationInputs[i].visible = false;
                valueInputs[i].visible = false;

                drawInputField(ruleX, ruleY, ms, partialTicks, 2);
            }
        }
    }

    private void drawInputField(int x, int y, @NotNull PoseStack ms, float partialTicks, int i) {
        background.bind();
        blit(ms, x, y,
                INPUT_FIELDS_X,
                INPUT_FIELDS_Y + (i * (INPUT_FIELDS_HEIGHT + INPUT_FIELDS_MARGIN)),
                INPUT_FIELDS_WIDTH,
                INPUT_FIELDS_HEIGHT
        );
    }

    private void makeOperationInputs() {
        int x = guiLeft;
        int y = guiTop;
        for (int i = 0; i < SequencedSeatRuleList.MAX_RULES; i++) {
            int ruleX = x + 36;
            int ruleY = y + 18 + i * (INPUT_FIELDS_HEIGHT + INPUT_FIELDS_MARGIN);

            SelectionScrollInput input = operationInputs[i] = new SelectionScrollInput(
                    ruleX,
                    ruleY,
                    INPUT_OPERATION_WIDTH,
                    INPUT_FIELDS_HEIGHT
            );

            input.visible = false;
            input.forOptions(Arrays.stream(SequencedSeatOperation.values())
                    .map(SequencedSeatOperation::asComponent)
                    .toList());
            input.calling(onOperationChanged(i));
            input.setState(currentList().getRule(i).operation().ordinal());

            addRenderableWidget(input);
        }
    }

    private Consumer<Integer> onOperationChanged(int index) {
        return (ordinal) -> {
            SequencedSeatOperation operation = SequencedSeatOperation.values()[ordinal];
            currentList().setOperation(index, operation);
        };
    }

    private void makeValueInputs() {
        int x = guiLeft;
        int y = guiTop;
        for (int i = 0; i < SequencedSeatRuleList.MAX_RULES; i++) {
            int ruleX = x + 36;
            int ruleY = y + 18 + i * (INPUT_FIELDS_HEIGHT + INPUT_FIELDS_MARGIN);

            ScrollInput input = valueInputs[i] = new ScrollInput(
                    ruleX + 2,
                    ruleY + 2,
                    INPUT_OPERATION_WIDTH - 4,
                    INPUT_FIELDS_HEIGHT - 4
            );

            input.visible = false;
        }
    }

    private Consumer<Integer> onValueChanged(int index) {
        return (ordinal) -> {
            //TODO value
            //currentList().setValue(index, new SequencedSeatValue());
        };
    }


    private void makeTabButtons() {
        for (Rotation rotation : Rotation.values()) {
            this.addRenderableWidget(createTabButton(rotation));
        }
    }

    private TabButton createTabButton(Rotation rotation) {
        int buttonX = 0, buttonY = 0;
        int width = 5, height = 5;

        // Configure these numbers for offsetting the buttons
        if (Rotation.NONE == rotation || Rotation.CLOCKWISE_180 == rotation) {
            buttonX = 5;
            width = 7;
        } else if (Rotation.COUNTERCLOCKWISE_90 == rotation) {
            buttonX = 0;
        } else if (Rotation.CLOCKWISE_90 == rotation) {
            buttonX = 12;
        }

        if (Rotation.CLOCKWISE_90 == rotation || Rotation.COUNTERCLOCKWISE_90 == rotation) {
            buttonY = 5;
            height = 7;
        } else if (Rotation.NONE == rotation) {
            buttonY = 0;
        } else if (Rotation.CLOCKWISE_180 == rotation) {
            buttonY = 12;
        }

        return new TabButton(buttonX, buttonY, width, height, rotation);
    }
    private class TabButton extends AbstractSimiWidget {
        private final Rotation rotation;
        private final int blitX, blitY;
        private TabButton(int x, int y, int width, int height, Rotation rotation) {
            super(
                    guiLeft + x + TAB_PAD_X,
                    guiTop + y + TAB_PAD_Y,
                    width,
                    height,
                    Component.nullToEmpty(rotation.toString())
            );

            blitX = x + 205;
            blitY = y;

            this.rotation = rotation;
            withCallback(() -> currentShaft = rotation);
        }

        @Override
        public void renderButton(@NotNull PoseStack ms, int mouseX, int mouseY, float partialTicks) {
            isHovered = rotation == currentShaft || (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height);

            background.bind();
            blit(ms, x, y, isHovered ? 17 + blitX : blitX, blitY, width, height);
        }
    }

    private void makeKeyButtons() {
        // i = 1 cus first rule has no buttons
        for (int i = 1; i < 5; i++) {
            for (InputKey key : InputKey.values()) {
                this.addRenderableWidget(createKeyButton(key, i));
            }
        }
    }

    private boolean isKeySelected(InputKey key, int index) {
        SequencedSeatRule rule = currentList().getRule(index);
        return rule.inputKeys().contains(key);
    }

    private void selectKey(InputKey key, int index) {
        currentList().addKey(index, key);
    }

    private void deselectKey(InputKey key, int index) {
        currentList().removeKey(index, key);
    }

    KeyButton createKeyButton(InputKey key, int index) {
        int buttonX = 0, buttonY = 0;
        int width = 6, height = 6;

        // Configure these numbers for offsetting the buttons
        if (InputKey.FORWARD == key || InputKey.BACKWARD == key) {
            buttonX = 5;
            width = 7;
        } else if (InputKey.LEFT == key) {
            buttonX = 0;
        } else if (InputKey.RIGHT == key) {
            buttonX = 11;
        } else if (InputKey.JUMP == key) {
            width = 5;
            height = 5;
            buttonX = 6;
            buttonY = 6;
        }

        if (InputKey.LEFT == key || InputKey.RIGHT == key) {
            buttonY = 5;
            height = 7;
        } else if (InputKey.FORWARD == key) {
            buttonY = 0;
        } else if (InputKey.BACKWARD == key) {
            buttonY = 11;
        }

        return new KeyButton(buttonX, buttonY, width, height, key, index);
    }

    private class KeyButton extends AbstractSimiWidget {
        private final InputKey key;
        private final int index;
        private final int blitX, blitY;
        private KeyButton(int x, int y, int width, int height, InputKey key, int index) {
            super(
                    guiLeft + x + INPUT_PAD_X,
                    guiTop + y + INPUT_PAD_Y + ((index - 1) * INPUT_PAD_MARGIN),
                    width,
                    height,
                    Component.nullToEmpty(key.toString())
            );

            blitX = x + 205;
            blitY = y + 17;

            this.key = key;
            this.index = index;

            withCallback(() -> {
                if (isKeySelected(key, index))
                    deselectKey(key, index);
                else
                    selectKey(key, index);
            });
        }

        @Override
        public void renderButton(@NotNull PoseStack ms, int mouseX, int mouseY, float partialTicks) {
            isHovered = isKeySelected(key, index) || (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height);

            background.bind();
            blit(ms, x, y, isHovered ? 17 + blitX : blitX, blitY, width, height);
        }
    }

    private SequencedSeatRuleList currentList() {
        return be.getList(currentShaft);
    }

    private static final int TAB_PAD_X = 11;
    private static final int TAB_PAD_Y = 16;
    private static final int INPUT_PAD_X = 11;
    private static final int INPUT_PAD_Y = 41;
    private static final int INPUT_PAD_MARGIN = 22;
    private static final int INPUT_FIELDS_X = 36;
    private static final int INPUT_FIELDS_Y = 62;
    private static final int INPUT_FIELDS_WIDTH = 110;
    private static final int INPUT_FIELDS_HEIGHT = 18;
    private static final int INPUT_FIELDS_MARGIN = 4;
    private static final int INPUT_OPERATION_WIDTH = 60;
}
