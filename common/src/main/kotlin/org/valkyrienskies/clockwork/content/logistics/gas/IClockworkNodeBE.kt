package org.valkyrienskies.clockwork.content.logistics.gas

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import dev.architectury.platform.Platform
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.kelvin.impl.client.DuctNetworkClient
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import kotlin.math.roundToInt

interface IClockworkNodeBE: INodeBlockEntity, IHaveGoggleInformation {

    fun heatableGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        tooltip.add(Component.literal("    Duct Info").withStyle(ChatFormatting.GRAY))


        var found = false

        val kelvin = if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin() else ClockworkModClient.getKelvin()

//        if (kelvin is DuctNetworkClient) {
//            tooltip.add(Component.literal("Last Synchronized: ${kelvin.queryTicksSinceLastSync()}..."))
//        }
        if (kelvin.nodeInfo[this.getDuctNodePosition()] != null && kelvin.nodeInfo[this.getDuctNodePosition()]!!.totalVolume > 0.0) {
            val totalVolume = kelvin.nodeInfo[this.getDuctNodePosition()]?.totalVolume ?: 0.0
            tooltip.add(Component.literal("Volume: $totalVolume m³").withStyle(ChatFormatting.GREEN))
            found = true
        }
        if (kelvin.getTemperatureAt(this.getDuctNodePosition()) > 0.0) {
            tooltip.add(Component.literal("Temperature: ${kelvin.getTemperatureAt(this.getDuctNodePosition()).toInt()} K").withStyle(ChatFormatting.GOLD))
            found = true
        }
        if (kelvin.nodeInfo[this.getDuctNodePosition()] != null && kelvin.nodeInfo[this.getDuctNodePosition()]!!.currentEnergy > 0.0 && isPlayerSneaking) {
            val currentEnergy = kelvin.nodeInfo[this.getDuctNodePosition()]!!.currentEnergy
            if (currentEnergy < 100000.0) {
                tooltip.add(Component.literal("Thermal Energy: ${(currentEnergy).roundToInt()} J").withStyle(ChatFormatting.RED))
            } else {
                tooltip.add(Component.literal("Thermal Energy: ${(currentEnergy/1000.0).roundToInt()} kJ").withStyle(ChatFormatting.RED))
            }
        }
        if (kelvin.getPressureAt(this.getDuctNodePosition()) > 0.0) {
            val currentPressure = kelvin.getPressureAt(this.getDuctNodePosition())
            if (currentPressure < 100000.0) {
                tooltip.add(Component.literal("Pressure: ${currentPressure.roundToInt()} Pa").withStyle(ChatFormatting.BLUE))
            } else {
                tooltip.add(Component.literal("Pressure: ${(currentPressure/1000.0).roundToInt()} kPa").withStyle(ChatFormatting.BLUE))
            }
            found = true
        }
        if (kelvin.getGasMassAt(this.getDuctNodePosition()).isNotEmpty()) {
            //tooltip.add(Component.literal("Gas Masses:"))
            val sortedByAmount = kelvin.getGasMassAt(this.getDuctNodePosition()).entries.sortedByDescending { it.value }
            val rows = mutableListOf<Pair<Component, Component>>()
            val finishedComponents = mutableListOf<Component>()

            for (entry in sortedByAmount) {
                val iconComponent = Component.literal(ClockworkGasses.getDisplayCharacterCode(entry.key)).withStyle {it.withFont(
                    ClockworkGasses.ICON_FONT_LOCATION)}
                val nameComponent = if (isPlayerSneaking) Component.literal(" ${entry.key.name} ").withStyle(ChatFormatting.GRAY)
                else Component.empty()
                val amtComponent =
                    if (entry.value > 0 && entry.value < 1) Component.literal("${(entry.value*1000.0).roundToInt()}g")
                    else if (entry.value >= 1) Component.literal("${(entry.value*1000.0).roundToInt()/1000.0}kg")
                    else null
                if (amtComponent != null) {
                    val finalComponent = Component.empty().append(iconComponent).append(nameComponent).append(amtComponent)
                    finishedComponents.add(finalComponent)
                }
            }
            for (num in finishedComponents.indices step 2) {
                if (num + 1 < finishedComponents.size) {
                    rows.add(Pair(finishedComponents[num], finishedComponents[num + 1]))
                } else {
                    rows.add(Pair(finishedComponents[num], Component.empty()))
                }
            }
            for (row in rows) {
                val rowComponent = Component.empty()
                    .append(row.first)
                    .append(Component.literal("  "))
                    .append(row.second)
                tooltip.add(rowComponent)
            }

            found = true
        }

        if (!found) {
            tooltip.add(Component.literal("This node is empty.").withStyle(ChatFormatting.DARK_GRAY).withStyle(
                ChatFormatting.ITALIC))
        }

        return found
    }
}
