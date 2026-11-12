package org.valkyrienskies.clockwork.content.kinetics.combobulator

import com.simibubi.create.AllItems
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotEntity
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotRenderer
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor

object ThinkableClient {
    fun render(graphics: GuiGraphics) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return
        val thinkable = player.mainHandItem.item is ThinkableItem || player.offhandItem.item is ThinkableItem
        val wrench = AllItems.WRENCH.isIn(player.mainHandItem) || AllItems.WRENCH.isIn(player.offhandItem)
        if (mc.screen != null || mc.options.hideGui || !thinkable && !wrench) return
        val anchor = when (val hit = mc.hitResult) {
            is BlockHitResult -> WandAnchor.at(level, hit.blockPos, hit.direction)
            is EntityHitResult -> {
                val knot = hit.entity as? RopeKnotEntity ?: return
                val link = RopeKnotRenderer.link(knot.linkId) ?: return
                if (!link.thinkable) return
                if (knot.end == 0) link.a else link.b
            }
            else -> return
        }
        val device = level.getBlockEntity(anchor.pos) as? IThinkable ?: return
        if (!thinkable && device !is ThinkableRelayBlockEntity) return
        val lines = mutableListOf<Component>(level.getBlockState(anchor.pos).block.name)
        if (device.thinkablePortName(anchor.face) == null) {
            lines += Component.translatable("thinkable.vs_clockwork.no_port").withStyle(ChatFormatting.DARK_GRAY)
        }
        for (side in Direction.entries) {
            val name = device.thinkablePortName(side) ?: continue
            lines += Component.translatable("thinkable.vs_clockwork.face.${side.name.lowercase()}")
                .append(": ").append(name).withStyle(if (side == anchor.face) ChatFormatting.GOLD else ChatFormatting.GRAY)
        }
        graphics.renderComponentTooltip(mc.font, lines, mc.window.guiScaledWidth / 2 + 12, mc.window.guiScaledHeight / 2 + 12)
    }
}
