package org.valkyrienskies.clockwork.content.kinetics.sequenced_seat

import com.simibubi.create.foundation.gui.AbstractSimiScreen
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.element.GuiGameElement
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.ScrollInput
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Rotation
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkIconTextures
import org.valkyrienskies.clockwork.ClockworkPackets
import java.util.*
import java.util.function.Consumer

class SequencedSeatScreen(private val be: SequencedSeatBlockEntity) : AbstractSimiScreen() {
    private val renderedItem: ItemStack = ClockworkBlocks.COMMAND_SEAT.asStack()
    private val background: ClockworkIconTextures = ClockworkIconTextures.COMMAND_SEAT
    private val operationInputs = arrayOfNulls<SelectionScrollInput>(SequencedSeatRuleList.MAX_RULES)
    private val valueInputs = arrayOfNulls<ScrollInput>(SequencedSeatRuleList.MAX_RULES)
    private var confirmButton: IconButton? = null
    private var currentShaft = Rotation.NONE
    override fun init() {
        setWindowSize(background.width, background.height)
        setWindowOffset(-20, 0)
        super.init()
        val x = guiLeft
        val y = guiTop
        confirmButton = IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM)
        confirmButton!!.withCallback<AbstractSimiWidget>(Runnable { onClose() })
        makeTabButtons()
        makeKeyButtons()
        makeOperationInputs()
        makeValueInputs()
        addRenderableWidget(confirmButton!!)
        updateTab(Rotation.NONE)
    }

    override fun onClose() {
        super.onClose()
        ClockworkPackets.sendToServer(UpdateSeatRulesPacket(be))
    }

    override fun renderWindow(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val x = guiLeft
        val y = guiTop

        background.render(guiGraphics, x, y)
        guiGraphics.drawCenteredString(font, title, x + (background.width - 8) / 2, y + 3, 0xFFFFFF)
        drawRuleList(guiGraphics, x, y, partialTicks)
        GuiGameElement.of(renderedItem)
            .at<GuiGameElement.GuiRenderBuilder>(
                (x + background.width + 6).toFloat(),
                (y + background.height - 56).toFloat(), -200f
            )
            .scale(5.0)
            .render(guiGraphics)
    }

    private fun drawRuleList(guiGraphics: GuiGraphics, x: Int, y: Int, partialTicks: Float) {
        val list = currentList()
        for (i in 0 until SequencedSeatRuleList.MAX_RULES) {
            val rule = list.getRule(i)
            val ruleX = x + 38
            val ruleY = y + 18 + i * (INPUT_FIELDS_HEIGHT + INPUT_FIELDS_MARGIN)
            if (!rule!!.inputKeys.isEmpty() || i == 0) {
                operationInputs[i]!!.visible = true
                val operation = rule.operation
                if (operation !== SequencedSeatOperation.NOTHING) {
                    valueInputs[i]!!.visible = true
                    drawInputField(ruleX, ruleY, guiGraphics, partialTicks, 0)
                    guiGraphics.drawCenteredString(
                        font,
                        rule.value!!.asComponent(),
                        ruleX + 62 + INPUT_VALUE_WIDTH / 2,
                        ruleY + (INPUT_FIELDS_HEIGHT - font.lineHeight) / 2 + 1,
                        0xFFFFFF
                    )
                } else {
                    valueInputs[i]!!.visible = false
                    drawInputField(ruleX, ruleY, guiGraphics, partialTicks, 1)
                }
                operation.icon.render(guiGraphics, ruleX + 1, ruleY + 1)
                guiGraphics.drawString(
                    font,
                    operation.asComponent(),
                    ruleX + 16,
                    ruleY + (INPUT_FIELDS_HEIGHT - font.lineHeight) / 2 + 1,
                    0xFFFFFF
                )
            } else {
                operationInputs[i]!!.visible = false
                valueInputs[i]!!.visible = false
                drawInputField(ruleX, ruleY, guiGraphics, partialTicks, 2)
            }
        }
    }


    private fun drawInputField(x: Int, y: Int, guiGraphics: GuiGraphics, partialTicks: Float, i: Int) {
        guiGraphics.blit(
            background.location, x - 2, y,
            INPUT_FIELDS_X,
            INPUT_FIELDS_Y + i * (INPUT_FIELDS_HEIGHT + INPUT_FIELDS_MARGIN),
            INPUT_FIELDS_WIDTH,
            INPUT_FIELDS_HEIGHT
        )
    }


    private fun makeOperationInputs() {
        val x = guiLeft
        val y = guiTop
        for (i in 0 until SequencedSeatRuleList.MAX_RULES) {
            val ruleX = x + 36
            val ruleY = y + 18 + i * (INPUT_FIELDS_HEIGHT + INPUT_FIELDS_MARGIN)
            operationInputs[i] = SelectionScrollInput(
                ruleX,
                ruleY,
                INPUT_OPERATION_WIDTH,
                INPUT_FIELDS_HEIGHT
            )
            val input = operationInputs[i]
            input!!.visible = false
            input.forOptions(
                Arrays.stream(SequencedSeatOperation.values())
                    .map { obj: SequencedSeatOperation -> obj.asComponent() }
                    .toList())
            input.calling(onOperationChanged(i))
            addRenderableWidget(input)
        }
    }

    private fun onOperationChanged(index: Int): Consumer<Int> {
        return Consumer<Int> { ordinal: Int? ->
            val operation = SequencedSeatOperation.values()[ordinal!!]
            currentList().setOperation(index, operation)
            val value = currentList().getRule(index)!!.value
            value?.configureInput(valueInputs[index]!!)
        }
    }

    private fun makeValueInputs() {
        val x = guiLeft
        val y = guiTop
        for (i in 0 until SequencedSeatRuleList.MAX_RULES) {
            val ruleX = x + 100
            val ruleY = y + 18 + i * (INPUT_FIELDS_HEIGHT + INPUT_FIELDS_MARGIN)
            valueInputs[i] = ScrollInput(
                ruleX + 2,
                ruleY + 2,
                INPUT_VALUE_WIDTH - 4,
                INPUT_FIELDS_HEIGHT - 4
            )
            val input = valueInputs[i]
            input!!.visible = false
            addRenderableWidget(input)
        }
    }

    private fun makeTabButtons() {
        for (rotation in Rotation.values()) {
            addRenderableWidget(createTabButton(rotation))
        }
    }

    private fun createTabButton(rotation: Rotation): TabButton {
        var buttonX = 0
        var buttonY = 0
        var width = 5
        var height = 5

        // Configure these numbers for offsetting the buttons
        if (Rotation.NONE == rotation || Rotation.CLOCKWISE_180 == rotation) {
            buttonX = 5
            width = 7
        } else if (Rotation.COUNTERCLOCKWISE_90 == rotation) {
            buttonX = 0
        } else if (Rotation.CLOCKWISE_90 == rotation) {
            buttonX = 12
        }
        if (Rotation.CLOCKWISE_90 == rotation || Rotation.COUNTERCLOCKWISE_90 == rotation) {
            buttonY = 5
            height = 7
        } else if (Rotation.NONE == rotation) {
            buttonY = 0
        } else if (Rotation.CLOCKWISE_180 == rotation) {
            buttonY = 12
        }
        return TabButton(buttonX, buttonY, width, height, rotation)
    }

    private fun updateTab(rotation: Rotation) {
        currentShaft = rotation
        for (i in 0 until SequencedSeatRuleList.MAX_RULES) {
            operationInputs[i]!!.setState(currentList().getRule(i)!!.operation.ordinal)
        }
        for (i in 0 until SequencedSeatRuleList.MAX_RULES) {
            val value = currentList().getRule(i)!!.value
            value?.configureInput(valueInputs[i]!!)
        }
    }

    private fun makeKeyButtons() {
        // i = 1 cus first rule has no buttons
        for (i in 1..4) {
            for (key in InputKey.values()) {
                addRenderableWidget(createKeyButton(key, i))
            }
        }
    }

    private fun isKeySelected(key: InputKey, index: Int): Boolean {
        val rule = currentList().getRule(index)
        return rule!!.inputKeys.contains(key)
    }

    private fun selectKey(key: InputKey, index: Int) {
        currentList().addKey(index, key)
    }

    private fun deselectKey(key: InputKey, index: Int) {
        currentList().removeKey(index, key)
    }

    fun createKeyButton(key: InputKey, index: Int): KeyButton {
        var buttonX = 0
        var buttonY = 0
        var width = 6
        var height = 6

        // Configure these numbers for offsetting the buttons
        if (InputKey.FORWARD === key || InputKey.BACKWARD === key) {
            buttonX = 5
            width = 7
        } else if (InputKey.LEFT === key) {
            buttonX = 0
        } else if (InputKey.RIGHT === key) {
            buttonX = 11
        } else if (InputKey.JUMP === key) {
            width = 5
            height = 5
            buttonX = 6
            buttonY = 6
        }
        if (InputKey.LEFT === key || InputKey.RIGHT === key) {
            buttonY = 5
            height = 7
        } else if (InputKey.FORWARD === key) {
            buttonY = 0
        } else if (InputKey.BACKWARD === key) {
            buttonY = 11
        }
        return KeyButton(buttonX, buttonY, width, height, key, index)
    }

    private fun currentList(): SequencedSeatRuleList {
        return be.getList(currentShaft)
    }

    private inner class TabButton constructor(
        x: Int,
        private val blitY: Int,
        width: Int,
        height: Int,
        private val rotation: Rotation
    ) :
        AbstractSimiWidget(
            guiLeft + x + TAB_PAD_X,
            guiTop + blitY + TAB_PAD_Y,
            width,
            height,
            Component.nullToEmpty(rotation.toString())
        ) {
        private val blitX: Int

        init {
            blitX = x + 205
            withCallback<AbstractSimiWidget>(Runnable {
                updateTab(
                    rotation
                )
            })
        }
//public void renderWidget(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            isHovered =
                rotation == currentShaft || mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height
            graphics.blit(background.location, x, y, if (isHovered) 17 + blitX else blitX, blitY, width, height)
        }
    }

    inner class KeyButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        private val key: InputKey,
        private val index: Int
    ) :
        AbstractSimiWidget(
            guiLeft + x + INPUT_PAD_X,
            guiTop + y + INPUT_PAD_Y + (index - 1) * INPUT_PAD_MARGIN,
            width,
            height,
            Component.nullToEmpty(key.toString())
        ) {
        private val blitX: Int
        private val blitY: Int

        init {
            blitX = x + 205
            blitY = y + 17
            withCallback<AbstractSimiWidget>(
                Runnable {
                    if (isKeySelected(
                            key,
                            index
                        )
                    ) deselectKey(key, index) else selectKey(key, index)
                }
            )
        }

        override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            isHovered =
                isKeySelected(key, index) || mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height
            guiGraphics.blit(background.location, x, y, if (isHovered) 17 + blitX else blitX, blitY, width, height)
        }


    }

    companion object {
        private const val TAB_PAD_X = 11
        private const val TAB_PAD_Y = 16
        private const val INPUT_PAD_X = 11
        private const val INPUT_PAD_Y = 41
        private const val INPUT_PAD_MARGIN = 22
        private const val INPUT_FIELDS_X = 36
        private const val INPUT_FIELDS_Y = 62
        private const val INPUT_FIELDS_WIDTH = 110
        private const val INPUT_FIELDS_HEIGHT = 18
        private const val INPUT_FIELDS_MARGIN = 4
        private const val INPUT_OPERATION_WIDTH = 60
        private const val INPUT_VALUE_WIDTH = 46
    }
}