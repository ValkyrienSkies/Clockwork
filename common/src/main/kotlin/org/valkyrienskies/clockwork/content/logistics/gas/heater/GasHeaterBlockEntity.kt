package org.valkyrienskies.clockwork.content.logistics.gas.heater

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
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
            temp < 400 -> HeatLevel.NONE
            temp < 750 -> HeatLevel.SMOULDERING
            temp < 1250 -> HeatLevel.FADING
            temp < 1500 -> HeatLevel.KINDLED
            else -> HeatLevel.SEETHING
        }

        val energyInHeater = kelvin.getHeatEnergy(getDuctNodePosition())
        kelvin.modHeatEnergy(getDuctNodePosition(), -(heatLevel.ordinal * heatLossPerLevel).coerceAtMost(energyInHeater))

        if (state.getValue(HEAT_LEVEL) == heatLevel) return

        println(heatLevel)


        state = state.setValue(HEAT_LEVEL, heatLevel)
        level!!.setBlockAndUpdate(blockPos, state)
        notifyUpdate()
    }

    override fun addToGoggleTooltip(tooltip: List<Component>?, isPlayerSneaking: Boolean): Boolean {
        (tooltip as MutableList).add(Component.literal("    Heater Info").withStyle(ChatFormatting.GRAY))
        tooltip.add(Component.literal("Heat Level: ${level!!.getBlockState(blockPos).getValue(HEAT_LEVEL).name}").withStyle(ChatFormatting.YELLOW))
        tooltip.add(Component.empty())

        return super.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }
}