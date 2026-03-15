package org.valkyrienskies.clockwork.content.logistics.gas.heater

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.gui.ClockworkTooltipHelper
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos

class GasHeaterBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) { }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }

    val heatLossPerLevel = 100.0

    override fun tick() {
        super.tick()



        if (level?.isClientSide != false ) return
        val kelvin = ClockworkMod.getKelvin()


        val node = kelvin.getNodeAt(getDuctNodePosition()) ?: return
        val temp = kelvin.getTemperatureAt(getDuctNodePosition())

        var state = level!!.getBlockState(blockPos)
        if (state.block !is GasHeaterBlock) return

        val heatLevel = when {
            temp >= ClockworkConfig.SERVER.heaterSeethingTemp -> HeatLevel.SEETHING
            temp >= ClockworkConfig.SERVER.heaterKindledTemp -> HeatLevel.KINDLED
            temp >= ClockworkConfig.SERVER.heaterSmoulderingTemp -> HeatLevel.SMOULDERING
            else -> HeatLevel.NONE
        }

        val energyInHeater = kelvin.getHeatEnergy(getDuctNodePosition())
        kelvin.modHeatEnergy(getDuctNodePosition(), -(heatLevel.ordinal * heatLossPerLevel).coerceAtMost(energyInHeater))

        if (state.getValue(HEAT_LEVEL) == heatLevel) return




        state = state.setValue(HEAT_LEVEL, heatLevel)
        level!!.setBlockAndUpdate(blockPos, state)
        notifyUpdate()
    }

    override fun addToGoggleTooltip(tooltip: List<Component>?, isPlayerSneaking: Boolean): Boolean {
        ClockworkLang.translate("gui.gas_heater.info.title").forGoggles((tooltip as MutableList))
        when (blockState.getValue(HEAT_LEVEL)) {
            HeatLevel.SEETHING, HeatLevel.FADING -> {
                ClockworkTooltipHelper.addHint(tooltip, "gui.gas_heater.info.heat_level.superheated", ChatFormatting.AQUA)
            }
            HeatLevel.KINDLED -> {
                ClockworkTooltipHelper.addHint(tooltip, "gui.gas_heater.info.heat_level.heated", ChatFormatting.GOLD)
                if (isPlayerSneaking)
                    ClockworkLang.translate("gui.gas_heater.info.heat_level.next",
                        DuctTextUtil.translateTemperature(
                            ClockworkLang.builder(),
                            ClockworkConfig.SERVER.heaterSeethingTemp.toDouble(), true
                        )
                    ).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip)
            }
            HeatLevel.SMOULDERING -> {
                ClockworkTooltipHelper.addHint(tooltip, "gui.gas_heater.info.heat_level.passive", ChatFormatting.RED)
                if (isPlayerSneaking)
                    ClockworkLang.translate("gui.gas_heater.info.heat_level.next",
                        DuctTextUtil.translateTemperature(
                            ClockworkLang.builder(),
                            ClockworkConfig.SERVER.heaterKindledTemp.toDouble(), true
                        )
                    ).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip)
            }
            else -> {
                ClockworkTooltipHelper.addHint(tooltip, "gui.gas_heater.info.heat_level.none", ChatFormatting.GRAY)
                if (isPlayerSneaking)
                    ClockworkLang.translate("gui.gas_heater.info.heat_level.next",
                        DuctTextUtil.translateTemperature(
                            ClockworkLang.builder(),
                            ClockworkConfig.SERVER.heaterSmoulderingTemp.toDouble(), true
                        )
                    ).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip)
            }
        }
        tooltip.add(CommonComponents.EMPTY)
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }
}