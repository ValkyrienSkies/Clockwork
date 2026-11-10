package org.valkyrienskies.clockwork.content.curiosities.rope

import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor

open class RopeKnotItem(properties: Properties) : CWItem(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (!context.level.isClientSide) RopeKnotServer.tie(player as ServerPlayer, context.itemInHand,
            WandAnchor.at(context.level, context.clickedPos, context.clickedFace))
        return InteractionResult.sidedSuccess(context.level.isClientSide)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (!player.isShiftKeyDown || !stack.orCreateTag.contains(START)) return InteractionResultHolder.pass(stack)
        if (!level.isClientSide) clearStart(stack)
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }

    override fun appendHoverText(stack: ItemStack, level: Level?, lines: MutableList<Component>, flag: TooltipFlag) {
        lines.add(Component.translatable("item.vs_clockwork.rope_knot.controls").withStyle(ChatFormatting.GRAY))
        if (stack.tag?.contains(START) == true)
            lines.add(Component.translatable("item.vs_clockwork.rope_knot.pending").withStyle(ChatFormatting.GRAY))
        super.appendHoverText(stack, level, lines, flag)
    }

    companion object {
        const val START = "RopeStart"
        fun start(stack: ItemStack, level: Level): WandAnchor? {
            val data = stack.tag?.getCompound(START) ?: return null
            if (data.getString("dimension") != level.dimension().location().toString()) return null
            return if (data.contains("anchor")) WandAnchor.load(data.getCompound("anchor")) else null
        }
        fun setStart(stack: ItemStack, level: Level, anchor: WandAnchor) {
            stack.orCreateTag.put(START, CompoundTag().also {
                it.putString("dimension", level.dimension().location().toString()); it.put("anchor", anchor.save())
            })
        }
        fun clearStart(stack: ItemStack) { stack.removeTagKey(START) }
    }
}
