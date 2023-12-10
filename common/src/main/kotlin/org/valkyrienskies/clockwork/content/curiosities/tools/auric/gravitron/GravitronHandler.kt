package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron

import com.google.common.collect.ImmutableList
import com.mojang.blaze3d.platform.Window
import com.simibubi.create.AllKeys
import com.simibubi.create.content.schematics.client.SchematicHotbarSlotOverlay
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.screen.GravitronSelectionScreen

open class GravitronHandler {

    @kotlin.jvm.JvmField
    var selectionScreen: GravitronSelectionScreen?
    @kotlin.jvm.JvmField
    var currentTool: ToolType? = null
    @kotlin.jvm.JvmField
    public var active = true
    @kotlin.jvm.JvmField
    var activeHotbarSlot = 0
    @kotlin.jvm.JvmField
    var activeSchematicItem: ItemStack? = null
    @kotlin.jvm.JvmField
    var overlay: SchematicHotbarSlotOverlay?

    init {
        currentTool = ToolType.GRAB
        selectionScreen = GravitronSelectionScreen(ImmutableList.of(ToolType.GRAB), this::equip)
        overlay = SchematicHotbarSlotOverlay()
    }

    fun tick() {
        val mc = Minecraft.getInstance()
        if (mc.gameMode!!.playerMode == GameType.SPECTATOR) {
            if (active) {
                active = false
                activeHotbarSlot = 0
                activeSchematicItem = null
            }
            return
        }
println(selectionScreen)
        val player = mc.player
        val stack = findBlueprintInHand(player!!)
        if (stack == null) {
            active = false
            if (activeSchematicItem != null && itemLost(player)) {
                activeHotbarSlot = 0
                activeSchematicItem = null
            }
            return
        }

        if (!active) return

        selectionScreen!!.update()
    }

    private fun itemLost(player: Player): Boolean {
        for (i in 0 until Inventory.getSelectionSize()) {
            if (!ItemStack.matches(
                    player.inventory
                        .getItem(i), activeSchematicItem
                )
            ) continue
            return false
        }
        return true
    }

    fun renderOverlay(graphics: GuiGraphics?, partialTicks: Float, window: Window) {
        println("In")
        if (Minecraft.getInstance().options.hideGui || !active) return
        println("O")
        currentTool?.tool?.renderOverlay(graphics, partialTicks, window.guiScaledWidth, window.guiScaledHeight)
        selectionScreen?.renderPassive(graphics!!, partialTicks)
    }

    fun equip(tool: ToolType?) {
        this.currentTool = tool
        currentTool?.tool
            ?.init()
    }

    private fun findBlueprintInHand(player: Player): ItemStack? {
        val stack = player.mainHandItem
        if (!ClockworkItems.GRAVITRON.isIn(stack)) return null
        activeSchematicItem = stack
        activeHotbarSlot = player.inventory.selected
        return stack
    }

    fun onMouseInput(button: Int, pressed: Boolean): Boolean {

        if (!active) return false
        if (!pressed || button != 1) return false
        val mc = Minecraft.getInstance()
        if (mc.player!!.isShiftKeyDown) return false

        return currentTool!!.tool.handleRightClick()
    }

    fun onKeyInput(key: Int, pressed: Boolean) {
        if (!active) return
        if (key != AllKeys.TOOL_MENU.boundCode) return

        if (pressed && !selectionScreen!!.focus) selectionScreen!!.focus = true
        if (!pressed && selectionScreen!!.focus) {
            selectionScreen!!.focus = false
            selectionScreen!!.onClose()
        }
    }

    fun mouseScrolled(delta: Double): Boolean {
        if (!active) return false

        if (selectionScreen!!.focus) {
            selectionScreen!!.cycle(delta.toInt())
            return true
        }
        if (AllKeys.ctrlDown()) return currentTool!!.tool
            .handleMouseWheel(delta)
        return false
    }

    fun init() {

    }
}