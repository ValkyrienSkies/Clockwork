package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.AbstractSimiScreen
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget
import com.simibubi.create.foundation.gui.widget.IconButton
import net.minecraft.util.Mth
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkIcons
import org.valkyrienskies.clockwork.ClockworkPackets

class GyroScreen(private val be: GyroBlockEntity) : AbstractSimiScreen() {
    private val background: ClockworkGuiTextures = ClockworkGuiTextures.GYRO
    private var confirmButton: IconButton? = null
    private var targetVec = Vector3d(0.0, 1.0, 0.0)
    var buttonStateArray = BooleanArray(9)

    override fun init() {
        setWindowSize(background.width, background.height)
        super.init()
        val x = guiLeft
        val y = guiTop

        makeTargetButtons(x, y)

        confirmButton = IconButton(x + background.width - 33, y + background.height - 24, AllIcons.I_CONFIRM)
        confirmButton!!.withCallback<AbstractSimiWidget>(Runnable { this.onClose() })
        addRenderableWidget(confirmButton!!)
    }

    private fun makeTargetButtons(x: Int, y: Int) {
        //Horizontal 5 buttons: -90, -45, 0, 45, 90

        for (i in 0 until 5) {
            var button = IconButton(x + background.width - 149 + (i * 18), y + background.height - 121,
                if (i == 0 || i == 4) ClockworkIcons.NINETY else if (i == 2) AllIcons.I_TARGET else ClockworkIcons.FORTY_FIVE
            )
            button.withCallback<AbstractSimiWidget>(Runnable {
                buttonStateArray[i] = !buttonStateArray[i]
                button.active = !button.active
            })
            addRenderableWidget(button)
        }
        //Vertical 4 buttons, -90, -45, 45, 90
        for (i in 5 until 9) {
            var e = 0
            if (i > 6) {
                e = 18
            }
            var button = IconButton(x + background.width - 113, y + background.height - 175 + ((i - 4) * 18) + e,
                if (i == 5 || i == 8) ClockworkIcons.NINETY else ClockworkIcons.FORTY_FIVE
            )
            button.withCallback<AbstractSimiWidget>(Runnable {
                buttonStateArray[i] = !buttonStateArray[i]
                button.active = !button.active
            })
            addRenderableWidget(button)
        }
    }

    override fun onClose() {
        super.onClose()
        var xm90 = buttonStateArray.get(0)
        var xm45 = buttonStateArray.get(1)
        var x0 = buttonStateArray.get(2)
        var x45 = buttonStateArray.get(3)
        var x90 = buttonStateArray.get(4)

        var y90 = buttonStateArray.get(5)
        var y45 = buttonStateArray.get(6)
        var ym45 = buttonStateArray.get(7)
        var ym90 = buttonStateArray.get(8)

        var x =
            (if (x90) 1.0 else 0.0) + (if (xm90) -1.0 else 0.0) + (if (x45) 0.5 else 0.0) + (if (xm45) -0.5 else 0.0)
        var y = if (x0) 1.0 else 0.0
        var z =
            (if (y90) 1.0 else 0.0) + (if (ym90) -1.0 else 0.0) + (if (y45) 0.5 else 0.0) + (if (ym45) -0.5 else 0.0)


        targetVec = Vector3d(Mth.clamp(x, -1.0, 1.0), Mth.clamp(y, -1.0, 1.0), Mth.clamp(z, -1.0, 1.0))

        ClockworkPackets.sendToServer(UpdateGyroPacket(be.blockPos, targetVec))
        be.targetVec3 = targetVec
        be.notifyUpdate()
    }

    override fun renderWindow(poseStack: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val x = guiLeft
        val y = guiTop

        background.render(poseStack, x, y)
        drawCenteredString(poseStack, font, title, x + (background.width - 8) / 2, y + 3, 0xFFFFFF)

    }
}