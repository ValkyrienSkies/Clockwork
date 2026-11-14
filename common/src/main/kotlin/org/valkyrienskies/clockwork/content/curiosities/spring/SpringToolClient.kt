package org.valkyrienskies.clockwork.content.curiosities.spring

import com.simibubi.create.foundation.gui.RemovedGuiUtils
import com.simibubi.create.infrastructure.config.AllConfigs
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.gui.element.BoxElement
import net.createmod.catnip.gui.element.GuiGameElement
import net.createmod.catnip.theme.Color
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.EntityHitResult
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotEntity
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotRenderer
import java.util.Locale

object SpringToolClient {
    private fun target(): RopeKnotEntity? {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return null
        if (mc.screen != null || !player.mayBuild() || player.isSpectator) return null
        val endpoint = ((mc.hitResult as? EntityHitResult)?.entity as? RopeKnotEntity)
            ?.takeIf { !it.winch && it.distanceToSqr(player) <= 36 } ?: return null
        if (!(endpoint.spring && SpringClipboard.isClipboard(player.mainHandItem)) &&
            !SpringControls.isTool(player.mainHandItem, endpoint.spring) &&
            !SpringControls.isTool(player.offhandItem, endpoint.spring)) return null
        return endpoint
    }

    fun scroll(delta: Double): Boolean {
        if (!delta.isFinite() || delta == 0.0) return false
        val endpoint = target() ?: return false
        if (endpoint.spring && Minecraft.getInstance().player?.mainHandItem?.let(SpringClipboard::isClipboard) == true) return false
        ClockworkPackets.sendToServer(SpringAdjustPacket(endpoint.id, if (delta > 0) 1 else -1))
        return true
    }

    fun render(graphics: GuiGraphics) {
        val mc = Minecraft.getInstance()
        if (mc.options.hideGui) return
        val endpoint = target() ?: return
        val link = RopeKnotRenderer.link(endpoint.linkId) ?: return
        val player = mc.player ?: return
        val stack = player.mainHandItem.takeIf { (endpoint.spring && SpringClipboard.isClipboard(it)) ||
            SpringControls.isTool(it, endpoint.spring) } ?: player.offhandItem
        val tooltip = mutableListOf<Component>()
        CreateLang.builder().add(Component.translatable(if (endpoint.spring) "item.vs_clockwork.spring" else "item.vs_clockwork.rope_knot"))
            .style(ChatFormatting.WHITE).forGoggles(tooltip)
        if (SpringClipboard.isClipboard(stack)) {
            CreateLang.builder().add(CreateLang.translateDirect("clipboard.to_copy", mc.options.keyUse.translatedKeyMessage))
                .forGoggles(tooltip, 1)
            if (SpringClipboard.hasSettings(stack)) {
                val attack = mc.options.keyAttack.translatedKeyMessage
                CreateLang.builder().add(Component.translatable("spring.vs_clockwork.clipboard.paste", attack))
                    .forGoggles(tooltip, 1)
                CreateLang.builder().add(Component.translatable("spring.vs_clockwork.clipboard.paste_all", attack))
                    .forGoggles(tooltip, 1)
            }
        } else {
            val wrench = SpringControls.isWrench(stack)
            val selected = if (wrench) 0 else SpringControls.selected(stack) + 1
            val settings = SpringSettings.load(link.settings)
            val keys = listOf("length", "stiffness", "damping", "twist")
            val values = listOf(link.length, settings.stiffness, settings.damping, settings.twist)
            for (i in if (wrench) listOf(0) else listOf(1, 2, 3)) {
                val value = String.format(Locale.ROOT, if (i == 0) "%.4f" else "%.3f", values[i]).trimEnd('0').trimEnd('.')
                CreateLang.builder().add(Component.translatable("spring.vs_clockwork." + keys[i], value))
                    .style(if (selected == i) ChatFormatting.GOLD else ChatFormatting.GRAY).forGoggles(tooltip, 1)
            }
        }
        val width = mc.window.guiScaledWidth
        val height = mc.window.guiScaledHeight
        val textWidth = tooltip.maxOf { mc.font.width(it) }
        val textHeight = 10 + (tooltip.size - 1) * 10
        val config = AllConfigs.client()
        val x = (width / 2 + config.overlayOffsetX.get()).coerceAtMost(width - textWidth - 20).coerceAtLeast(4)
        val y = (height / 2 + config.overlayOffsetY.get()).coerceAtMost(height - textHeight - 20).coerceAtLeast(20)
        val custom = config.overlayCustomColor.get()
        val background = if (custom) Color(config.overlayBackgroundColor.get()) else BoxElement.COLOR_VANILLA_BACKGROUND.copy().scaleAlpha(0.75f)
        val borderTop = if (custom) Color(config.overlayBorderColorTop.get()) else BoxElement.COLOR_VANILLA_BORDER.getFirst().copy()
        val borderBottom = if (custom) Color(config.overlayBorderColorBot.get()) else BoxElement.COLOR_VANILLA_BORDER.getSecond().copy()
        graphics.pose().pushPose()
        GuiGameElement.of(stack).at<GuiGameElement.GuiRenderBuilder>((x + 10).toFloat(), (y - 16).toFloat(), 450f).render(graphics)
        RemovedGuiUtils.drawHoveringText(graphics, tooltip, x, y, width, height, -1,
            background.getRGB(), borderTop.getRGB(), borderBottom.getRGB(), mc.font)
        graphics.pose().popPose()
    }
}
