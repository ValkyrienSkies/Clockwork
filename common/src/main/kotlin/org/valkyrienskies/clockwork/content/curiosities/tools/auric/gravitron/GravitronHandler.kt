package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron

import com.google.common.collect.ImmutableList
import com.mojang.blaze3d.platform.Window
import com.simibubi.create.AllBlocks
import com.simibubi.create.AllKeys
import com.simibubi.create.content.schematics.client.SchematicHandler
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.screen.GravitronSelectionScreen
import java.util.*

open class GravitronHandler {

    var selectionScreen: GravitronSelectionScreen? = null
    var currentTool: ToolType? = null
    val active = false

    fun GravitronHandler() {
        currentTool = ToolType.GRAB
        selectionScreen = GravitronSelectionScreen(ImmutableList.of(ToolType.GRAB), this::equip)
    }

    fun renderOverlay(graphics: GuiGraphics?, partialTicks: Float, window: Window) {
        if (Minecraft.getInstance().options.hideGui || !active) return

        currentTool?.tool?.renderOverlay(graphics, partialTicks, window.guiScaledWidth, window.guiScaledHeight)
        selectionScreen?.renderPassive(graphics!!, partialTicks)
    }

    fun equip(tool: ToolType?) {
        this.currentTool = tool
        currentTool?.tool
            ?.init()
    }

    fun onMouseInput(button: Int, pressed: Boolean): Boolean {

        if (!active) return false
        if (!pressed || button != 1) return false
        val mc = Minecraft.getInstance()
        if (mc.player!!.isShiftKeyDown) return false
        if (mc.hitResult is BlockHitResult) {
            val blockRayTraceResult = mc.hitResult as BlockHitResult?
            val clickedBlock = mc.level!!.getBlockState(blockRayTraceResult!!.blockPos)
            if (AllBlocks.SCHEMATICANNON.has(clickedBlock)) return false
            if (AllBlocks.DEPLOYER.has(clickedBlock)) return false
        }
        return currentTool!!.tool
            .handleRightClick()
    }

    fun onKeyInput(key: Int, pressed: Boolean) {
        if (!active) return
        if (key != AllKeys.TOOL_MENU.boundCode) return

        if (pressed && !selectionScreen!!.focused) selectionScreen!!.focused = true
        if (!pressed && selectionScreen!!.focused) {
            selectionScreen!!.focused = false
            selectionScreen!!.onClose()
        }
    }

    fun mouseScrolled(delta: Double): Boolean {
        if (!active) return false

        if (selectionScreen!!.focused) {
            selectionScreen!!.cycle(delta.toInt())
            return true
        }
        if (AllKeys.ctrlDown()) return currentTool!!.tool
            .handleMouseWheel(delta)
        return false
    }
}