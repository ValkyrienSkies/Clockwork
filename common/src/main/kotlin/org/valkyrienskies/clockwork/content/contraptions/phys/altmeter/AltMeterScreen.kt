package org.valkyrienskies.clockwork.content.contraptions.phys.altmeter

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.AbstractSimiScreen
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.element.GuiGameElement
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget
import com.simibubi.create.foundation.gui.widget.IconButton
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkPackets

class AltMeterScreen(private val be: AltMeterBlockEntity) : AbstractSimiScreen() {
    private val renderedItem: ItemStack = ClockworkBlocks.COMMAND_SEAT.asStack()
    private val background: ClockworkGuiTextures = ClockworkGuiTextures.COMMAND_SEAT
    private var confirmButton: IconButton? = null

    override fun init() {
        setWindowSize(background.width, background.height)
        setWindowOffset(-20, 0)
        super.init()
        val x = guiLeft
        val y = guiTop
        confirmButton = IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM)
        confirmButton!!.withCallback<AbstractSimiWidget>(Runnable { onClose() })
        addRenderableWidget(confirmButton!!)
    }

    override fun onClose() {
        super.onClose()
        ClockworkPackets.sendToServer(UpdateAltMeterPacket(be.triggerHeight, be.blockPos))
    }

    override fun renderWindow(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val x = guiLeft
        val y = guiTop
        background.render(ms, x, y, this)
        drawCenteredString(ms, font, title, x + (background.width - 8) / 2, y + 3, 0xFFFFFF)
        // drawRuleList(ms, x, y, partialTicks)
        GuiGameElement.of(renderedItem)
            .at<GuiGameElement.GuiRenderBuilder>((x + background.width + 6).toFloat(),
                (y + background.height - 56).toFloat(), -200f)
            .scale(5.0)
            .render(ms)
    }
}
