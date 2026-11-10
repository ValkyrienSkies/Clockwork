package org.valkyrienskies.clockwork.content.curiosities.spring

import com.simibubi.create.AllTags.AllItemTags
import com.simibubi.create.foundation.utility.CreateLang
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotEntity
import org.valkyrienskies.clockwork.content.curiosities.tools.screwdriver.ScrewdriverItem
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandServer
import org.valkyrienskies.mod.common.world.clipIncludeShips

object SpringControls {
    const val SELECTED = "SpringSetting"
    fun isWrench(stack: ItemStack) = AllItemTags.WRENCH.matches(stack)
    fun isTool(stack: ItemStack) = isWrench(stack) || stack.item is ScrewdriverItem
    fun selected(stack: ItemStack) = (stack.tag?.getInt(SELECTED) ?: 0).coerceIn(0, 2)

    private fun link(player: ServerPlayer, endpoint: RopeKnotEntity): WandLinks.Link? {
        if (!player.mayBuild() || player.isSpectator || !player.isAlive || endpoint.isRemoved) return null
        val level = player.serverLevel()
        val link = endpoint.linkId?.let { WandLinks.get(level).links[it] }?.takeIf { it.physical && it.spring } ?: return null
        val anchor = if (endpoint.end == 0) link.a else link.b
        val point = anchor.world(level) ?: return null
        if (!level.mayInteract(player, anchor.pos) || point.distanceToSqr(player.eyePosition) > 36) return null
        val hit = level.clipIncludeShips(ClipContext(player.eyePosition, point, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player), true)
        if (hit.type != HitResult.Type.MISS && hit.blockPos != anchor.pos &&
            hit.location.distanceTo(player.eyePosition) + 0.35 < point.distanceTo(player.eyePosition)) return null
        return link
    }

    fun use(player: ServerPlayer, endpoint: RopeKnotEntity, stack: ItemStack) {
        val link = link(player, endpoint) ?: return
        if (stack.item !is ScrewdriverItem) return
        if (player.isShiftKeyDown) {
            link.settings = link.settings.reset(selected(stack))
            changed(player, link)
        } else stack.orCreateTag.putInt(SELECTED, (selected(stack) + 1) % 3)
    }

    fun copyToClipboard(player: ServerPlayer, endpoint: RopeKnotEntity, stack: ItemStack) {
        val link = link(player, endpoint) ?: return
        SpringClipboard.copy(stack, link)
        player.displayClientMessage(CreateLang.translateDirect("clipboard.copied_from",
            Component.translatable("item.vs_clockwork.spring").withStyle(ChatFormatting.WHITE)), true)
    }

    fun pasteFromClipboard(player: ServerPlayer, endpoint: RopeKnotEntity) {
        val link = link(player, endpoint) ?: return
        val includeLength = player.isShiftKeyDown
        if (!SpringClipboard.paste(player.mainHandItem, link, includeLength)) return
        changed(player, link)
        player.displayClientMessage(Component.translatable(
            if (includeLength) "spring.vs_clockwork.clipboard.pasted_all" else "spring.vs_clockwork.clipboard.pasted"), true)
    }

    fun adjust(player: ServerPlayer, endpoint: RopeKnotEntity, direction: Int) {
        if (direction != -1 && direction != 1) return
        val stack = player.mainHandItem.takeIf { isTool(it) } ?: player.offhandItem.takeIf { isTool(it) } ?: return
        val link = link(player, endpoint) ?: return
        val tick = player.level().gameTime
        if (link.lastAdjustment == tick) return
        link.lastAdjustment = tick
        if (isWrench(stack)) link.length = SpringSettings.adjustLength(link.length, direction, player.isShiftKeyDown)
        else link.settings = link.settings.adjust(selected(stack), direction, player.isShiftKeyDown)
        changed(player, link)
    }

    private fun changed(player: ServerPlayer, link: WandLinks.Link) {
        link.settingsDirty = true
        WandLinks.get(player.serverLevel()).setDirty()
        WanderwandServer.syncLinks(player.serverLevel())
    }
}
