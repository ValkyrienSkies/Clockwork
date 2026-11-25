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
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.content.curiosities.strut.StrutAdjustPacket
import org.valkyrienskies.clockwork.content.curiosities.strut.StrutPhysics
import java.util.Locale
import java.util.UUID

object SpringToolClient {
    private fun target(): RopeKnotEntity? {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return null
        if (mc.screen != null || !player.mayBuild() || player.isSpectator) return null
        val endpoint = ((mc.hitResult as? EntityHitResult)?.entity as? RopeKnotEntity)
            ?.takeIf { !it.winch && it.distanceToSqr(player) <= 36 } ?: return null
        val link = RopeKnotRenderer.link(endpoint.linkId) ?: return null
        if (link.harpoon || link.strut) return null
        if (!(endpoint.spring && SpringClipboard.isClipboard(player.mainHandItem)) &&
            !SpringControls.isTool(player.mainHandItem, endpoint.spring) &&
            !SpringControls.isTool(player.offhandItem, endpoint.spring)) return null
        return endpoint
    }

    fun scroll(delta: Double): Boolean {
        if (!delta.isFinite() || delta == 0.0) return false
        val endpoint = target()
        if (endpoint == null) {
            val strut = strutTarget() ?: return false
            ClockworkPackets.sendToServer(StrutAdjustPacket(strut.key, if (delta > 0) 1 else -1))
            return true
        }
        if (endpoint.spring && Minecraft.getInstance().player?.mainHandItem?.let(SpringClipboard::isClipboard) == true) return false
        ClockworkPackets.sendToServer(SpringAdjustPacket(endpoint.id, if (delta > 0) 1 else -1))
        return true
    }

    fun render(graphics: GuiGraphics) {
        val mc = Minecraft.getInstance()
        if (mc.options.hideGui) return
        val endpoint = target()
        if (endpoint == null) {
            val link = strutTarget()?.value ?: return
            val player = mc.player ?: return
            val tool = player.mainHandItem.takeIf { SpringControls.isTool(it) } ?: player.offhandItem
            val tooltip = mutableListOf<Component>()
            CreateLang.builder().add(Component.translatable("item.vs_clockwork.strut")).style(ChatFormatting.WHITE).forGoggles(tooltip)
            val mode = when {
                link.a.shipId == link.b.shipId -> Component.translatable("strut.vs_clockwork.shared_body")
                SpringControls.isWrench(tool) -> Component.translatable("spring.vs_clockwork.length", String.format(Locale.ROOT, "%.4f", link.length).trimEnd('0').trimEnd('.'))
                else -> {
                    val axis = StrutPhysics.axis(player.level(), link.a, player.lookAngle)
                    val name = (if (axis.axisDirection.step > 0) "+" else "−") + axis.axis.name
                    Component.translatable(if (player.isShiftKeyDown) "strut.vs_clockwork.turn" else "strut.vs_clockwork.move", name)
                }
            }
            CreateLang.builder().add(mode).style(ChatFormatting.GOLD).forGoggles(tooltip, 1)
            draw(graphics, tool, tooltip)
            return
        }
        val link = RopeKnotRenderer.link(endpoint.linkId) ?: return
        val player = mc.player ?: return
        val stack = player.mainHandItem.takeIf { (endpoint.spring && SpringClipboard.isClipboard(it)) ||
            SpringControls.isTool(it, endpoint.spring) } ?: player.offhandItem
        val tooltip = mutableListOf<Component>()
        val name = when {
            link.thinkable -> "item.vs_clockwork.thinkable"
            link.spring -> "item.vs_clockwork.spring"
            else -> "item.vs_clockwork.rope_knot"
        }
        CreateLang.builder().add(Component.translatable(name))
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
        draw(graphics, stack, tooltip)
    }

    private fun strutTarget(): Map.Entry<UUID, RopeKnotRenderer.Link>? {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return null
        if (mc.screen != null || !player.mayBuild() || player.isSpectator ||
            (!SpringControls.isTool(player.mainHandItem) && !SpringControls.isTool(player.offhandItem))) return null
        return RopeKnotRenderer.targetedStrut(player)
    }

    private fun draw(graphics: GuiGraphics, stack: ItemStack, tooltip: List<Component>) {
        val mc = Minecraft.getInstance()
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
