package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.simibubi.create.foundation.gui.AbstractSimiScreen
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget
import com.simibubi.create.foundation.gui.widget.IconButton
import net.minecraft.client.gui.GuiGraphics
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkPackets

class GyroScreen(private val be: GyroBlockEntity) : AbstractSimiScreen() {
    private val background: ClockworkGuiTextures = ClockworkGuiTextures.GYRO
    private var confirmButton: IconButton? = null

    override fun init() {
        setWindowSize(background.width, background.height)
        super.init()
        val x = guiLeft
        val y = guiTop

        confirmButton = IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM)
        confirmButton!!.withCallback<AbstractSimiWidget>(Runnable { this.onClose() })
        addRenderableWidget(confirmButton!!)
    }

    override fun onClose() {
        super.onClose()
        ClockworkPackets.sendToServer(UpdateGyroPacket(be.blockPos, Vector3d(0.0, 0.0, 0.0)))//TODO fix target vector
    }

    override fun renderWindow(graphics: GuiGraphics?, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val x = guiLeft
        val y = guiTop

        background.render(graphics!!, x, y)
        graphics.drawCenteredString(font, title, x + (background.width - 8) / 2, y + 3, 0xFFFFFF)

    }
}