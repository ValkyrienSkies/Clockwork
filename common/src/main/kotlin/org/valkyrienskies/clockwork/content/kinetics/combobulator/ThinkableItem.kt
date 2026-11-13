package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotItem
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.platform.CWItem

class ThinkableItem(properties: Properties) : CWItem(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (!player.isShiftKeyDown || stack.tag?.contains(RopeKnotItem.START) != true) return InteractionResultHolder.pass(stack)
        if (!level.isClientSide) RopeKnotItem.clearStart(stack)
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        if (!player.mayBuild()) return InteractionResult.FAIL
        val level = context.level
        if (level !is ServerLevel) return InteractionResult.sidedSuccess(level.isClientSide)
        return useAnchor(player as ServerPlayer, context.itemInHand, WandAnchor.at(level, context.clickedPos, context.clickedFace))
    }

    fun useAnchor(player: ServerPlayer, stack: ItemStack, clicked: WandAnchor): InteractionResult {
        val level = player.serverLevel()
        if (!player.mayBuild() || player.isSpectator) return InteractionResult.FAIL
        if (!level.mayInteract(player, clicked.pos)) return InteractionResult.FAIL
        if (player.isShiftKeyDown && ThinkableConnections.disconnect(level, clicked)) {
            RopeKnotItem.clearStart(stack)
            message(player, "disconnected")
            return InteractionResult.CONSUME
        }
        val first = RopeKnotItem.start(stack, level)
        if (first == null) {
            if (ThinkableConnections.device(level, clicked)?.providesThinkable(clicked.face) != true) {
                message(player, "source")
                return InteractionResult.FAIL
            }
            RopeKnotItem.setStart(stack, level, clicked)
            clicked.world(level)?.let { ClockworkSounds.THINKABLE_ATTACH.play(level, null, it, 0.6f, 1.1f) }
            message(player, "pending")
            return InteractionResult.CONSUME
        }
        val start = first.world(level)
        val end = clicked.world(level)
        if (start == null || end == null || ThinkableConnections.device(level, first)?.providesThinkable(first.face) != true || !level.mayInteract(player, first.pos)) {
            RopeKnotItem.clearStart(stack)
            message(player, "missing")
            return InteractionResult.FAIL
        }
        if (start.distanceToSqr(end) > MAX_RANGE * MAX_RANGE) {
            message(player, "range")
            return InteractionResult.FAIL
        }
        if (!ThinkableConnections.connect(level, first, clicked)) {
            message(player, "input")
            return InteractionResult.FAIL
        }
        RopeKnotItem.clearStart(stack)
        ClockworkSounds.THINKABLE_ATTACH.play(level, null, end, 0.7f, 1f)
        message(player, "connected")
        return InteractionResult.CONSUME
    }

    private fun message(player: ServerPlayer, key: String) {
        player.displayClientMessage(Component.translatable("message.vs_clockwork.thinkable.$key"), true)
    }

    companion object {
        const val MAX_RANGE = 24.0

        fun source(level: ServerLevel, anchor: WandAnchor, reading: String = ""): Double? {
            val device = ThinkableConnections.device(level, anchor) ?: return null
            return if (device.providesThinkable(anchor.face)) device.thinkableValue(anchor.face, reading)?.takeIf { it.isFinite() } else null
        }
    }
}
