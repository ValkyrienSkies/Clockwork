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
import org.valkyrienskies.clockwork.ClockworkPackets
import net.minecraft.core.BlockPos
import org.valkyrienskies.clockwork.ClockworkModClient
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
        if (Minecraft.getInstance().options.hideGui || !active) return

        if (activeSchematicItem != null && isRegular) {
            overlay!!.renderWanderlite(poseStack, activeHotbarSlot, partialTicks)
        }

        currentTool!!.tool.renderOverlay(poseStack, partialTicks, width, height)
        selectionScreen!!.renderPassive(poseStack, partialTicks)
        ClockworkModClient.WANDERWAND_EFFECT_RENDERER.ropeLength()?.let { length ->
            val mc = Minecraft.getInstance()
            poseStack.drawCenteredString(mc.font, net.minecraft.network.chat.Component.translatable(
                "vs_clockwork.wanderwand.rope_length", String.format(java.util.Locale.ROOT, "%.1f", length)),
                mc.window.guiScaledWidth / 2, mc.window.guiScaledHeight - 65, 0xC3A0E3)
        }
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
        if (active && Minecraft.getInstance().player != null)
            ClockworkPackets.sendToServer(WandSelectionPacket(BlockPos.ZERO, null, currentTool!!, false, -2))
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
        if (!active) return false
        else if (!pressed || (button != 1 && button != 0)) return false
        val mc = Minecraft.getInstance()
        if (mc.screen != null || mc.player == null) return false
        if (button == 0) return currentTool!!.tool.handleLeftClick()
        if (currentTool == ToolType.SELECT || currentTool == ToolType.DESELECT) {
            val hit = mc.hitResult as? net.minecraft.world.phys.BlockHitResult
            if (mc.player!!.isShiftKeyDown || (hit != null && mc.level?.getBlockEntity(hit.blockPos) is
                    org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity)) return false
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
        if (!active || delta == 0.0) {
            return false
        }

        if (selectionScreen!!.focus) {
            selectionScreen!!.cycle(delta.toInt())
            return true
        }
        if (ClockworkModClient.WANDERWAND_EFFECT_RENDERER.holdingRope()) {
            ClockworkPackets.sendToServer(WanderwandReelPacket(if (delta > 0) 1 else -1))
            // Both loaders cancel hotbar scrolling when this event is consumed, even at the rope's limits.
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
