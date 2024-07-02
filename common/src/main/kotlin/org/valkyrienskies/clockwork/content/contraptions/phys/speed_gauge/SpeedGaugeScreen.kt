package org.valkyrienskies.clockwork.content.contraptions.phys.speed_gauge

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.datafixers.TypeRewriteRule.All
import com.simibubi.create.foundation.gui.AbstractSimiScreen
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.ScrollInput
import com.simibubi.create.foundation.utility.Components
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextComponent
import net.minecraft.network.chat.TranslatableComponent
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkPackets
import kotlin.math.roundToInt

class SpeedGaugeScreen(private val be: SpeedGaugeBlockEntity) : AbstractSimiScreen() {
    private val background: ClockworkGuiTextures = ClockworkGuiTextures.ALT_METER;
    private var speedInput: ScrollInput? = null
    private var confirmButton: IconButton? = null
    private var moreThanButton: IconButton? = null
    private var triggerSpeed: Int = be.triggerSpeed.roundToInt()
    private var moreThan: Boolean = be.moreThan

    override fun init() {
        setWindowSize(background.width, background.height)
        super.init()
        val x = guiLeft
        val y = guiTop

        val ruleX = x + 75
        val ruleY = y + 18
        speedInput = ScrollInput(
            ruleX + 2,
            ruleY + 2,
            INPUT_VALUE_WIDTH - 4,
            INPUT_FIELDS_HEIGHT - 4
        )

        speedInput!!.titled(TRIGGER_SPEED_COMPONENT)
        speedInput!!.withRange(MIN_SPEED, MAX_SPEED)
        speedInput!!.calling { v: Int -> triggerSpeed = v }
        speedInput!!.setState(triggerSpeed)
        addRenderableWidget(speedInput!!)

        confirmButton = IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM)
        confirmButton!!.withCallback<AbstractSimiWidget>(Runnable { onClose() })
        addRenderableWidget(confirmButton!!)

        moreThanButton = IconButton(x + 125, y + 20, getMoreThanIcon())
        moreThanButton!!.withCallback<AbstractSimiWidget>(Runnable { onButton() })
        addRenderableWidget(moreThanButton!!)
    }


    override fun onClose() {
        super.onClose()
        ClockworkPackets.sendToServer(UpdateSpeedGaugePacket(triggerSpeed.toDouble(), moreThan, be.blockPos))
    }

    fun getMoreThanIcon(): AllIcons {
        if (moreThan) return AllIcons.I_MTD_RIGHT
        return AllIcons.I_MTD_LEFT
    }

    fun onButton() {
        moreThan = !moreThan
        moreThanButton?.setIcon(getMoreThanIcon())

    }

    override fun renderWindow(poseStack: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val x = guiLeft
        val y = guiTop

        background.render(poseStack, x, y)
        drawCenteredString(poseStack, font, title, x + (background.width - 8) / 2, y + 3, 0xFFFFFF)
        drawRuleList(poseStack, x, y, partialTicks)
    }


    private fun drawRuleList(poseStack: PoseStack, x: Int, y: Int, partialTicks: Float) {
        val ruleX = x + 38 - 7
        val ruleY = y + 18 + 2

        val icon = AllIcons.I_PRIORITY_VERY_HIGH

        val speedStr = triggerSpeed.toString()
        val valueComponent: Component = TextComponent("$speedStr m/s")

        drawCenteredString(poseStack,
            font,
            valueComponent,
            ruleX + 62 - 12 + INPUT_VALUE_WIDTH / 2,
            ruleY + (INPUT_FIELDS_HEIGHT - font.lineHeight) / 2 + 1,
            0xFFFFFF
        )
        icon.render(poseStack, ruleX + 1, ruleY + 1)

        val nameComponent: Component = TRIGGER_SPEED_COMPONENT
        drawString(poseStack,
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
        private const val MAX_SPEED = 1024
        private const val MIN_SPEED = 0
        private val TRIGGER_SPEED_COMPONENT = Components.literal("Speed")
    }
}
