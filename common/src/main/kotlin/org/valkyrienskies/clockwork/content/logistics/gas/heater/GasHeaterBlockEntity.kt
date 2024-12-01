package org.valkyrienskies.clockwork.content.logistics.gas.heater

import com.simibubi.create.content.fluids.tank.BoilerHeaters
import com.simibubi.create.content.processing.basin.BasinBlockEntity
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.mod.common.util.toJOMLD

class GasHeaterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state), IHeatableBlockEntity {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) { }

    override fun getDuctNodePosition(): DuctNodePos {
        return blockPos.toJOMLD()
    }

    val energyCostPerLevel = 1000

    override fun tick() {
        super.tick()



        if (level?.isClientSide != false ) return

        val node = ClockworkMod.getKelvin().getNodeAt(getDuctNodePosition()) ?: return
        val temp = ClockworkMod.getKelvin().getTemperatureAt(getDuctNodePosition())

        var state = level!!.getBlockState(blockPos)
        println("${state.hasProperty(HEAT_LEVEL)} ${state.getValue(HEAT_LEVEL)}")
        if (state.block !is GasHeaterBlock) return

        val heatLevel = when {
            temp < 400 -> HeatLevel.NONE
            temp < 750 -> HeatLevel.SMOULDERING
            temp < 1250 -> HeatLevel.FADING
            temp < 1500 -> HeatLevel.KINDLED
            else -> HeatLevel.SEETHING
        }



        if (state.getValue(HEAT_LEVEL) == heatLevel) return

        println(heatLevel)


        state = state.setValue(HEAT_LEVEL, heatLevel)
        level!!.setBlockAndUpdate(blockPos, state)
        notifyUpdate()
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        tooltip.add(TextComponent("    Heater Info").withStyle(ChatFormatting.GRAY))
        tooltip.add(TextComponent("Heat Level: ${level!!.getBlockState(blockPos).getValue(HEAT_LEVEL).name}").withStyle(ChatFormatting.YELLOW))
        tooltip.add(TextComponent.EMPTY)

        return super.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }
}