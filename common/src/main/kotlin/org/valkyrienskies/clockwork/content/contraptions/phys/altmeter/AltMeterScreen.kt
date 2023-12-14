package org.valkyrienskies.clockwork.content.contraptions.phys.altmeter

import com.simibubi.create.foundation.gui.AbstractSimiScreen
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.element.GuiGameElement
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.ScrollInput
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkIconTextures
import org.valkyrienskies.clockwork.ClockworkPackets
import kotlin.math.roundToInt

class AltMeterScreen(private val be: AltMeterBlockEntity) : AbstractSimiScreen() {
    private val background: ClockworkGuiTextures = ClockworkGuiTextures.ALT_METER;
    private var altitudeInput: ScrollInput? = null
    private var confirmButton: IconButton? = null
    private var triggerHeight: Int = be.triggerHeight.roundToInt()

    override fun init() {
        setWindowSize(background.width, background.height)
        super.init()
        val x = guiLeft
        val y = guiTop

        val ruleX = x + 100
        val ruleY = y + 18
        altitudeInput = ScrollInput(
            ruleX + 2,
            ruleY + 2,
            INPUT_VALUE_WIDTH - 4,
            INPUT_FIELDS_HEIGHT - 4
        )

        altitudeInput!!.titled(TRIGGER_HEIGHT_COMPONENT)
        altitudeInput!!.withRange(MIN_HEIGHT, MAX_HEIGHT)
        altitudeInput!!.calling { v: Int -> triggerHeight = v }
        altitudeInput!!.setState(triggerHeight)
        addRenderableWidget(altitudeInput!!)

        confirmButton = IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM)
        confirmButton!!.withCallback<AbstractSimiWidget>(Runnable { onClose() })
        addRenderableWidget(confirmButton!!)
    }


    override fun onClose() {
        super.onClose()
        ClockworkPackets.sendToServer(UpdateAltMeterPacket(triggerHeight.toDouble(), be.blockPos))
    }

    override fun renderWindow(graphics: GuiGraphics?, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val x = guiLeft
        val y = guiTop

        background.render(graphics!!, x, y)
        graphics.drawCenteredString(font, title, x + (background.width - 8) / 2, y + 3, 0xFFFFFF)
        drawRuleList(graphics, x, y, partialTicks)
    }


    private fun drawRuleList(guiGraphics: GuiGraphics, x: Int, y: Int, partialTicks: Float) {
        val ruleX = x + 38 - 7
        val ruleY = y + 18 + 2

        val icon = AllIcons.I_PRIORITY_VERY_HIGH

        val heightStr = triggerHeight.toString()
        val valueComponent: Component = Component.literal("$heightStr m")

        guiGraphics.drawCenteredString(
            font,
            valueComponent,
            ruleX + 62 - 12 + INPUT_VALUE_WIDTH / 2,
            ruleY + (INPUT_FIELDS_HEIGHT - font.lineHeight) / 2 + 1,
            0xFFFFFF
        )
        icon.render(guiGraphics, ruleX + 1, ruleY + 1)

        val nameComponent: Component = TRIGGER_HEIGHT_COMPONENT
        guiGraphics.drawString(
            font,
            nameComponent,
            ruleX + 16,
            ruleY + (INPUT_FIELDS_HEIGHT - font.lineHeight) / 2 + 1,
            0xFFFFFF
        )
    }

    companion object {
        private const val INPUT_FIELDS_HEIGHT = 18
        private const val INPUT_VALUE_WIDTH = 46
        private const val MAX_HEIGHT = 1024
        private const val MIN_HEIGHT = -1024
        private val TRIGGER_HEIGHT_COMPONENT = Component.translatable("alt_meter.trigger_height")
    }
}
