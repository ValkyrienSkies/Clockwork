package org.valkyrienskies.clockwork.content.curiosities.altmeter

import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.ScrollInput
import kotlinx.coroutines.Runnable
import net.createmod.catnip.gui.AbstractSimiScreen
import net.createmod.catnip.gui.widget.AbstractSimiWidget
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.altmeter.AltMeterBlockEntity.AltMeterDirection
import org.valkyrienskies.clockwork.content.curiosities.altmeter.AltMeterBlockEntity.AltMeterDirection.*

class AltMeterScreen(private val blockEntity: AltMeterBlockEntity) :
    AbstractSimiScreen(Component.translatable("alt_meter.title")) {

    private val background: ClockworkGuiTextures = ClockworkGuiTextures.ALT_METER;

    private var triggerHeight: Int = blockEntity.triggerHeight
    private var triggerSensitivity: Int = blockEntity.triggerSensitivity
    private var triggerDirection: AltMeterDirection = blockEntity.triggerDirection
    private var doSave: Boolean = false


    override fun init() {
        setWindowSize(background.width, background.height)
        super.init()
        initControls()
    }

    private fun initControls() {

        //triggerHeight
        ScrollInput(
            guiLeft + 88, guiTop + 20,
            INPUT_VALUE_WIDTH - 4, INPUT_FIELDS_HEIGHT - 4
        ).apply {
            titled(TRIGGER_HEIGHT_COMPONENT)
            withRange(MIN_HEIGHT, MAX_HEIGHT)
            calling { v: Int -> triggerHeight = v }
            setState(triggerHeight)
            addRenderableWidget(this)
        }

        //triggerSensitivity
        ScrollInput(
            guiLeft + 88, guiTop + 49,
            INPUT_VALUE_WIDTH, INPUT_FIELDS_HEIGHT
        ).apply {
            titled(TRIGGER_SENSITIVITY_COMPONENT)
            withRange(1, MAX_HEIGHT)
            calling { v: Int -> triggerSensitivity = v }
            setState(triggerSensitivity)
            addRenderableWidget(this)
        }

        //triggerDirectionInput
        DirectionButton(
            guiLeft + 95, guiTop + 80,
            INPUT_VALUE_WIDTH, INPUT_FIELDS_HEIGHT
        ).apply {
            titled(TRIGGER_DIRECTION_COMPONENT)
            withValue(Component.literal(triggerDirection.name))
            calling {
                triggerDirection = nextDirection(triggerDirection)
                withValue(Component.literal(triggerDirection.name))
            }
            addRenderableWidget(this)
        }

        //confirm
        IconButton(
            guiLeft + background.width - 33,
            guiTop + background.height - 24,
            AllIcons.I_CONFIRM
        ).apply {
            withCallback<AbstractSimiWidget>{
                doSave = true
                onClose()
            }
            addRenderableWidget(this)
        }

        //cancel
        IconButton(
            guiLeft + background.width - 62,
            guiTop + background.height - 24,
            AllIcons.I_DISABLE
        ).apply {
            withCallback<AbstractSimiWidget>(::onClose)
            addRenderableWidget(this)
        }

    }

    private fun nextDirection(dir: AltMeterDirection): AltMeterDirection {
        return with(AltMeterDirection.entries) {
            elementAt((indexOf(dir) + 1) % size)
        }
    }

    override fun onClose() {
        super.onClose()
        if (doSave)
            ClockworkPackets.sendToServer(
            UpdateAltMeterPacket(
                triggerHeight,
                triggerSensitivity,
                triggerDirection,
                blockEntity.blockPos
            ))
    }

    override fun renderWindow(poseStack: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        background.render(poseStack, guiLeft, guiTop)
        poseStack.drawCenteredString(font, title, guiLeft + (background.width - 8) / 2, guiTop + 3, 0xFFFFFF)
        drawProperties(poseStack)
    }

    private fun drawProperties(poseStack: GuiGraphics) {
        drawPropertyName(poseStack, 0, TRIGGER_HEIGHT_COMPONENT)
        drawPropertyValue(poseStack, 0, Component.literal("$triggerHeight m"))
        drawPropertyName(poseStack, 1, TRIGGER_SENSITIVITY_COMPONENT)
        drawPropertyValue(poseStack, 1, Component.literal("$triggerSensitivity m"))
        drawPropertyName(poseStack, 2, TRIGGER_DIRECTION_COMPONENT)
        drawPropertyValue(poseStack, 2, TRIGGER_DIRECTION_ICONS[triggerDirection]!!)
    }

    private fun drawPropertyName(poseStack: GuiGraphics, lineNumber: Int, line: Component) {
        poseStack.drawString(font, line, guiLeft + 11, guiTop + 25 + (29 * lineNumber), 0xFFFFFF)
    }

    private fun drawPropertyValue(poseStack: GuiGraphics, lineNumber: Int, value: Component) {
        poseStack.drawCenteredString(font, value, guiLeft + 104, guiTop + 25 + (29 * lineNumber), 0xFFFFFF)
    }

    private fun drawPropertyValue(poseStack: GuiGraphics, lineNumber: Int, icon: AllIcons) {
        icon.render(poseStack, guiLeft + 96, guiTop + 21 + (29 * lineNumber))
    }

    companion object {
        private const val INPUT_FIELDS_HEIGHT = 18
        private const val INPUT_VALUE_WIDTH = 36
        private const val MAX_HEIGHT = 1024
        private const val MIN_HEIGHT = -1024
        private val TRIGGER_HEIGHT_COMPONENT = Component.translatable("alt_meter.trigger_height")
        private val TRIGGER_SENSITIVITY_COMPONENT = Component.translatable("alt_meter.trigger_sensitivity")
        private val TRIGGER_DIRECTION_COMPONENT = Component.translatable("alt_meter.trigger_direction")
        private val TRIGGER_DIRECTION_ICONS = mapOf<AltMeterDirection, AllIcons>(
            UP to AllIcons.I_PRIORITY_HIGH,
            DOWN to AllIcons.I_PRIORITY_LOW,
            BOTH to AllIcons.I_FLIP
        )
    }

    private class DirectionButton(
        x: Int, y: Int, width: Int, height: Int,
        var title: Component = Component.empty(),
        var value: Component = Component.empty(),
        val hint: MutableComponent = Component.literal("Click to modify")
    ): AbstractSimiWidget(x, y, width, height) {

        private var calling: Runnable? = null

        fun calling(callback: Runnable) { calling = callback }

        override fun onClick(mouseX: Double, mouseY: Double) { calling?.run() }

        fun titled(title: MutableComponent) {
            this.title = title
            updateTooltip()
        }

        fun withValue(value: MutableComponent) {
            this.value = value
            updateTooltip()
        }

        fun updateTooltip() {
            toolTip.clear()
            toolTip.add(
                title.plainCopy()
                    .withStyle({ s: Style? -> s!!.withColor(HEADER_RGB.rgb) })
            )
            toolTip.add(
                value.plainCopy()
                    .withStyle({ s: Style? -> s!!.withColor(HINT_RGB.rgb)})
            )
            toolTip.add(
                hint.plainCopy()
                    .withStyle({ s: Style? -> s!!.withColor(ChatFormatting.DARK_GRAY) })
            )
        }
    }
}
