package org.valkyrienskies.clockwork.content.curiosities.spring

import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.content.curiosities.rope.RopeKnotItem

class SpringItem(properties: Properties) : RopeKnotItem(properties) {
    override fun appendHoverText(stack: ItemStack, level: Level?, lines: MutableList<Component>, flag: TooltipFlag) {
        lines.add(Component.translatable("item.vs_clockwork.spring.controls"))
        lines.add(Component.translatable("item.vs_clockwork.spring.adjust"))
        if (stack.tag?.contains(RopeKnotItem.START) == true) lines.add(Component.translatable("item.vs_clockwork.spring.pending"))
    }
}
