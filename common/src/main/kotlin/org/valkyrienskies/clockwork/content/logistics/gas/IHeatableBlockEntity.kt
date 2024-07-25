package org.valkyrienskies.clockwork.content.logistics.gas

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextComponent
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos

interface IHeatableBlockEntity: IHaveGoggleInformation {
    fun getDuctNodePosition(): DuctNodePos
    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        tooltip.add(TextComponent("    Duct Info").withStyle(ChatFormatting.GRAY))

        var found = false
        if (ClockworkMod.getKelvin().getTemperatureAt(this.getDuctNodePosition()) != 0.0) {
            tooltip.add(TextComponent("Temperature: ${ClockworkMod.getKelvin().getTemperatureAt(this.getDuctNodePosition())} K"))
            found = true
        }
        if (ClockworkMod.getKelvin().getPressureAt(this.getDuctNodePosition()) != 0.0) {
            tooltip.add(TextComponent("Pressure: ${ClockworkMod.getKelvin().getPressureAt(this.getDuctNodePosition())} Pa"))
            found = true
        }
        if (ClockworkMod.getKelvin().getGasVolumesAt(this.getDuctNodePosition()).isNotEmpty()) {
            tooltip.add(TextComponent("Gas Volumes:"))
            for (entry in ClockworkMod.getKelvin().getGasVolumesAt(this.getDuctNodePosition()).entries) {
                tooltip.add(TextComponent("${entry.key}: ${entry.value} m^3"))
            }
            found = true
        }

        if (!found) {
            tooltip.add(TextComponent("Empty."))
        }
        return found
    }
}