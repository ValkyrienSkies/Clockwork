package org.valkyrienskies.clockwork.content.logistics.gas

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation
import dev.architectury.platform.Platform
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.impl.client.DuctNetworkClient
import org.valkyrienskies.kelvin.serialization.NodeNBTUtil
import kotlin.math.roundToInt

interface IHeatableBlockEntity: IHaveGoggleInformation {
    fun getDuctNodePosition(): DuctNodePos

    fun heatableGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        tooltip.add(Component.literal("    Duct Info").withStyle(ChatFormatting.GRAY))


        var found = false

        val kelvin = if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin() else ClockworkModClient.getKelvin()

        if (kelvin is DuctNetworkClient) {
            tooltip.add(Component.literal("Last Synchronized: ${kelvin.queryTicksSinceLastSync()}..."))
        }

        if (kelvin.getTemperatureAt(this.getDuctNodePosition()) != 0.0) {
            tooltip.add(Component.literal("Temperature: ${kelvin.getTemperatureAt(this.getDuctNodePosition()).toInt()} K").withStyle(ChatFormatting.GOLD))
            found = true
        }
        if (kelvin.getPressureAt(this.getDuctNodePosition()) != 0.0) {
            tooltip.add(Component.literal("Pressure: ${(kelvin.getPressureAt(this.getDuctNodePosition())/1000.0).roundToInt()} KPa").withStyle(ChatFormatting.BLUE))
            found = true
        }
        //todo replace with gas icon overlay
        if (kelvin.getGasMassAt(this.getDuctNodePosition()).isNotEmpty()) {
            tooltip.add(Component.literal("Gas Masses:"))
            for (entry in kelvin.getGasMassAt(this.getDuctNodePosition()).entries) {
                if (entry.value > 0) tooltip.add(Component.literal("${entry.key.name}: ${entry.value.roundToInt()} kg"))
            }
            found = true
        }

        if (!found) {
            tooltip.add(Component.literal("Empty."))
        }

        return found
    }

    fun saveData(tag: CompoundTag, pos: DuctNodePos) {
        NodeNBTUtil.serializeNode(pos, KelvinMod.getKelvinByPlatform()!!, tag)
    }
    fun loadData(tag: CompoundTag, pos: DuctNodePos) {
        val nodeData = tag.getCompound("kelvin_node_data")
        if (nodeData.isEmpty) {
            return
        }
        NodeNBTUtil.deserializeNode(pos, KelvinMod.getKelvinByPlatform()!!, tag)
    }

}