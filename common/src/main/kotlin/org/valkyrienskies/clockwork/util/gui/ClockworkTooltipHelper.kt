package org.valkyrienskies.clockwork.util.gui

import net.createmod.catnip.lang.FontHelper
import net.createmod.catnip.lang.FontHelper.cutTextComponent
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkLang

object ClockworkTooltipHelper {
    fun addOnlyHint(
        tooltip: MutableList<Component>, hintKey: String, indents: Int = 0, vararg args: Any
    ) {
        val hint = ClockworkLang.translateDirect(hintKey, *args)
        val cutComponent = cutTextComponent(hint, FontHelper.Palette.GRAY_AND_WHITE)
        for (c in cutComponent) {
            ClockworkLang.builder().add(c).forGoggles(tooltip, indents)
        }
    }

    fun addTitleAndHint(
        tooltip: MutableList<Component>, titleKey: String, hintKey: String,
        style: ChatFormatting = ChatFormatting.GOLD, titleIndents: Int = 0, contentIndents: Int = 0, vararg args: Any
    ) {
        ClockworkLang.translate(titleKey).style(style).forGoggles(tooltip, titleIndents)
        addOnlyHint(tooltip, hintKey, contentIndents, *args)
    }

    fun addHint(
        tooltip: MutableList<Component>, hintKey: String,
        style: ChatFormatting = ChatFormatting.GOLD, titleIndents: Int = 0, contentIndents: Int = 0, vararg args: Any
    ) {
        addTitleAndHint(tooltip, "$hintKey.title", hintKey, style, titleIndents, contentIndents, *args)
    }
}