package org.valkyrienskies.clockwork.content.physicalities.wing

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.generic.ColorBlockEntity
import java.util.*

class DyedWingBlockItem(block: Block?, properties: Properties?) :
    BlockItem(block, properties) {
    override fun appendHoverText(
            stack: ItemStack,
            level: Level?,
            tooltipComponents: MutableList<Component>,
            isAdvanced: TooltipFlag
    ) {
        if (stack.hasTag()) {
            val tag = stack.getOrCreateTag()
            val color = tag.getInt("Clockwork\$color")
            val comp: MutableComponent =
                TextComponent("#" + Integer.toHexString(color).uppercase(Locale.getDefault()))
            tooltipComponents.add(comp.setStyle(Style.EMPTY.withColor(color)))
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced)
    }

    fun hasColor(stack: ItemStack): Boolean {
        return stack.hasTag() && stack.getOrCreateTag().contains("Clockwork\$color") && stack.getOrCreateTag()
            .getInt("Clockwork\$color") != -1
    }

    fun clearColor(stack: ItemStack) {
        if (!stack.hasTag()) return
        val tag = stack.getOrCreateTag()
        val keys = tag.allKeys
        keys.remove("Clockwork\$color")
        if (keys.size > 0) tag.remove("Clockwork\$color") else stack.tag = null
    }

    override fun placeBlock(context: BlockPlaceContext, state: BlockState): Boolean {
        val result = super.placeBlock(context, state)
        if (result) {
            val stack = context.itemInHand
            val level = context.level
            val pos = context.clickedPos
            var be = level.getBlockEntity(pos)
            if (be == null) {
                be = ColorBlockEntity(ClockworkBlockEntities.COLOR_BLOCK_ENTITY.get(), pos, state)
                level.setBlockEntity(be)
            }
            val color: ColorBlockEntity = be as ColorBlockEntity? ?: return result
            color.setColor(
                if (stack.hasTag() && stack.getOrCreateTag().contains("Clockwork\$color")) stack.getOrCreateTag()
                    .getInt("Clockwork\$color") else -1
            )
        }
        return result
    }
}
