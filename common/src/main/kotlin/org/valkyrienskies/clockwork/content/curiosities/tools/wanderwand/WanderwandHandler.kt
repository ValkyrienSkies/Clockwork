package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllKeys
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.ToolType
import org.valkyrienskies.clockwork.util.ClockworkHotbarSlotOverlays

open class WanderwandHandler {
    var selectionScreen: WanderwandSelectionScreen? = null
    var active: Boolean = false
    var currentTool: ToolType? = null
    var activeHotbarSlot: Int = 0
    var activeSchematicItem: ItemStack? = null
    var overlay: ClockworkHotbarSlotOverlays? = null
    var isRegular = true

    init {
        overlay = ClockworkHotbarSlotOverlays()
        currentTool = ToolType.SELECT
        selectionScreen = WanderwandSelectionScreen(
            ToolType.getTools()
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
        val stack = findWandInHand(player)
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

    private fun init(player: LocalPlayer?) {
        active = true
    }

    fun render(poseStack: GuiGraphics, partialTicks: Float, width: Int, height: Int) {
        if (Minecraft.getInstance().options.hideGui || !active) {
            return
        }
        if (activeSchematicItem != null && isRegular) {
            overlay!!.renderWanderlite(poseStack, activeHotbarSlot, partialTicks)
        }

        currentTool!!.tool.renderOverlay(poseStack, partialTicks, width, height)
        selectionScreen!!.renderPassive(poseStack, partialTicks)
    }

    private fun itemLost(player: Player): Boolean {
        for (i in 0 until Inventory.getSelectionSize()) {
            val bl = player.inventory.getItem(i).`is`(ClockworkItems.WANDERWAND.get().asItem())
            if (!bl) {
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

    fun findWandInHand(player: Player?): ItemStack? {
        if (player == null) {
            return null
        }
        val stack = player.mainHandItem
        if (!ClockworkItems.WANDERWAND.isIn(stack)) {
            return null
        }

        isRegular = ClockworkItems.WANDERWAND.isIn(stack)

        activeSchematicItem = stack
        activeHotbarSlot = player.inventory.selected
        return stack
    }

    fun onMouseInput(button: Int, pressed: Boolean): Boolean {
        if (!active) {
            return false
        }
        if (!pressed || (button != 1 && button != 0)) {
            return false
        }
        val mc = Minecraft.getInstance()
        if (mc.player!!.isShiftKeyDown && currentTool != ToolType.DESELECT) {
            return false
        }
        if (button == 0) {
            return currentTool!!.tool.handleLeftClick()
        }
        return currentTool!!.tool.handleRightClick(mc.player!!.isCrouching)
    }

    fun onKeyInput(key: Int, pressed: Boolean) {
        if (!active) {
            return
        }
        if (!AllKeys.TOOL_MENU.doesModifierAndCodeMatch(key)) {
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
