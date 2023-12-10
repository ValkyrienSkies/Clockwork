package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.screen

import com.mojang.blaze3d.systems.RenderSystem
import com.simibubi.create.AllKeys
import com.simibubi.create.foundation.gui.AllGuiTextures
import com.simibubi.create.foundation.utility.Components
import com.simibubi.create.foundation.utility.Lang
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.ToolType
import java.util.function.Consumer
import kotlin.math.max

class GravitronSelectionScreen(tools: List<ToolType>, callback: Consumer<ToolType>) : Screen(Components.literal("Tool Selection")) {
    val scrollToCycle: String = Lang.translateDirect("gui.toolmenu.cycle").string
    val holdToFocus: String = "gui.toolmenu.focusKey"
    protected var tools: List<ToolType>? = null
    protected var callback: Consumer<ToolType>? = null
    var focus: Boolean = false
    private var yOffset = 0f
    protected var selection: Int = 0
    private var initialized = false

    protected var w: Int = 0
    protected var h: Int = 0

    init {
        this.minecraft = Minecraft.getInstance()
        this.tools = tools
        this.callback = callback
        focus = false
        yOffset = 0f
        selection = 0
        initialized = false

        callback.accept(tools[selection])

        w = max((tools.size * 50 + 30).toDouble(), 220.0).toInt()
        h = 30
    }

    fun setSelectedElement(tool: ToolType?) {
        if (!tools!!.contains(tool!!)) return
        selection = tools!!.indexOf(tool)
    }

    fun cycle(direction: Int) {
        selection += if ((direction < 0)) 1 else -1
        selection = (selection + tools!!.size) % tools!!.size
    }

    private fun draw(graphics: GuiGraphics, partialTicks: Float) {
        val matrixStack = graphics.pose()
        val mainWindow = minecraft!!.window
        if (!initialized) init(minecraft, mainWindow.guiScaledWidth, mainWindow.guiScaledHeight)

        var x = (mainWindow.guiScaledWidth - w) / 2 + 15
        val y = mainWindow.guiScaledHeight - h - 75

        matrixStack.pushPose()
        matrixStack.translate(0f, -yOffset, (if (focus) 100 else 0).toFloat())

        val gray = AllGuiTextures.HUD_BACKGROUND
        RenderSystem.enableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, if (focus) 7 / 8f else 1 / 2f)

        graphics.blit(
            gray.location,
            x - 15,
            y,
            gray.startX.toFloat(),
            gray.startY.toFloat(),
            w,
            h,
            gray.width,
            gray.height
        )

        val toolTipAlpha = yOffset / 10
        val toolTip = tools!![selection]
            .description
        val stringAlphaComponent = ((toolTipAlpha * 0xFF).toInt()) shl 24

        if (toolTipAlpha > 0.25f) {
            RenderSystem.setShaderColor(.7f, .7f, .8f, toolTipAlpha)
            graphics.blit(
                gray.location,
                x - 15,
                y + 33,
                gray.startX.toFloat(),
                gray.startY.toFloat(),
                w,
                h + 22,
                gray.width,
                gray.height
            )
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            if (toolTip.size > 0) graphics.drawString(
                font,
                toolTip[0], x - 10, y + 38, 0xEEEEEE + stringAlphaComponent, false
            )
            if (toolTip.size > 1) graphics.drawString(
                font,
                toolTip[1], x - 10, y + 50, 0xCCDDFF + stringAlphaComponent, false
            )
            if (toolTip.size > 2) graphics.drawString(
                font,
                toolTip[2], x - 10, y + 60, 0xCCDDFF + stringAlphaComponent, false
            )
            if (toolTip.size > 3) graphics.drawString(
                font,
                toolTip[3], x - 10, y + 72, 0xCCCCDD + stringAlphaComponent, false
            )
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        if (tools!!.size > 1) {
            val keyName = AllKeys.TOOL_MENU.boundKey
            val width = minecraft!!.window
                .guiScaledWidth
            if (!focus) graphics.drawCenteredString(
                minecraft!!.font, Lang.translateDirect(holdToFocus, keyName), width / 2,
                y - 10, 0xCCDDFF
            )
            else graphics.drawCenteredString(minecraft!!.font, scrollToCycle, width / 2, y - 10, 0xCCDDFF)
        } else {
            x += 65
        }


        for (i in tools!!.indices) {
            RenderSystem.enableBlend()
            matrixStack.pushPose()

            var alpha = if (focus) 1f else .2f
            if (i == selection) {
                matrixStack.translate(0f, -10f, 0f)
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                graphics.drawCenteredString(
                    minecraft!!.font, tools!![i]
                        .displayName
                        .string, x + i * 50 + 24, y + 28, 0xCCDDFF
                )
                alpha = 1f
            }
            RenderSystem.setShaderColor(0f, 0f, 0f, alpha)
            tools!![i]
                .icon
                .render(graphics, x + i * 50 + 16, y + 12)
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha)
            tools!![i]
                .icon
                .render(graphics, x + i * 50 + 16, y + 11)

            matrixStack.popPose()
        }

        RenderSystem.disableBlend()
        matrixStack.popPose()
    }

    fun update() {
        if (focus) yOffset += (10 - yOffset) * .1f
        else yOffset *= .9f
    }

    fun renderPassive(graphics: GuiGraphics, partialTicks: Float) {
        draw(graphics, partialTicks)
    }

    override fun onClose() {
        callback!!.accept(tools!![selection])
    }

    override fun init() {
        super.init()
        initialized = true
    }
}