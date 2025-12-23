package org.valkyrienskies.clockwork.util.gui

import com.simibubi.create.foundation.item.TooltipModifier
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.block.Block
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.platform.PlatformUtils
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import kotlin.collections.addAll

abstract class DuctStats(val block: Block) : TooltipModifier {

    companion object {
        @JvmStatic
        fun getDuctStats(block: Block, player: Player): List<Component> {
            val list: MutableList<Component> = ArrayList()

            if (block is IHaveDuctStats) {
                val internalVolume = block.getInternalVolume()
                val maxTemperature = block.getMaximumTemperature()
                val maxPressure = block.getMaximumPressure()

                //list += CommonComponents.EMPTY

                list += Component.translatable("vs_clockwork.duct_stats.header").withStyle(ChatFormatting.GRAY)

                list += Component.translatable("vs_clockwork.duct_stats.internal_volume").append(" $internalVolume m³").withStyle(
                    ChatFormatting.GREEN)
                list += Component.translatable("vs_clockwork.duct_stats.max_temperature").append(" $maxTemperature °K").withStyle(
                    ChatFormatting.RED)
                list += Component.translatable("vs_clockwork.duct_stats.max_pressure").append(" ${maxPressure/1000} kPa").withStyle(
                    ChatFormatting.BLUE)

                val productionStats = block.getProductionStats()
                if (productionStats.isNotEmpty()) {
                    list += CommonComponents.EMPTY
                    list += Component.translatable("vs_clockwork.duct_stats.is_producer").withStyle(ChatFormatting.GOLD)
                    for ((key, info) in productionStats) {
                        val iconComponent = Component.literal(ClockworkGasses.getDisplayCharacterCode(GasTypeRegistry.getGasType(key)!!)).withStyle {it.withFont(
                            ClockworkGasses.ICON_FONT_LOCATION)}
                        val methodComponent = Component.translatable(info.method.langKey)
                        val typeComponent = Component.translatable(info.type.langKey)
                        var fullComponent = Component.empty().append(iconComponent).append(" - ").append(methodComponent).append(" (").append(typeComponent)
                        if (info.condition.toString().isNotEmpty()) {
                            fullComponent = fullComponent.append(info.condition)
                        }
                        fullComponent = fullComponent.append(")")
                        list += fullComponent.withStyle(ChatFormatting.WHITE)
                    }
                }
                val additional = block.getAdditionalInfoLines()
                if (additional.isNotEmpty()) {
                    list += CommonComponents.EMPTY
                    list.addAll(additional)
                }
            }

            return list
        }

        @JvmStatic
        fun create(item: Item): DuctStats? {
            if (item is BlockItem) {
                val block = item.block;
                if (block is IHaveDuctStats) {
                    return PlatformUtils.getDuctStats(block)
                }
            }
            return null;
        }
    }
}
