package org.valkyrienskies.clockwork.content.curiosities

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.RecordItem
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkSounds

class WanderlustMusicDisc : RecordItem {
    constructor(properties: Properties) : super(7, ClockworkSounds.WANDERLUST.mainEvent!!, properties, 84)

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltipComponents: MutableList<Component>,
        isAdvanced: TooltipFlag
    ) {
        tooltipComponents.add(this.getDisplayName().withStyle(ChatFormatting.LIGHT_PURPLE))
        tooltipComponents.add(
            Component.translatable("item.vs_clockwork.music_disc_wanderlust.lore1")
                .withStyle(ChatFormatting.GRAY)
        )
        tooltipComponents.add(
            Component.translatable("item.vs_clockwork.music_disc_wanderlust.lore2")
                .withStyle(ChatFormatting.DARK_GRAY)
        )
    }
}
