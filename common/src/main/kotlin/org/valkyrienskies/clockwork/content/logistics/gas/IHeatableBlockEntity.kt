package org.valkyrienskies.clockwork.content.logistics.gas

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation
import dev.architectury.platform.Platform
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextComponent
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.impl.client.DuctNetworkClient
import kotlin.math.roundToInt

interface IHeatableBlockEntity: IHaveGoggleInformation {
    fun getDuctNodePosition(): DuctNodePos

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        tooltip.add(TextComponent("    Duct Info").withStyle(ChatFormatting.GRAY))


        var found = false

        val kelvin = if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin() else ClockworkModClient.getKelvin()

        if (kelvin is DuctNetworkClient) {
            tooltip.add(TextComponent("Last Synchronized: ${kelvin.queryTicksSinceLastSync()}..."))
        }

        if (kelvin.getTemperatureAt(this.getDuctNodePosition()) != 0.0) {
            tooltip.add(TextComponent("Temperature: ${kelvin.getTemperatureAt(this.getDuctNodePosition()).toInt()} K").withStyle(ChatFormatting.GOLD))
            found = true
        }
        if (kelvin.getPressureAt(this.getDuctNodePosition()) != 0.0) {
            tooltip.add(TextComponent("Pressure: ${(kelvin.getPressureAt(this.getDuctNodePosition())/1000.0).roundToInt()} KPa").withStyle(ChatFormatting.BLUE))
            found = true
        }
        //todo replace with gas icon overlay
        if (kelvin.getGasMassAt(this.getDuctNodePosition()).isNotEmpty()) {
            tooltip.add(TextComponent("Gas Masses:"))
            for (entry in kelvin.getGasMassAt(this.getDuctNodePosition()).entries) {
                tooltip.add(TextComponent("${entry.key.name}: ${entry.value.roundToInt()} kg"))
            }
            found = true
        }

        if (!found) {
            tooltip.add(TextComponent("Empty."))
        }

        return found
    }

}