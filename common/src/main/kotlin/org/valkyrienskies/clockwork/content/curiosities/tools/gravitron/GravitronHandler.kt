package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.simibubi.create.AllKeys
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.ToolType
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool.ToolType.Companion.getTools

open class GravitronHandler {
    var selectionScreen: GravitronSelectionScreen? = null
    var active: Boolean = false
    var currentTool: ToolType? = null
    var activeHotbarSlot: Int = 0
    var activeSchematicItem: ItemStack? = null
    var overlay: GravitronHotbarSlotOverlay? = null
    var isRegular = true

    init {
        overlay = GravitronHotbarSlotOverlay()
        currentTool = ToolType.GRAB
        selectionScreen = GravitronSelectionScreen(
            getTools()
        ) { tool: ToolType? ->
            this.equip(
                tool
            )
        }
    }

    fun tick() {
        val mc = Minecraft.getInstance()
        if (mc.gameMode != null && mc.gameMode!!.playerMode == GameType.SPECTATOR) {
            if (active) {
                active = false
                activeHotbarSlot = 0
                activeSchematicItem = null
            }
            return
        }
        val player = mc.player
        val stack = findGravitronInHand(player)
        if (stack == null) {
            active = false
            if (activeSchematicItem != null && itemLost(player!!)) {
                activeHotbarSlot = 0
                activeSchematicItem = null
            }
            return
        }
        init(player)
        if (!active) {
            return
        }

        selectionScreen!!.update()
    }

    fun render(graphics: GuiGraphics, partialTicks: Float, width: Int, height: Int) {
        if (Minecraft.getInstance().options.hideGui || !active || isRegular) {
            return
        }
        if (activeSchematicItem != null) {
            overlay!!.renderOn(graphics, activeHotbarSlot)
        }

        currentTool!!.tool.renderOverlay(graphics, partialTicks, width, height)
        selectionScreen!!.renderPassive(graphics, partialTicks)
    }

    private fun init(player: LocalPlayer?) {
        active = true
    }

    private fun itemLost(player: Player): Boolean {
        for (i in 0 until Inventory.getSelectionSize()) {
            val bl = player.inventory.getItem(i).`is`(ClockworkItems.GRAVITRON.get().asItem())
            val bl2 = player.inventory.getItem(i).`is`(ClockworkItems.CREATIVE_GRAVITRON.get().asItem())
            if (!bl && !bl2) {
                continue
            }
            return false
        }
        return true
    }

    fun equip(tool: ToolType?) {
        this.currentTool = tool
        currentTool!!.tool.init()
    }

    private fun findGravitronInHand(player: Player?): ItemStack? {
        val stack = player!!.mainHandItem
        if (!ClockworkItems.GRAVITRON.isIn(stack) && !ClockworkItems.CREATIVE_GRAVITRON.isIn(stack)) {
            return null
        }

        isRegular = ClockworkItems.GRAVITRON.isIn(stack)

        activeSchematicItem = stack
        activeHotbarSlot = player.inventory.selected
        return stack
    }

    fun onMouseInput(button: Int, pressed: Boolean): Boolean {
        if (!active) {
            return false
        }
        if (!pressed || button != 1) {
            return false
        }
        val mc = Minecraft.getInstance()
        if (mc.player!!.isShiftKeyDown) {
            return false
        }
        return currentTool!!.tool.handleRightClick()
    }

    fun onKeyInput(key: Int, pressed: Boolean) {
        if (!active) {
            return
        }
        if (key != AllKeys.TOOL_MENU.boundCode) {
            return
        }

        if (pressed && !selectionScreen!!.focus) {
            selectionScreen!!.focus = true
        }
        if (!pressed && selectionScreen!!.focus) {
            selectionScreen!!.focus = false
            selectionScreen!!.onClose()
        }
    }

    fun mouseScrolled(delta: Double): Boolean {
        if (!active) {
            return false
        }
        if (isRegular) {
            return false
        }

        if (selectionScreen!!.focus) {
            selectionScreen!!.cycle(delta.toInt())
            return true
        }
        if (AllKeys.ctrlDown()) {
            return currentTool!!.tool.handleMouseWheel(delta)
        }
        return false
    }

    fun init() {
    }
}