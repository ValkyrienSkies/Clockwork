package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllKeys
import com.simibubi.create.foundation.utility.Components
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.ToolType
import java.util.function.Consumer
import kotlin.math.max

class WanderwandSelectionScreen(private val tools: List<ToolType>, private val callback: Consumer<ToolType>) :
    Screen(Components.literal("Tool Selection")) {
    val holdToFocus: String = "gui.toolmenu.focusKey"

    var focus: Boolean = false
    private var yOffset = 0f
    protected var selection: Int = 0
    private var initialized = false

    protected var w: Int = 0
    protected var h: Int = 0

    init {
        this.minecraft = Minecraft.getInstance()

        focus = false
        yOffset = 0f
        selection = 0
        initialized = false

        callback.accept(tools[selection])

        w = max((tools.size * 50 + 30).toDouble(), 220.0).toInt()
        h = 30
    }

    fun cycle(direction: Int) {
        selection += if ((direction < 0)) 1 else -1
        selection = (selection + tools.size) % tools.size
    }

    private fun draw(poseStack: PoseStack, partialTicks: Float) {
        val mainWindow = minecraft!!.window
        if (!initialized) {
            init(minecraft!!, mainWindow.guiScaledWidth, mainWindow.guiScaledHeight)
        }

        val x = (mainWindow.guiScaledWidth - w) / 2 + 15
        val y = mainWindow.guiScaledHeight - h - 75

        poseStack.pushPose()
        poseStack.translate(0.0, 20.0 - yOffset, (if (focus) 100.0 else 0.0))


        val gray = ClockworkGuiTextures.WANDER_TOOL_BACKGROUND
        RenderSystem.enableBlend()
        RenderSystem.setShaderColor(1f, 1f, 1f, if (focus) 7 / 8f else 1 / 2f)
        RenderSystem.setShaderTexture(0, gray.location)
        blit(poseStack,
            x - 15,
            y,
            gray.startX.toFloat(),
            gray.startY.toFloat(),
            w,
            h,
            gray.width,
            gray.height
        )

        val toolTipAlpha = yOffset / 40
        val toolTip: List<Component> = tools[selection].getDescription()
        val stringAlphaComponent = ((toolTipAlpha * 0xFF).toInt()) shl 24

        if (toolTipAlpha > 0.25f) {
            RenderSystem.setShaderColor(.7f, .7f, .8f, toolTipAlpha)

            RenderSystem.setShaderTexture(0, gray.location)
            blit(
                poseStack,
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

            if (toolTip.size > 0) {
                drawString(poseStack, font, toolTip[0], x - 10, y + 38, 0xEEEEEE + stringAlphaComponent)
            }
            if (toolTip.size > 1) {
                drawString(poseStack, font, toolTip[1], x - 10, y + 50, 0xDDCCFF + stringAlphaComponent)
            }
            if (toolTip.size > 2) {
                drawString(poseStack, font, toolTip[2], x - 10, y + 60, 0xDDCCFF + stringAlphaComponent)
            }
            if (toolTip.size > 3) {
                drawString(poseStack, font, toolTip[3], x - 10, y + 72, 0xCCCCDD + stringAlphaComponent)
            }
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        val keyName = AllKeys.TOOL_MENU.boundKey
        val width = minecraft!!.window.guiScaledWidth
        if (!focus) {
            drawCenteredString(poseStack,
                minecraft!!.font,
                ClockworkLang.translateDirect(holdToFocus, keyName),
                width / 2,
                y - 10,
                0xDDCCFF
            )
        } else {
            drawCenteredString(poseStack,
                minecraft!!.font,
                ClockworkLang.translateDirect("gui.toolmenu.cycle"),
                width / 2,
                y - 10,
                0xDDCCFF
            )
        }

        for (i in tools.indices) {
            RenderSystem.enableBlend()
            poseStack.pushPose()

            var alpha = if (focus) 1f else .2f
            if (i == selection) {
                poseStack.translate(0.0, -10.0, 0.0)
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                drawCenteredString(poseStack,
                    minecraft!!.font,
                    tools[i].getDisplayName().getString(),
                    x + i * 50 + 24,
                    y + 28,
                    0xDDCCFF
                )
                alpha = 1f
            }
            RenderSystem.setShaderColor(0f, 0f, 0f, alpha)
            tools[i].icon.render(poseStack, x + i * 50 + 16, y + 12)
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha)
            tools[i].icon.render(poseStack, x + i * 50 + 16, y + 11)

            poseStack.popPose()
        }

        RenderSystem.disableBlend()
        poseStack.popPose()
    }

    fun update() {
        if (focus) {
            yOffset += (40 - yOffset) * .1f
        } else {
            yOffset *= .9f
        }
    }

    fun renderPassive(poseStack: PoseStack, partialTicks: Float) {
        draw(poseStack, partialTicks)
    }

    override fun onClose() {
        callback.accept(tools[selection])
    }

    override fun init() {
        super.init()
        initialized = true
    }
}