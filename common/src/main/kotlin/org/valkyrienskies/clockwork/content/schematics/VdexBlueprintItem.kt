package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.platform.CWItem

class VdexBlueprintItem(properties: Properties) : CWItem(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        if (context.hand != InteractionHand.MAIN_HAND || !player.mayBuild()) return InteractionResult.PASS
        if (context.level.isClientSide) {
            if (player.isShiftKeyDown || !context.itemInHand.orCreateTag.contains("VdexFile")) VdexBlueprintClient.open()
            else VdexPlacementClient.rightClick()
        }
        return InteractionResult.sidedSuccess(context.level.isClientSide)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(player.getItemInHand(hand))
        if (level.isClientSide) {
            if (player.isShiftKeyDown || !player.getItemInHand(hand).orCreateTag.contains("VdexFile")) VdexBlueprintClient.open()
            else VdexPlacementClient.rightClick()
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }

    override fun appendHoverText(stack: ItemStack, level: Level?, lines: MutableList<Component>, flag: TooltipFlag) {
        stack.tag?.getString("VdexName")?.takeIf { it.isNotBlank() }?.let { lines.add(Component.literal(it).withStyle(ChatFormatting.AQUA)) }
        if (stack.tag?.getBoolean("Deployed") == true) lines.add(Component.translatable("item.vs_clockwork.vdex_blueprint.deployed").withStyle(ChatFormatting.GRAY))
        super.appendHoverText(stack, level, lines, flag)
    }
}
